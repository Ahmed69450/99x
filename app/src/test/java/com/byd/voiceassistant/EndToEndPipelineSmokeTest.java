package com.byd.voiceassistant;

import com.byd.voiceassistant.ai.llm.LlmEngine;
import com.byd.voiceassistant.ai.router.OnnxSemanticRouter;
import com.byd.voiceassistant.ai.sanitizer.TtsSanitizer;
import com.byd.voiceassistant.context.location.LocationData;
import com.byd.voiceassistant.context.location.LocationTimeProvider;
import com.byd.voiceassistant.context.memory.MemoryRetrievalService;
import com.byd.voiceassistant.mcp.McpClientEngine;
import com.byd.voiceassistant.mcp.ToolResult;
import com.byd.voiceassistant.mcp.tools.ScreenInspectTool;
import com.byd.voiceassistant.orchestrator.VoicePipelineOrchestrator;
import com.byd.voiceassistant.orchestrator.VoiceState;
import com.byd.voiceassistant.vehicle.SimulatedVehicleController;
import org.junit.Assert;
import org.junit.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class EndToEndPipelineSmokeTest {

    @Test
    public void testFullVehicleCommandTurn() {
        SimulatedVehicleController vehicle = new SimulatedVehicleController();
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-08T14:30:00Z"), ZoneId.of("UTC"));
        LocationTimeProvider locationProvider = new LocationTimeProvider(null, fixedClock);
        MemoryRetrievalService memory = new MemoryRetrievalService();
        McpClientEngine mcp = new McpClientEngine();

        LlmEngine mockLlm = new LlmEngine() {
            @Override public boolean loadModel(String p) { return true; }
            @Override public String generate(String p) { return "LLM not needed"; }
            @Override public void setThreadCount(int t) {}
            @Override public int getThreadCount() { return 2; }
            @Override public void release() {}
        };

        VoicePipelineOrchestrator orchestrator = new VoicePipelineOrchestrator(
            router,
            vehicle,
            mockLlm,
            mcp,
            locationProvider,
            memory
        );

        // Turn: User says "set temperature to 25 degrees"
        String response = orchestrator.processUtterance("set temperature to 25 degrees");
        String sanitized = TtsSanitizer.sanitize(response);

        Assert.assertTrue(sanitized.contains("25"));
        Assert.assertTrue(sanitized.contains("degrees Celsius"));
        Assert.assertEquals(25.0f, vehicle.getCurrentState().getCabinTemperature(), 0.01f);
        Assert.assertEquals(VoiceState.IDLE, orchestrator.getCurrentState());
    }

    @Test
    public void testFullMcpScreenAwarenessTurn() {
        McpClientEngine mcpEngine = new McpClientEngine();
        mcpEngine.registerTool(new ScreenInspectTool(new ScreenInspectTool.ScreenContentProvider() {
            @Override
            public String getScreenContent() {
                return "Active App: DiLink Navigation, Route: Airport 15km";
            }
        }));

        ToolResult result = mcpEngine.executeTool("screen_inspect", "{}");
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getContent().contains("DiLink Navigation"));

        String sanitizedSpeech = TtsSanitizer.sanitize("You are navigating to Airport, about 15km remaining.");
        Assert.assertEquals("You are navigating to Airport, about 15 kilometers remaining.", sanitizedSpeech);
    }

    @Test
    public void testFullOfflineFallbackTurnWithFacts() {
        SimulatedVehicleController vehicle = new SimulatedVehicleController();
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-08T14:30:00Z"), ZoneId.of("UTC"));
        LocationTimeProvider locationProvider = new LocationTimeProvider(null, fixedClock);
        MemoryRetrievalService memory = new MemoryRetrievalService();
        memory.saveFact("driver_pref", "User likes cabin temperature at 22 degrees", "pref,climate,temperature");

        McpClientEngine mcp = new McpClientEngine();

        LlmEngine testLlm = new LlmEngine() {
            @Override public boolean loadModel(String p) { return true; }
            @Override public String generate(String p) {
                // Assert context was injected into prompt
                Assert.assertTrue(p.contains("User likes cabin temperature at 22 degrees"));
                Assert.assertTrue(p.contains("2026-10-08 14:30:00"));
                return "Based on your preferences, the cabin is set up comfortably.";
            }
            @Override public void setThreadCount(int t) {}
            @Override public int getThreadCount() { return 2; }
            @Override public void release() {}
        };

        VoicePipelineOrchestrator orchestrator = new VoicePipelineOrchestrator(
            router,
            vehicle,
            testLlm,
            mcp,
            locationProvider,
            memory
        );

        String speech = orchestrator.processUtterance("What are my climate preferences for this drive?");
        Assert.assertTrue(speech.contains("cabin is set up comfortably"));
        Assert.assertEquals(VoiceState.IDLE, orchestrator.getCurrentState());
    }
}
