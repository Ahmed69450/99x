# Offline Hybrid Voice Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a production-grade, offline-capable hybrid Android voice assistant for BYD DiLink car head units (Android 10+, API 29+) with sub-50ms car hardware routing, local GGUF LLM fallback, Piper TTS, MCP tool integration, and a floating HUD overlay.

**Architecture:** Reactive Clean Architecture with Kotlin Coroutines/Flow. An Android Foreground Service coordinates microphone capture and a central `VoicePipelineOrchestrator` state machine. Engine A (quantized ONNX sentence transformer) immediately routes vehicle commands ($\ge 0.70$ confidence) to BYD DiLink vendor intents. Engine B (`llama.cpp` JNI wrapper) handles chitchat and complex requests with strict CPU limits (2–3 threads) and MCP tools (web search & accessibility screen awareness). Room DB provides single-turn RAG memory, and `TtsSanitizer` cleans responses before Piper TTS synthesis.

**Tech Stack:** Kotlin 1.9+, Android SDK 29+ (Android 10+), CMake & C++17 JNI, Coroutines & Flow, Room Database, Jetpack DataStore, ONNX Runtime Android (`ai.onnxruntime:onnxruntime-android`), Google Play Services Location (`play-services-location`), kotlinx.serialization, JUnit 4, MockK, Robolectric.

**Spec:** `docs/superpowers/specs/2026-10-08-offline-hybrid-voice-assistant-design.md`

## Global Constraints

- Android API Floor: `minSdkVersion 29` (Android 10), `targetSdkVersion 34`, `compileSdkVersion 34`.
- All model inference must support offline operation without crashing if network is absent.
- Engine B LLM thread pool strictly limited to 2 or 3 CPU threads (`n_threads = 2..3`) to preserve head unit system stability.
- Fast routing threshold strictly gated at confidence $\ge 0.70$ (70%).
- Room DB fact memory injected only for the active conversational turn to avoid context window explosion.
- Strict JNI fallback/mock mode required so the project builds, tests, and runs cleanly without requiring external multi-gigabyte `.gguf` or `.onnx` files pre-downloaded.

## Review Focus

1. **Missing Model File Handling:** Loading `.gguf` or Piper `.onnx` with nonexistent or invalid file paths must fail gracefully without native crash/SIGSEGV, defaulting to Android TTS and informative user feedback.
2. **Microphone Permission Denied / Concurrency:** Head unit audio focus conflict or missing `RECORD_AUDIO` must trigger a clear UI error state rather than an uncaught exception in the Foreground Service.
3. **MCP Tool Offline Timeout:** When web search MCP tool is executed without an internet connection, it must return a swift failure payload within 2 seconds instead of hanging the LLM response.
4. **TTS Sanitizer Edge Cases:** Inputs containing complex nested Markdown, code blocks, raw URLs, and mathematical symbols (`25°C`, `95%`, `120km/h`) must expand to natural spoken English words without phonetic stuttering.
5. **Vehicle Command Bounds:** Setting AC temperature must clamp to valid automotive ranges (e.g., $16.0^\circ\text{C} - 32.0^\circ\text{C}$) even if the user speaks out-of-range temperatures.

---

### Task 1: Project Scaffolding & Multi-Module Gradle Setup

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `core-vehicle/build.gradle`
- Create: `core-ai/build.gradle`
- Create: `core-context/build.gradle`
- Create: `core-mcp/build.gradle`
- Create: `core-audio/build.gradle`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: None (root configuration)
- Produces: Complete multi-module Gradle structure (`:app`, `:core-vehicle`, `:core-ai`, `:core-context`, `:core-mcp`, `:core-audio`) with dependencies and Android SDK 29+ setup.

- [ ] **Step 1: Write `settings.gradle` and root `build.gradle`**

Configure project modules:
```groovy
// settings.gradle
rootProject.name = "BYDVoiceAssistant"
include ':app'
include ':core-vehicle'
include ':core-ai'
include ':core-context'
include ':core-mcp'
include ':core-audio'
```

- [ ] **Step 2: Configure module `build.gradle` files**

Add dependencies across modules:
- Room DB (`androidx.room:room-runtime`, `androidx.room:room-ktx`, `androidx.room:room-compiler`)
- Coroutines (`kotlinx-coroutines-core`, `kotlinx-coroutines-android`, `kotlinx-coroutines-test`)
- DataStore (`androidx.datastore:datastore-preferences`)
- Kotlinx Serialization (`kotlinx-serialization-json`)
- Location (`play-services-location`)
- ONNX Runtime Android (`com.microsoft.onnxruntime:onnxruntime-android:1.17.0`)
- Testing: `junit:junit:4.13.2`, `io.mockk:mockk:1.13.8`, `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3`

- [ ] **Step 3: Create base `AndroidManifest.xml` with required automotive permissions**

Declare `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `SYSTEM_ALERT_WINDOW`, `ACCESS_FINE_LOCATION`, `INTERNET`.

- [ ] **Step 4: Verify gradle project configuration**

Run: `Test-Path settings.gradle`
Expected: Exists and accurately configured.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle build.gradle gradle.properties core-* app/
git commit -m "chore: scaffold multi-module android gradle project"
```

---

### Task 2: Core Vehicle Layer (`:core-vehicle`)

**Files:**
- Create: `core-vehicle/src/main/java/com/byd/voiceassistant/vehicle/VehicleAction.kt`
- Create: `core-vehicle/src/main/java/com/byd/voiceassistant/vehicle/VehicleState.kt`
- Create: `core-vehicle/src/main/java/com/byd/voiceassistant/vehicle/CarController.kt`
- Create: `core-vehicle/src/main/java/com/byd/voiceassistant/vehicle/BYDVehicleController.kt`
- Create: `core-vehicle/src/main/java/com/byd/voiceassistant/vehicle/SimulatedVehicleController.kt`
- Test: `core-vehicle/src/test/java/com/byd/voiceassistant/vehicle/VehicleControllerTest.kt`

**Interfaces:**
- Consumes: Android `Context`, `Intent`, `AudioManager`
- Produces: `CarController` interface, `VehicleAction` sealed class, `VehicleState` data class, `CarExecutionResult(success: Boolean, speechResponse: String)`

- [ ] **Step 1: Write the failing test for vehicle command execution & temperature clamping**

```kotlin
// VehicleControllerTest.kt
package com.byd.voiceassistant.vehicle

import org.junit.Assert.*
import org.junit.Test

class VehicleControllerTest {
    @Test
    fun testSetTemperatureWithinBounds() {
        val controller = SimulatedVehicleController()
        val result = controller.execute(VehicleAction.SetAcTemperature(24.5f))
        assertTrue(result.success)
        assertEquals(24.5f, controller.currentState.cabinTemperature, 0.01f)
        assertTrue(result.speechResponse.contains("24.5"))
    }

    @Test
    fun testSetTemperatureClamping() {
        val controller = SimulatedVehicleController()
        // Out of bounds: 40°C should clamp to 32.0°C max
        val resultMax = controller.execute(VehicleAction.SetAcTemperature(40.0f))
        assertTrue(resultMax.success)
        assertEquals(32.0f, controller.currentState.cabinTemperature, 0.01f)

        // Out of bounds: 10°C should clamp to 16.0°C min
        val resultMin = controller.execute(VehicleAction.SetAcTemperature(10.0f))
        assertTrue(resultMin.success)
        assertEquals(16.0f, controller.currentState.cabinTemperature, 0.01f)
    }

    @Test
    fun testToggleAc() {
        val controller = SimulatedVehicleController()
        controller.execute(VehicleAction.SetAcEnabled(true))
        assertTrue(controller.currentState.acEnabled)
        controller.execute(VehicleAction.SetAcEnabled(false))
        assertFalse(controller.currentState.acEnabled)
    }

    @Test
    fun testWindowControl() {
        val controller = SimulatedVehicleController()
        val result = controller.execute(VehicleAction.SetWindowPosition(WindowPosition.ALL, 100))
        assertTrue(result.success)
        assertEquals(100, controller.currentState.windows[WindowPosition.FRONT_LEFT])
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Compile/test execution to confirm missing classes.

- [ ] **Step 3: Implement `VehicleAction`, `VehicleState`, `CarController`, `SimulatedVehicleController`, and `BYDVehicleController`**

Implement complete vehicle control logic with bounds clamping ($16.0^\circ\text{C} - 32.0^\circ\text{C}$), fan speed bounds ($1 - 7$), window position ($0 - 100\%$), and BYD vendor broadcast intents (`byd.intent.action.AC_CONTROL`, `byd.intent.action.WINDOW_CONTROL`).

- [ ] **Step 4: Run unit tests to verify pass**

Verify all `VehicleControllerTest` methods pass.

- [ ] **Step 5: Commit**

```bash
git add core-vehicle/
git commit -m "feat(vehicle): implement BYD intent dispatcher and simulated vehicle controller"
```

---

### Task 3: Voice-First TTS Sanitizer (`:core-ai`)

**Files:**
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/sanitizer/TtsSanitizer.kt`
- Test: `core-ai/src/test/java/com/byd/voiceassistant/ai/sanitizer/TtsSanitizerTest.kt`

**Interfaces:**
- Consumes: Raw text generated from LLM or vehicle controllers
- Produces: `TtsSanitizer.sanitize(text: String): String`

- [ ] **Step 1: Write the failing tests for markdown, emojis, and phonetic symbol expansions**

```kotlin
// TtsSanitizerTest.kt
package com.byd.voiceassistant.ai.sanitizer

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsSanitizerTest {
    @Test
    fun testStripMarkdownAsterisksAndHeaders() {
        val input = "### Hello! **The temperature** is *optimal*."
        val expected = "Hello! The temperature is optimal."
        assertEquals(expected, TtsSanitizer.sanitize(input))
    }

    @Test
    fun testStripCodeBlocks() {
        val input = "Here is the code:\n```kotlin\nval x = 1\n```\nDone."
        val expected = "Here is the code: (code snippet omitted) Done."
        assertEquals(expected, TtsSanitizer.sanitize(input))
    }

    @Test
    fun testStripEmojis() {
        val input = "Sure! Setting AC to cool ❄️🚗 Have a great drive! 😊"
        val expected = "Sure! Setting AC to cool Have a great drive!"
        assertEquals(expected, TtsSanitizer.sanitize(input))
    }

    @Test
    fun testExpandPhoneticSymbols() {
        val input = "Target temp is 22°C with 80% humidity at 60km/h."
        val expected = "Target temp is 22 degrees Celsius with 80 percent humidity at 60 kilometers per hour."
        assertEquals(expected, TtsSanitizer.sanitize(input))
    }

    @Test
    fun testExpandAmpersandAndDollar() {
        val input = "Turn on AC & music for $5."
        val expected = "Turn on AC and music for 5 dollars."
        assertEquals(expected, TtsSanitizer.sanitize(input))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify tests fail due to missing `TtsSanitizer`.

- [ ] **Step 3: Implement `TtsSanitizer`**

Implement regex patterns for markdown stripping, code blocks, emoji Unicode blocks (`[\uD83C-\uDBFF\uDC00-\uDFFF]+`), and exact symbol replacements (`°C`, `°F`, `%`, `km/h`, `mph`, `&`, `\$`).

- [ ] **Step 4: Run unit tests to verify pass**

Verify all tests in `TtsSanitizerTest` pass.

- [ ] **Step 5: Commit**

```bash
git add core-ai/src/main/java/com/byd/voiceassistant/ai/sanitizer/ core-ai/src/test/java/com/byd/voiceassistant/ai/sanitizer/
git commit -m "feat(ai): implement voice-first TTS text sanitizer with symbol expansion"
```

---

### Task 4: Engine A: Fast Semantic Router (`:core-ai`)

**Files:**
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/router/CarCommandCatalog.kt`
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/router/SemanticRouterResult.kt`
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/router/SemanticRouter.kt`
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/router/OnnxSemanticRouter.kt`
- Test: `core-ai/src/test/java/com/byd/voiceassistant/ai/router/SemanticRouterTest.kt`

**Interfaces:**
- Consumes: User query string (`query: String`)
- Produces: `SemanticRouterResult.Match(action: VehicleAction, confidence: Float)` or `SemanticRouterResult.NoMatch(highestConfidence: Float)`

- [ ] **Step 1: Write the failing test for semantic similarity routing and 0.70 threshold**

```kotlin
// SemanticRouterTest.kt
package com.byd.voiceassistant.ai.router

import com.byd.voiceassistant.vehicle.VehicleAction
import org.junit.Assert.*
import org.junit.Test

class SemanticRouterTest {
    @Test
    fun testExactCarCommandRoutesAboveThreshold() {
        val router = OnnxSemanticRouter()
        val result = router.route("turn on the air conditioner")
        assertTrue(result is SemanticRouterResult.Match)
        val match = result as SemanticRouterResult.Match
        assertTrue(match.confidence >= 0.70f)
        assertEquals(VehicleAction.SetAcEnabled(true), match.action)
    }

    @Test
    fun testTemperatureAdjustmentParsing() {
        val router = OnnxSemanticRouter()
        val result = router.route("set temperature to 23 degrees")
        assertTrue(result is SemanticRouterResult.Match)
        val match = result as SemanticRouterResult.Match
        assertEquals(VehicleAction.SetAcTemperature(23.0f), match.action)
    }

    @Test
    fun testGeneralQueryFallsBelowThreshold() {
        val router = OnnxSemanticRouter()
        val result = router.route("Who was the first president of Brazil?")
        assertTrue(result is SemanticRouterResult.NoMatch)
        val noMatch = result as SemanticRouterResult.NoMatch
        assertTrue(noMatch.highestConfidence < 0.70f)
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing router classes.

- [ ] **Step 3: Implement `CarCommandCatalog` and `OnnxSemanticRouter`**

Implement sentence embedding cosine similarity calculation with action extraction for temperatures (e.g. "set temperature to X"), window controls, and media controls. Provide an in-memory normalized centroid vector matcher as the native ONNX embedding fallback for fast offline evaluation.

- [ ] **Step 4: Run unit tests to verify pass**

Verify `SemanticRouterTest` passes.

- [ ] **Step 5: Commit**

```bash
git add core-ai/src/main/java/com/byd/voiceassistant/ai/router/ core-ai/src/test/java/com/byd/voiceassistant/ai/router/
git commit -m "feat(ai): implement Engine A fast semantic router with confidence thresholding"
```

---

### Task 5: Engine B: llama.cpp Bridge & Prompt Builder (`:core-ai`)

**Files:**
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/llm/PromptTemplateBuilder.kt`
- Create: `core-ai/src/main/java/com/byd/voiceassistant/ai/llm/LlamaCppEngine.kt`
- Create: `core-ai/src/main/cpp/CMakeLists.txt`
- Create: `core-ai/src/main/cpp/llama-jni.cpp`
- Test: `core-ai/src/test/java/com/byd/voiceassistant/ai/llm/PromptTemplateBuilderTest.kt`
- Test: `core-ai/src/test/java/com/byd/voiceassistant/ai/llm/LlamaCppEngineTest.kt`

**Interfaces:**
- Consumes: Prompt string, thread limit (2..3), `.gguf` model file path
- Produces: `LlamaCppEngine.generateStream(prompt: String): Flow<String>`, `PromptTemplateBuilder.build(...)`

- [ ] **Step 1: Write failing tests for PromptTemplateBuilder and LlamaCppEngine**

```kotlin
// PromptTemplateBuilderTest.kt
package com.byd.voiceassistant.ai.llm

import org.junit.Assert.assertTrue
import org.junit.Test

class PromptTemplateBuilderTest {
    @Test
    fun testBuildPromptInjectsContextAndFacts() {
        val prompt = PromptTemplateBuilder.build(
            systemPrompt = "You are a car voice assistant.",
            locationContext = "Latitude: 22.54, Longitude: 114.05, Shenzhen",
            timeContext = "2026-10-08 14:30:00, Thursday",
            screenContext = "App: Maps, Showing: Navigation to Airport",
            memoryFacts = listOf("User prefers cabin temp at 23C", "User likes jazz music"),
            userQuery = "What is the next turn and what music should I play?"
        )

        assertTrue(prompt.contains("Latitude: 22.54"))
        assertTrue(prompt.contains("2026-10-08 14:30:00"))
        assertTrue(prompt.contains("User prefers cabin temp at 23C"))
        assertTrue(prompt.contains("App: Maps"))
        assertTrue(prompt.contains("What is the next turn"))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing `PromptTemplateBuilder`.

- [ ] **Step 3: Implement `PromptTemplateBuilder`, `CMakeLists.txt`, `llama-jni.cpp`, and `LlamaCppEngine`**

- `PromptTemplateBuilder`: Formats clean markdown-free system instructions, tool calling syntax (`[TOOL_CALL: name {"param": "val"}]`), and context slots.
- `CMakeLists.txt` and `llama-jni.cpp`: JNI bindings for model initialization, thread setting (`n_threads = 2..3`), token generation callbacks, and cleanup. Include built-in mock fallback if native library is unloaded so unit tests pass in JVM.
- `LlamaCppEngine`: Kotlin interface wrapping native handles with coroutine `Flow<String>`.

- [ ] **Step 4: Run unit tests to verify pass**

Verify all tests pass.

- [ ] **Step 5: Commit**

```bash
git add core-ai/
git commit -m "feat(ai): implement Engine B prompt builder and llama.cpp JNI wrapper"
```

---

### Task 6: Core Context: Location & Time Provider (`:core-context`)

**Files:**
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/location/LocationData.kt`
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/location/LocationTimeProvider.kt`
- Test: `core-context/src/test/java/com/byd/voiceassistant/context/location/LocationTimeProviderTest.kt`

**Interfaces:**
- Consumes: Android `Context`, `FusedLocationProviderClient` / `LocationManager`
- Produces: `LocationTimeProvider.getCurrentContext(): ContextSnapshot(location: LocationData?, formattedTime: String)`

- [ ] **Step 1: Write the failing test for location and time snapshot**

```kotlin
// LocationTimeProviderTest.kt
package com.byd.voiceassistant.context.location

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class LocationTimeProviderTest {
    @Test
    fun testFormattedTimeGeneratedAccurately() {
        val fixedClock = Clock.fixed(Instant.parse("2026-10-08T14:30:00Z"), ZoneId.of("UTC"))
        val provider = LocationTimeProvider(clock = fixedClock)
        val snapshot = provider.getCurrentContext(mockLocation = LocationData(22.5431, 114.0579, "Shenzhen"))
        
        assertNotNull(snapshot.location)
        assertEquals("Shenzhen", snapshot.location?.locality)
        assertTrue(snapshot.formattedTime.contains("2026-10-08 14:30:00"))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing classes.

- [ ] **Step 3: Implement `LocationData` and `LocationTimeProvider`**

Implement GPS provider with graceful permission check, location cache, and formatted system time.

- [ ] **Step 4: Run unit tests to verify pass**

Verify `LocationTimeProviderTest` passes.

- [ ] **Step 5: Commit**

```bash
git add core-context/src/main/java/com/byd/voiceassistant/context/location/ core-context/src/test/java/com/byd/voiceassistant/context/location/
git commit -m "feat(context): implement LocationTimeProvider for dynamic GPS and time context"
```

---

### Task 7: Core Context: Long-Term Memory (Room DB RAG) (`:core-context`)

**Files:**
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/memory/FactEntity.kt`
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/memory/FactDao.kt`
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/memory/AssistantMemoryDatabase.kt`
- Create: `core-context/src/main/java/com/byd/voiceassistant/context/memory/MemoryRetrievalService.kt`
- Test: `core-context/src/test/java/com/byd/voiceassistant/context/memory/MemoryRetrievalServiceTest.kt`

**Interfaces:**
- Consumes: Room Database, query string
- Produces: `MemoryRetrievalService.retrieveRelevantFacts(query: String, limit: Int = 3): List<String>`, `saveFact(key: String, value: String, tags: String)`

- [ ] **Step 1: Write the failing test for keyword fact retrieval and single-turn limit**

```kotlin
// MemoryRetrievalServiceTest.kt
package com.byd.voiceassistant.context.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRetrievalServiceTest {
    @Test
    fun testFactRetrievalMatchesRelevantKeywords() {
        val memoryDao = FakeFactDao()
        val service = MemoryRetrievalService(memoryDao)
        service.saveFact("ac_pref", "User prefers cabin temperature set to 23 degrees", "ac,temp,temperature")
        service.saveFact("music_pref", "User likes smooth jazz while driving", "music,jazz,media")
        service.saveFact("home_address", "User lives at 123 Tech Park Road", "home,navigation,address")

        val acFacts = service.retrieveRelevantFacts("What temperature should we set?", limit = 2)
        assertEquals(1, acFacts.size)
        assertTrue(acFacts[0].contains("23 degrees"))

        val musicFacts = service.retrieveRelevantFacts("Play my favorite jazz music", limit = 2)
        assertEquals(1, musicFacts.size)
        assertTrue(musicFacts[0].contains("smooth jazz"))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing memory classes.

- [ ] **Step 3: Implement `FactEntity`, `FactDao`, `AssistantMemoryDatabase`, and `MemoryRetrievalService`**

Implement keyword token extraction and Room DAO SQL queries (`SELECT * FROM facts WHERE value LIKE '%' || :keyword || '%' OR tags LIKE '%' || :keyword || '%' LIMIT :limit`).

- [ ] **Step 4: Run unit tests to verify pass**

Verify `MemoryRetrievalServiceTest` passes.

- [ ] **Step 5: Commit**

```bash
git add core-context/src/main/java/com/byd/voiceassistant/context/memory/ core-context/src/test/java/com/byd/voiceassistant/context/memory/
git commit -m "feat(context): implement Room DB long-term memory RAG retrieval service"
```

---

### Task 8: Core MCP Layer: Model Context Protocol Client & Tools (`:core-mcp`)

**Files:**
- Create: `core-mcp/src/main/java/com/byd/voiceassistant/mcp/McpProtocol.kt`
- Create: `core-mcp/src/main/java/com/byd/voiceassistant/mcp/McpTool.kt`
- Create: `core-mcp/src/main/java/com/byd/voiceassistant/mcp/tools/WebSearchTool.kt`
- Create: `core-mcp/src/main/java/com/byd/voiceassistant/mcp/tools/ScreenInspectTool.kt`
- Create: `core-mcp/src/main/java/com/byd/voiceassistant/mcp/McpClientEngine.kt`
- Test: `core-mcp/src/test/java/com/byd/voiceassistant/mcp/McpClientEngineTest.kt`

**Interfaces:**
- Consumes: Tool requests formatted as JSON-RPC 2.0 or `[TOOL_CALL: name {params}]`
- Produces: `McpClientEngine.executeTool(toolName: String, paramsJson: String): ToolResult(isSuccess: Boolean, content: String)`

- [ ] **Step 1: Write failing test for MCP JSON-RPC protocol and tool execution**

```kotlin
// McpClientEngineTest.kt
package com.byd.voiceassistant.mcp

import com.byd.voiceassistant.mcp.tools.ScreenInspectTool
import com.byd.voiceassistant.mcp.tools.WebSearchTool
import org.junit.Assert.*
import org.junit.Test

class McpClientEngineTest {
    @Test
    fun testScreenInspectToolExecution() {
        val engine = McpClientEngine()
        val screenTool = ScreenInspectTool(screenContentProvider = { "Active App: Spotify, Playing: Miles Davis" })
        engine.registerTool(screenTool)

        val result = engine.executeTool("screen_inspect", "{}")
        assertTrue(result.isSuccess)
        assertTrue(result.content.contains("Spotify"))
    }

    @Test
    fun testWebSearchToolOfflineFallback() {
        val engine = McpClientEngine()
        val webTool = WebSearchTool(networkAvailable = false)
        engine.registerTool(webTool)

        val result = engine.executeTool("web_search", """{"query": "world cup score"}""")
        assertFalse(result.isSuccess)
        assertTrue(result.content.contains("offline") || result.content.contains("unavailable"))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing MCP classes.

- [ ] **Step 3: Implement MCP Protocol, Tools, and `McpClientEngine`**

Implement JSON-RPC 2.0 data models (`JsonRpcRequest`, `JsonRpcResponse`, `ToolDefinition`), `WebSearchTool`, `ScreenInspectTool`, and `McpClientEngine`.

- [ ] **Step 4: Run unit tests to verify pass**

Verify `McpClientEngineTest` passes.

- [ ] **Step 5: Commit**

```bash
git add core-mcp/
git commit -m "feat(mcp): implement Model Context Protocol client and automotive tools"
```

---

### Task 9: Core Audio: Audio Capture, Vosk STT, Wake Word & Piper TTS (`:core-audio`)

**Files:**
- Create: `core-audio/src/main/java/com/byd/voiceassistant/audio/capture/AudioRecordSource.kt`
- Create: `core-audio/src/main/java/com/byd/voiceassistant/audio/wakeword/WakeWordDetector.kt`
- Create: `core-audio/src/main/java/com/byd/voiceassistant/audio/stt/VoskSpeechRecognizer.kt`
- Create: `core-audio/src/main/java/com/byd/voiceassistant/audio/tts/PiperTtsEngine.kt`
- Create: `core-audio/src/main/java/com/byd/voiceassistant/audio/tts/AndroidFallbackTtsEngine.kt`
- Test: `core-audio/src/test/java/com/byd/voiceassistant/audio/AudioPipelineUnitTest.kt`

**Interfaces:**
- Consumes: Audio buffers (16kHz PCM), Wake Word trigger, Text to speak
- Produces: `AudioRecordSource.audioStream: SharedFlow<ByteArray>`, `WakeWordDetector.wakeEvent: Flow<Unit>`, `VoskSpeechRecognizer.recognizeStream(...): Flow<String>`, `TtsPlayer.speak(text: String)`

- [ ] **Step 1: Write failing test for AudioPipeline & Fallback TTS**

```kotlin
// AudioPipelineUnitTest.kt
package com.byd.voiceassistant.audio

import com.byd.voiceassistant.audio.tts.PiperTtsEngine
import org.junit.Assert.*
import org.junit.Test

class AudioPipelineUnitTest {
    @Test
    fun testTtsFallsBackGracefullyWhenModelMissing() {
        val tts = PiperTtsEngine(modelPath = "/invalid/path/piper.onnx", fallbackSpeaker = { text ->
            "AndroidFallback: $text"
        })
        val response = tts.speak("Adjusting temperature to 22 degrees Celsius")
        assertTrue(response.startsWith("AndroidFallback:"))
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing audio classes.

- [ ] **Step 3: Implement Audio capture, Wake Word detector, Vosk wrapper, and Piper/Android TTS**

Implement 16kHz audio capture with coroutine flow, keyword wake word detector, streaming speech recognizer with 1.2s silence timeout, and Piper TTS player with Android system TTS fallback.

- [ ] **Step 4: Run unit tests to verify pass**

Verify `AudioPipelineUnitTest` passes.

- [ ] **Step 5: Commit**

```bash
git add core-audio/
git commit -m "feat(audio): implement audio capture, wake word, Vosk STT and Piper TTS engine"
```

---

### Task 10: App Orchestrator & State Machine (`:app`)

**Files:**
- Create: `app/src/main/java/com/byd/voiceassistant/orchestrator/VoiceState.kt`
- Create: `app/src/main/java/com/byd/voiceassistant/orchestrator/VoicePipelineOrchestrator.kt`
- Test: `app/src/test/java/com/byd/voiceassistant/orchestrator/VoicePipelineOrchestratorTest.kt`

**Interfaces:**
- Consumes: All core layers (`core-audio`, `core-ai`, `core-vehicle`, `core-context`, `core-mcp`)
- Produces: `VoicePipelineOrchestrator.state: StateFlow<VoiceState>`, `processUserUtterance(query: String)`

- [ ] **Step 1: Write failing test for orchestrator state flow and routing**

```kotlin
// VoicePipelineOrchestratorTest.kt
package com.byd.voiceassistant.orchestrator

import com.byd.voiceassistant.ai.router.OnnxSemanticRouter
import com.byd.voiceassistant.vehicle.SimulatedVehicleController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoicePipelineOrchestratorTest {
    @Test
    fun testVehicleCommandFastRoutesWithoutLlm() = runTest {
        val vehicleController = SimulatedVehicleController()
        val router = OnnxSemanticRouter()
        val orchestrator = VoicePipelineOrchestrator(
            semanticRouter = router,
            vehicleController = vehicleController,
            llmEngine = FakeLlmEngine(),
            mcpClient = FakeMcpClient(),
            contextManager = FakeContextManager()
        )

        val result = orchestrator.processUtterance("set temperature to 21 degrees")
        assertTrue(result.contains("21"))
        assertEquals(21.0f, vehicleController.currentState.cabinTemperature, 0.01f)
        assertEquals(VoiceState.IDLE, orchestrator.state.value)
    }

    @Test
    fun testGeneralQueryInvokesLlmFallback() = runTest {
        val vehicleController = SimulatedVehicleController()
        val router = OnnxSemanticRouter()
        val fakeLlm = FakeLlmEngine(mockResponse = "The battery level is 85 percent.")
        val orchestrator = VoicePipelineOrchestrator(
            semanticRouter = router,
            vehicleController = vehicleController,
            llmEngine = fakeLlm,
            mcpClient = FakeMcpClient(),
            contextManager = FakeContextManager()
        )

        val result = orchestrator.processUtterance("How is my battery?")
        assertTrue(result.contains("85 percent"))
        assertTrue(fakeLlm.wasInvoked)
    }
}
```

- [ ] **Step 2: Run test to verify failure**

Verify test failure due to missing orchestrator classes.

- [ ] **Step 3: Implement `VoiceState` and `VoicePipelineOrchestrator`**

Implement complete state transition logic:
`IDLE` ➔ `WAKE_DETECTED` ➔ `QUERY_CAPTURING` ➔ `ROUTING` ➔ `EXECUTING_ACTION` / `RUNNING_LLM` ➔ `SYNTHESIZING_TTS` ➔ `IDLE`.

- [ ] **Step 4: Run unit tests to verify pass**

Verify `VoicePipelineOrchestratorTest` passes.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/byd/voiceassistant/orchestrator/ app/src/test/java/com/byd/voiceassistant/orchestrator/
git commit -m "feat(orchestrator): implement VoicePipelineOrchestrator state machine and routing"
```

---

### Task 11: Foreground Service & Accessibility Service (`:app`)

**Files:**
- Create: `app/src/main/java/com/byd/voiceassistant/service/VoiceForegroundService.kt`
- Create: `app/src/main/java/com/byd/voiceassistant/service/VoiceAccessibilityService.kt`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: Android System Service lifecycle, `AccessibilityEvent`
- Produces: Running background service maintaining wake word loop, live screen text provider for MCP `screen_inspect` tool

- [ ] **Step 1: Write `VoiceAccessibilityService` with node hierarchy parser**

Implement recursive `AccessibilityNodeInfo` tree traversal to extract foreground app package name, button titles, and text contents (max 300 tokens).

- [ ] **Step 2: Write `VoiceForegroundService` with notification and wake lock**

Implement foreground service notification (`NotificationChannel`), persistent wake lock acquisition, and lifecycle binding to `VoicePipelineOrchestrator`.

- [ ] **Step 3: Update `AndroidManifest.xml` with service declarations and permissions**

Register `VoiceForegroundService` with `android:foregroundServiceType="microphone"` and `VoiceAccessibilityService` with `android.permission.BIND_ACCESSIBILITY_SERVICE`.

- [ ] **Step 4: Verify service classes compile and register**

Run check on manifest and service source files.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/byd/voiceassistant/service/ app/src/main/res/xml/ app/src/main/AndroidManifest.xml
git commit -m "feat(service): implement VoiceForegroundService and VoiceAccessibilityService"
```

---

### Task 12: Floating HUD Overlay Window (`:app`)

**Files:**
- Create: `app/src/main/java/com/byd/voiceassistant/presentation/overlay/VoiceOverlayView.kt`
- Create: `app/src/main/java/com/byd/voiceassistant/presentation/overlay/VoiceOverlayManager.kt`
- Create: `app/src/main/res/layout/view_voice_overlay.xml`
- Create: `app/src/main/res/drawable/bg_overlay_hud.xml`

**Interfaces:**
- Consumes: `WindowManager`, `VoiceState` flow
- Produces: Non-intrusive floating HUD widget on top of third-party apps, showing waveform/orb, recognized text, response text, and auto-dismiss timer.

- [ ] **Step 1: Write HUD layout and dark-mode automotive styling**

Design layout with status icon/orb, user speech transcript, assistant response box, and close button.

- [ ] **Step 2: Implement `VoiceOverlayView` and `VoiceOverlayManager`**

Implement `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, touch pass-through/dismiss, real-time transcript updating, and 3-second auto-dismiss on speech completion.

- [ ] **Step 3: Connect OverlayManager to `VoicePipelineOrchestrator` state flow**

Overlay shows automatically when wake word is triggered, updates state dynamically, and dismisses when returning to `IDLE`.

- [ ] **Step 4: Verify overlay component implementation**

Check layout inflation and overlay management logic.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/byd/voiceassistant/presentation/overlay/ app/src/main/res/
git commit -m "feat(ui): implement floating HUD overlay with dynamic voice state visualizer"
```

---

### Task 13: Settings, Diagnostics & Dynamic Model Selection UI (`:app`)

**Files:**
- Create: `app/src/main/java/com/byd/voiceassistant/presentation/settings/SettingsActivity.kt`
- Create: `app/src/main/java/com/byd/voiceassistant/presentation/settings/SettingsViewModel.kt`
- Create: `app/src/main/java/com/byd/voiceassistant/context/storage/ModelPreferencesDataStore.kt`
- Create: `app/src/main/res/layout/activity_settings.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: User file selection (`ActivityResultContracts.GetContent()`), DataStore preferences
- Produces: Configurable GGUF model path, Piper model path, Vosk model path, live engine diagnostic status, Room memory fact manager.

- [ ] **Step 1: Implement `ModelPreferencesDataStore`**

Implement Jetpack DataStore saving/loading paths for `.gguf` Tiny LLM, `.onnx` Piper model, and Vosk model directories.

- [ ] **Step 2: Implement `SettingsViewModel` and file picker launcher**

Handle file URI resolution (`ActivityResultContracts.GetContent()`), copy to app private storage if needed, and update DataStore.

- [ ] **Step 3: Implement `SettingsActivity` layout and diagnostics dashboard**

Add UI sections for Model Paths, Engine Readiness badges (Engine A, Engine B, Vosk, Piper, GPS), Simulated Vehicle State sliders/toggles, and Room Memory fact editor.

- [ ] **Step 4: Register `SettingsActivity` in `AndroidManifest.xml` as main launcher activity**

Ensure the activity is marked `MAIN` and `LAUNCHER`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/byd/voiceassistant/presentation/settings/ app/src/main/java/com/byd/voiceassistant/context/storage/ app/src/main/res/layout/activity_settings.xml
git commit -m "feat(ui): implement SettingsActivity with dynamic GGUF model picker and diagnostics"
```

---

### Task 14: End-to-End Pipeline Verification & Smoke Test

**Files:**
- Create: `app/src/test/java/com/byd/voiceassistant/EndToEndPipelineSmokeTest.kt`

**Interfaces:**
- Consumes: Complete assembled voice assistant pipeline
- Produces: Automated verification passing all test assertions for Vehicle Command, Fallback LLM with context, MCP tool calling, and Voice-first TTS sanitization.

- [ ] **Step 1: Write `EndToEndPipelineSmokeTest`**

```kotlin
// EndToEndPipelineSmokeTest.kt
package com.byd.voiceassistant

import com.byd.voiceassistant.ai.router.OnnxSemanticRouter
import com.byd.voiceassistant.ai.sanitizer.TtsSanitizer
import com.byd.voiceassistant.context.location.LocationData
import com.byd.voiceassistant.context.location.LocationTimeProvider
import com.byd.voiceassistant.context.memory.MemoryRetrievalService
import com.byd.voiceassistant.mcp.McpClientEngine
import com.byd.voiceassistant.mcp.tools.ScreenInspectTool
import com.byd.voiceassistant.orchestrator.VoicePipelineOrchestrator
import com.byd.voiceassistant.orchestrator.VoiceState
import com.byd.voiceassistant.vehicle.SimulatedVehicleController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EndToEndPipelineSmokeTest {
    @Test
    fun testFullVehicleCommandTurn() = runTest {
        val vehicle = SimulatedVehicleController()
        val router = OnnxSemanticRouter()
        val orchestrator = VoicePipelineOrchestrator(
            semanticRouter = router,
            vehicleController = vehicle,
            llmEngine = FakeLlmEngine(),
            mcpClient = McpClientEngine(),
            contextManager = FakeContextManager()
        )

        // Simulate voice query: "turn up the air conditioner to 25 degrees"
        val response = orchestrator.processUtterance("set temperature to 25 degrees")
        val sanitized = TtsSanitizer.sanitize(response)

        assertTrue(sanitized.contains("25 degrees Celsius"))
        assertEquals(25.0f, vehicle.currentState.cabinTemperature, 0.01f)
        assertEquals(VoiceState.IDLE, orchestrator.state.value)
    }

    @Test
    fun testFullMcpScreenAwarenessTurn() = runTest {
        val mcpEngine = McpClientEngine()
        mcpEngine.registerTool(ScreenInspectTool { "Active App: DiLink Navigation, Route: Airport 15km" })

        val result = mcpEngine.executeTool("screen_inspect", "{}")
        assertTrue(result.isSuccess)
        assertTrue(result.content.contains("DiLink Navigation"))

        val sanitizedSpeech = TtsSanitizer.sanitize("You are navigating to Airport, about 15km remaining.")
        assertEquals("You are navigating to Airport, about 15 kilometers remaining.", sanitizedSpeech)
    }
}
```

- [ ] **Step 2: Run test suite to verify pass**

Run: `gradle test` or run test runner on all unit tests.
Expected: All test suites PASS.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/byd/voiceassistant/EndToEndPipelineSmokeTest.kt
git commit -m "test: add end-to-end integration and smoke tests for voice assistant pipeline"
```
