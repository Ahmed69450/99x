# Architectural Specification: Offline Hybrid Voice Assistant for Car Head Unit (BYD DiLink)

**Date:** 2026-10-08  
**Target Platform:** BYD DiLink Head Unit (Android 10+, API Level 29+)  
**Architecture:** Clean Architecture + MVVM + Reactive Kotlin Coroutines & Flow  
**Primary Execution Model:** Android Foreground Service with Floating HUD Overlay  

---

## 1. Executive Summary & Goals

This project provides an offline, edge-native, hybrid voice assistant designed specifically for automotive head units running Android 10+ (targeted at BYD DiLink Qualcomm Snapdragon platforms such as Snapdragon 665, 6125, and 8155).

### Key Architectural Pillars:
1. **Low-Latency Vehicle Control (<50ms):** Engine A (ONNX Runtime quantized sentence transformer) routes hardware control queries instantly if semantic confidence $\ge 0.70$.
2. **Offline Intelligent Fallback:** Engine B (`llama.cpp` running quantized `.gguf` Tiny LLMs like Qwen2.5-0.5B/1.5B or SmolLM) handles conversational requests with strict thread and memory limits (2–3 CPU threads).
3. **Vehicle Awareness & MCP Extensibility:** Model Context Protocol (MCP) client exposes live vehicle tools, dynamic screen awareness (via `AccessibilityService`), and real-time internet search when connectivity is available.
4. **Dynamic Context & Long-Term Memory:** Fused location, system clock, and Room-based keyword RAG memory enrich user prompts on-demand without bloating token context.
5. **Driver Safety & Voice-First Ergonomics:** Hands-free wake word activation in background, non-blocking floating HUD overlay, and text sanitization for natural Piper TTS playback.

---

## 2. High-Level Component Topology

```
+-----------------------------------------------------------------------------------+
|                           Android OS (BYD DiLink, Android 10+)                    |
+-----------------------------------------------------------------------------------+
  |
  +--> [VoiceAssistantForegroundService] (Persistent Background Execution)
         |
         +--> [AudioCaptureManager] (AudioRecord 16kHz PCM stream)
         |      |
         |      +--> [WakeWordDetector] (Porcupine / Vosk Wake Word Engine)
         |             | (Trigger: chime earcon)
         |             v
         +--> [VoskSpeechRecognizer] (Streaming STT + Silence Detection)
                | (User Query Text)
                v
         +--> [VoicePipelineOrchestrator] (Reactive State Machine)
                |
                +--> [Engine A: ONNX Semantic Router]
                |      |
                |      +-- Confidence >= 0.70 --> [BYDVehicleController]
                |                                    | (Broadcast Intents / Media)
                |                                    v
                |                                 [TTS Sanitizer & Piper TTS]
                |
                +-- Confidence < 0.70 (Fallback)
                       |
                       +--> [DynamicContextManager]
                       |      +-- Location (GPS) & System Time
                       |      +-- AccessibilityScreenReader (Live UI Hierarchy)
                       |      +-- Room DB Memory RAG (Keyword Facts)
                       |
                       +--> [MCP Client Layer] (JSON-RPC 2.0 Tools)
                       |      +-- web_search tool (online query)
                       |      +-- screen_inspect tool
                       |
                       +--> [Engine B: llama.cpp GGUF LLM] (2-3 CPU threads)
                              | (Streaming Flow<String>)
                              v
                            [TTS Sanitizer] (Markdown/Emoji/Symbol cleanup)
                              v
                            [Piper TTS Engine] (VITS ONNX to AudioTrack)
```

---

## 3. Project Structure & Module Organization

The codebase is structured under clean architectural boundaries into discrete modular packages:

```
cdcdcd/
├── app/
│   └── src/main/
│       ├── java/com/byd/voiceassistant/
│       │   ├── di/                        # Dependency Injection / Service Locator
│       │   ├── presentation/
│       │   │   ├── overlay/               # Floating HUD Window & Waveform View
│       │   │   ├── settings/              # Settings & Diagnostics Activity
│       │   │   └── viewmodel/             # SettingsViewModel, DiagnosticsViewModel
│       │   ├── service/
│       │   │   ├── VoiceForegroundService.kt
│       │   │   └── VoiceAccessibilityService.kt
│       │   └── VoiceAssistantApp.kt
│       └── res/                           # Layouts, drawables, strings
├── core-audio/                            # Audio capture, Wake Word, Vosk STT, Piper TTS
│   ├── src/main/java/com/byd/voiceassistant/audio/
│   │   ├── capture/AudioRecordSource.kt
│   │   ├── wakeword/WakeWordDetector.kt
│   │   ├── stt/VoskSpeechRecognizer.kt
│   │   └── tts/
│   │       ├── PiperTtsEngine.kt
│   │       └── AndroidFallbackTtsEngine.kt
│   └── cpp/                               # JNI wrappers for Piper & Audio processing
├── core-ai/                               # Engine A (ONNX), Engine B (llama.cpp), TTS Sanitizer
│   ├── src/main/java/com/byd/voiceassistant/ai/
│   │   ├── router/
│   │   │   ├── OnnxSemanticRouter.kt
│   │   │   └── CarCommandCatalog.kt
│   │   ├── llm/
│   │   │   ├── LlamaCppEngine.kt
│   │   │   └── PromptTemplateBuilder.kt
│   │   └── sanitizer/
│   │       └── TtsSanitizer.kt
│   └── cpp/                               # JNI llama.cpp bridge (llama-jni.cpp, CMakeLists.txt)
├── core-vehicle/                          # BYD Vendor Intent Control & Simulation
│   └── src/main/java/com/byd/voiceassistant/vehicle/
│       ├── BYDVehicleController.kt
│       ├── SimulatedVehicleController.kt
│       ├── VehicleAction.kt
│       └── VehicleState.kt
├── core-context/                          # Location, Screen Reader, Room Memory RAG
│   └── src/main/java/com/byd/voiceassistant/context/
│       ├── location/LocationTimeProvider.kt
│       ├── accessibility/ScreenContextExtractor.kt
│       ├── storage/ModelStorageManager.kt
│       └── memory/
│           ├── AssistantMemoryDatabase.kt
│           ├── FactEntity.kt
│           ├── FactDao.kt
│           └── MemoryRetrievalService.kt
└── core-mcp/                              # Model Context Protocol JSON-RPC Client & Tools
    └── src/main/java/com/byd/voiceassistant/mcp/
        ├── McpClient.kt
        ├── McpProtocol.kt
        ├── tools/
        │   ├── WebSearchTool.kt
        │   └── ScreenInspectTool.kt
        └── McpToolRegistry.kt
```

---

## 4. Audio & Speech Processing Pipeline

### 4.1 Audio Capture (`AudioRecordSource`)
- **Format:** 16,000 Hz, 16-bit Mono PCM (`AudioFormat.ENCODING_PCM_16BIT`).
- **Buffer:** Double-buffered ring buffer chunked into 20ms frames (640 bytes).
- Emits raw PCM chunks across Kotlin Coroutine `SharedFlow<ByteArray>`.

### 4.2 Wake Word Engine (`WakeWordDetector`)
- Continuous evaluation of PCM stream while state is `WAKE_LISTENING`.
- Employs low-power acoustic keyword detection (Vosk Keyword / Porcupine model).
- Upon detection:
  - Generates an immediate audio earcon feedback tone (`ding.wav` or synthesized chime via `AudioTrack`).
  - Emits `WAKE_DETECTED` state and transitions to `QUERY_CAPTURING`.

### 4.3 Vosk STT Engine (`VoskSpeechRecognizer`)
- Ingests PCM frames into the native Vosk recognizer.
- Implements trailing silence detection (Voice Activity Detection):
  - After speech begins, if 1.2s of continuous silence is detected or a full sentence boundary is reached, recognition commits final transcript.
  - Automatically times out after 8s of total query duration to avoid perpetual listening.

### 4.4 Piper TTS Engine (`PiperTtsEngine`) & Android Fallback
- Piper VITS C++ engine executes ONNX voice models directly on CPU.
- Generates 22,050 Hz raw PCM audio played through `AudioTrack` with `AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` to duck background car media.
- If Piper model is not configured or fails to load, `AndroidFallbackTtsEngine` gracefully uses system `android.speech.tts.TextToSpeech`.

### 4.5 Voice-First TTS Sanitizer (`TtsSanitizer`)
Transforms raw LLM text into smooth spoken words:
- **Markdown & Code Stripping:** Strips markdown headers, asterisks (`**bold**`), backticks, lists, URLs, and code blocks (` ```...``` ` -> `code snippet omitted`).
- **Emoji Stripping:** Removes all Unicode emoji code points (e.g., U+1F600–U+1F64F, U+1F300–U+1F5FF).
- **Phonetic Unit Expansion:**
  - `°C` $\rightarrow$ "degrees Celsius"
  - `°F` $\rightarrow$ "degrees Fahrenheit"
  - `%` $\rightarrow$ "percent"
  - `km/h` $\rightarrow$ "kilometers per hour"
  - `mph` $\rightarrow$ "miles per hour"
  - `&` $\rightarrow$ "and"
  - `$` $\rightarrow$ "dollars"
  - `+` $\rightarrow$ "plus"

---

## 5. AI Inference & Routing Layer

### 5.1 Engine A: Fast Semantic Router (`OnnxSemanticRouter`)
- Runs a quantized Sentence Transformer (e.g. `all-MiniLM-L6-v2` ONNX) using `onnxruntime-android`.
- Produces normalized 384-dimensional dense vectors:
  $$\text{sim}(q, c) = \frac{q \cdot c}{\|q\| \|c\|}$$
- Target Catalog (`CarCommandCatalog`):
  - `AC_SET_TEMP(temp)`
  - `AC_TOGGLE(state)`
  - `WINDOW_CONTROL(position, target)`
  - `MEDIA_PLAY_PAUSE`
  - `MEDIA_NEXT_PREVIOUS`
  - `SEAT_HEATING(level)`
- **Confidence Gate:**
  - $\text{similarity} \ge 0.70$: Immediately dispatches action to `BYDVehicleController`, returns spoken confirmation without invoking LLM. Latency $<50$ms.
  - $\text{similarity} < 0.70$: Dispatches query to Engine B fallback.

### 5.2 Engine B: Fallback LLM (`LlamaCppEngine`)
- Direct JNI bindings to `llama.cpp` using native CMake configuration.
- **Resource Constraints for Car SoCs:**
  - Maximum CPU threads: 2 or 3 (`n_threads = 2`).
  - Context size: 1024 or 2048 tokens (`n_ctx = 1024`).
  - Quantization: GGUF Q4_K_M or Q4_0 models.
- Emits response tokens incrementally via `Flow<String>` to enable low first-token latency before feeding to the TTS pipeline.

---

## 6. Context Enrichment & MCP Layer

### 6.1 Dynamic Location & Time (`LocationTimeProvider`)
- Uses `FusedLocationProviderClient` with `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
- Fallback to Android `LocationManager` (GPS provider).
- Provides current coordinates, city/neighborhood name (via geocoder when online), and formatted system time (`yyyy-MM-dd HH:mm:ss`, day of week, timezone).

### 6.2 Screen Awareness (`VoiceAccessibilityService`)
- Android `AccessibilityService` monitoring `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`.
- Traverses the live `AccessibilityNodeInfo` hierarchy to produce a compact representation:
  - Top package name (e.g. `com.autonavi.amapauto`, `com.netease.cloudmusic`).
  - Primary text nodes, buttons, headings, and lists.
  - Formatted into a lightweight context block (max 300 tokens).

### 6.3 Long-Term Memory RAG (`MemoryRetrievalService`)
- Room Database: `facts` table with `(id, key, value, tags, timestamp)`.
- Fast keyword search: extracts query tokens and queries `FactDao` for matching records.
- Injects top-3 matching facts **only** into the active prompt turn, preventing token drift.

### 6.4 Model Context Protocol (MCP) Client
- Implements MCP 2024 JSON-RPC specification.
- Local MCP Tools registered:
  1. `web_search(query: string)`: Performs real-time internet search when connectivity is available.
  2. `screen_inspect()`: Queries `VoiceAccessibilityService` for active on-screen context.
- System prompt instructs Engine B to output structured tool calls:
  `[TOOL_CALL: web_search {"query": "weather in Shenzhen"}]`
- `McpClient` executes tool and injects result back into the prompt context for final answer synthesis.

---

## 7. Car Hardware Control Layer (`core-vehicle`)

### 7.1 BYD DiLink Integration (`BYDVehicleController`)
Dispatches system intents and vendor broadcasts matching DiLink specs:
- **HVAC:** `byd.intent.action.AC_CONTROL` with extras (`temperature`, `ac_switch`, `fan_speed`).
- **Windows:** `byd.intent.action.WINDOW_CONTROL` (`window_type`, `window_state`).
- **Media:** Broadcasts standard Android `KeyEvent.KEYCODE_MEDIA_*` via `AudioManager`.
- **Seat Comfort:** `byd.intent.action.SEAT_HEATING_CONTROL` (`seat_pos`, `level`).

### 7.2 Simulation Engine (`SimulatedVehicleController`)
Maintains reactive in-memory vehicle state (`VehicleState`):
- `cabinTemperature: Float` (default 22.0°C)
- `acEnabled: Boolean`
- `fanSpeed: Int` (1..7)
- `windowsOpenPercent: Map<WindowPosition, Int>`
- `mediaPlaying: Boolean`
Provides deterministic, testable verification on emulators and standard devices.

---

## 8. User Interface & Overlay

### 8.1 Floating HUD Overlay (`VoiceOverlayView`)
- Rendered via `WindowManager` with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
- Minimalist automotive dark mode styling:
  - Dynamic audio waveform / pulsing status orb.
  - Spoken query transcription in real-time.
  - Streaming assistant response.
  - Tap-to-dismiss and mute controls.
  - Auto-dismisses 3 seconds after TTS speech finishes.

### 8.2 Settings & Diagnostics Activity
- **Model Selector:** Uses `ActivityResultContracts.GetContent()` to select local `.gguf` Tiny LLMs and Piper `.onnx` models from device storage.
- **Model Path Storage:** Persisted in Jetpack DataStore.
- **Room Memory Manager:** Add, view, and delete remembered facts.
- **Diagnostics Dashboard:**
  - Real-time microphone input volume visualizer.
  - Engine readiness badges (Wake Word, Vosk, Piper, ONNX, llama.cpp, GPS, Accessibility).
  - Simulated vehicle state toggle switches.

---

## 9. Performance & Resource Budgets

| Metric | Target Budget | Mitigation Strategy |
|---|---|---|
| Engine A Route Latency | < 50ms | Quantized ONNX embeddings + in-memory cosine vectors |
| Engine B First Token Latency | < 800ms | 2–3 CPU threads, quantized GGUF (Q4_K_M), 1024 context |
| RAM Footprint | < 600MB total | Unload unused context, single model instance, native memory cleanup |
| Foreground Audio CPU | < 3% Idle | Efficient ring-buffer chunking, sleep between VAD frames |
| Audio Ducking | Instantaneous | Acquire transient audio focus with ducking on wake word trigger |

---

## 10. Verification & Test Plan

1. **Unit Tests:**
   - `OnnxSemanticRouterTest`: Semantic similarity scoring and threshold routing ($\ge 0.70$ vs $< 0.70$).
   - `TtsSanitizerTest`: Unit tests for regex rules (markdown stripping, emoji removal, phonetic symbol expansion).
   - `MemoryRetrievalServiceTest`: Room DB insertion, query matching, and turn-based context formatting.
   - `McpClientTest`: JSON-RPC protocol parsing and tool call execution.
   - `VehicleControllerTest`: Simulated vehicle state transitions and BYD intent creation.
2. **Integration & Build Verification:**
   - Complete compilation of Kotlin and CMake JNI modules.
   - Android Foreground Service lifecycle tests and state machine transitions.
   - End-to-end simulated speech turn (Audio $\rightarrow$ Wake Word $\rightarrow$ Vosk $\rightarrow$ Router $\rightarrow$ Piper/Sanitizer).

---

## 11. Security & Permissions Specification

The Android Manifest mandates the following permissions:
- `RECORD_AUDIO`: Continuous microphone capture.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MICROPHONE`: Background wake-word listening.
- `SYSTEM_ALERT_WINDOW`: Displaying the floating HUD overlay.
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: GPS context for navigation/weather.
- `INTERNET` & `ACCESS_NETWORK_STATE`: MCP web search when online.
- `BIND_ACCESSIBILITY_SERVICE`: Live screen awareness extraction.
- `WAKE_LOCK`: Preventing CPU sleep while processing audio and inference turns.
