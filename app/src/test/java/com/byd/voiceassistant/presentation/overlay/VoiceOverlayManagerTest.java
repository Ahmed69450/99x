package com.byd.voiceassistant.presentation.overlay;

import com.byd.voiceassistant.orchestrator.VoiceState;
import org.junit.Assert;
import org.junit.Test;

public class VoiceOverlayManagerTest {

    @Test
    public void testOverlayStateTracking() {
        VoiceOverlayManager manager = new VoiceOverlayManager(null);
        Assert.assertFalse(manager.isShowing());

        manager.showOverlay("Listening...");
        Assert.assertTrue(manager.isShowing());
        Assert.assertEquals("Listening...", manager.getLastTranscript());

        manager.updateState(VoiceState.SPEAKING);
        Assert.assertEquals(VoiceState.SPEAKING, manager.getCurrentVoiceState());

        manager.updateResponse("Setting temperature to 23 degrees Celsius.");
        Assert.assertEquals("Setting temperature to 23 degrees Celsius.", manager.getLastResponse());

        manager.dismissOverlay();
        Assert.assertFalse(manager.isShowing());
    }
}
