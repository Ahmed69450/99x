package com.byd.voiceassistant.audio;

import com.byd.voiceassistant.audio.tts.AndroidFallbackTtsEngine;
import com.byd.voiceassistant.audio.tts.PiperTtsEngine;
import com.byd.voiceassistant.audio.wakeword.WakeWordDetector;
import com.byd.voiceassistant.audio.wakeword.WakeWordListener;
import org.junit.Assert;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPipelineUnitTest {

    @Test
    public void testTtsFallsBackGracefullyWhenModelMissing() {
        AndroidFallbackTtsEngine fallback = new AndroidFallbackTtsEngine(null);
        PiperTtsEngine tts = new PiperTtsEngine("/invalid/path/piper.onnx", fallback);

        String response = tts.speak("Adjusting temperature to 22 degrees Celsius");
        Assert.assertNotNull(response);
        Assert.assertTrue(response.contains("22 degrees Celsius"));
    }

    @Test
    public void testWakeWordDetectionTrigger() {
        final AtomicBoolean triggered = new AtomicBoolean(false);
        WakeWordDetector detector = new WakeWordDetector(new WakeWordListener() {
            @Override
            public void onWakeWordDetected(String keyword) {
                triggered.set(true);
            }
        });

        detector.simulateDetection("hey byd");
        Assert.assertTrue(triggered.get());
    }
}
