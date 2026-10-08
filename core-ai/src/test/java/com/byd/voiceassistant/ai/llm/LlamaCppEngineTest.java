package com.byd.voiceassistant.ai.llm;

import org.junit.Assert;
import org.junit.Test;

public class LlamaCppEngineTest {

    @Test
    public void testThreadClamping() {
        LlamaCppEngine engine = new LlamaCppEngine();
        // Requesting 8 threads on car head unit should clamp to 3 max
        engine.setThreadCount(8);
        Assert.assertTrue(engine.getThreadCount() <= 3);
        Assert.assertTrue(engine.getThreadCount() >= 2);

        // Requesting 1 thread should clamp to 2 min
        engine.setThreadCount(1);
        Assert.assertEquals(2, engine.getThreadCount());
    }

    @Test
    public void testFallbackGenerationWhenNativeNotLoaded() {
        LlamaCppEngine engine = new LlamaCppEngine();
        String response = engine.generate("Hello assistant");
        Assert.assertNotNull(response);
        Assert.assertFalse(response.trim().isEmpty());
    }

    @Test
    public void testLoadModelNonExistentFails() {
        LlamaCppEngine engine = new LlamaCppEngine();
        Assert.assertFalse(engine.loadModel("/invalid/nonexistent/model.gguf"));
    }
}
