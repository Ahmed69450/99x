package com.byd.voiceassistant.service;

import org.junit.Assert;
import org.junit.Test;

public class ServiceUnitTest {

    @Test
    public void testAccessibilityServiceStoresAndFormatsScreenText() {
        VoiceAccessibilityService.updateScreenContext(
            "com.autonavi.amapauto",
            "Route: Airport Express, Remaining: 12 km, Next Exit: Guanshen Hwy"
        );

        String context = VoiceAccessibilityService.getLastScreenContext();
        Assert.assertNotNull(context);
        Assert.assertTrue(context.contains("com.autonavi.amapauto"));
        Assert.assertTrue(context.contains("Airport Express"));
    }

    @Test
    public void testVoiceForegroundServiceConstants() {
        Assert.assertEquals("voice_service_channel", VoiceForegroundService.CHANNEL_ID);
        Assert.assertEquals(1001, VoiceForegroundService.NOTIFICATION_ID);
    }
}
