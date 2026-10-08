package com.byd.voiceassistant.orchestrator;

import com.byd.voiceassistant.ai.llm.LlmEngine;
import com.byd.voiceassistant.ai.router.OnnxSemanticRouter;
import com.byd.voiceassistant.context.location.LocationData;
import com.byd.voiceassistant.context.location.LocationTimeProvider;
import com.byd.voiceassistant.context.memory.MemoryRetrievalService;
import com.byd.voiceassistant.mcp.McpClientEngine;
import com.byd.voiceassistant.vehicle.SimulatedVehicleController;
import org.junit.Assert;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;

public class VoicePipelineOrchestratorTest {

    @Test
    public void testVehicleCommandFastRoutesWithoutLlm() {
        SimulatedVehicleController vehicle = new SimulatedVehicleController();
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        final AtomicBoolean llmCalled = new AtomicBoolean(false);

        LlmEngine mockLlm = new LlmEngine() {
            @Override
            public boolean loadModel(String modelPath) { return true; }
            @Override
            public String generate(String prompt) {
                llmCalled.set(true);
                return "Mock LLM Response";
            }
            @Override
            public void setThreadCount(int threads) {}
            @Override
            public int getThreadCount() { return 2; }
            @Override
            public void release() {}
        };

        VoicePipelineOrchestrator orchestrator = new VoicePipelineOrchestrator(
            router,
            vehicle,
            mockLlm,
            new McpClientEngine(),
            new LocationTimeProvider(null),
            new MemoryRetrievalService()
        );

        String result = orchestrator.processUtterance("set temperature to 21 degrees");
        Assert.assertTrue(result.contains("21"));
        Assert.assertEquals(21.0f, vehicle.getCurrentState().getCabinTemperature(), 0.01f);
        Assert.assertFalse("LLM should not be called for fast routed vehicle command", llmCalled.get());
        Assert.assertEquals(VoiceState.IDLE, orchestrator.getCurrentState());
    }

    @Test
    public void testGeneralQueryInvokesLlmFallback() {
        SimulatedVehicleController vehicle = new SimulatedVehicleController();
        OnnxSemanticRouter router = new OnnxSemanticRouter();
        final AtomicBoolean llmCalled = new AtomicBoolean(false);

        LlmEngine mockLlm = new LlmEngine() {
            @Override
            public boolean loadModel(String modelPath) { return true; }
            @Override
            public String generate(String prompt) {
                llmCalled.set(true);
                return "The battery level is 85 percent.";
            }
            @Override
            public void setThreadCount(int threads) {}
            @Override
            public int getThreadCount() { return 2; }
            @Override
            public void release() {}
        };

        VoicePipelineOrchestrator orchestrator = new VoicePipelineOrchestrator(
            router,
            vehicle,
            mockLlm,
            new McpClientEngine(),
            new LocationTimeProvider(null),
            new MemoryRetrievalService()
        );

        String result = orchestrator.processUtterance("How is my battery?");
        Assert.assertTrue(llmCalled.get());
        Assert.assertTrue(result.contains("85 percent"));
        Assert.assertEquals(VoiceState.IDLE, orchestrator.getCurrentState());
    }
}
