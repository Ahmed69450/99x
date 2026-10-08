package com.byd.voiceassistant.context.location;

import org.junit.Assert;
import org.junit.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class LocationTimeProviderTest {

    @Test
    public void testFormattedTimeGeneratedAccurately() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-08T14:30:00Z"), ZoneId.of("UTC"));
        LocationTimeProvider provider = new LocationTimeProvider(null, fixedClock);
        LocationData mockLocation = new LocationData(22.5431, 114.0579, "Shenzhen");

        ContextSnapshot snapshot = provider.getCurrentContext(mockLocation);

        Assert.assertNotNull(snapshot.getLocation());
        Assert.assertEquals("Shenzhen", snapshot.getLocation().getLocality());
        Assert.assertTrue(snapshot.getFormattedTime().contains("2026-10-08 14:30:00"));
        Assert.assertTrue(snapshot.getFormattedTime().contains("Thursday"));
    }
}
