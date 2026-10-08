package com.byd.voiceassistant.context.location;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class LocationTimeProvider {

    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss, EEEE", Locale.US);
    public static final long CACHE_TTL_MS = 10000; // 10 seconds TTL for vehicle motion

    private final Context context;
    private final Clock clock;
    private LocationData cachedLocation;
    private long lastLocationFetchTime = 0;

    public LocationTimeProvider(Context context) {
        this(context, Clock.systemDefaultZone());
    }

    public LocationTimeProvider(Context context, Clock clock) {
        this.context = context;
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
        this.cachedLocation = null;
    }

    public ContextSnapshot getCurrentContext() {
        return getCurrentContext(null);
    }

    public ContextSnapshot getCurrentContext(LocationData overrideLocation) {
        String formattedTime = ZonedDateTime.now(clock).format(TIME_FORMATTER);

        if (overrideLocation != null) {
            this.cachedLocation = overrideLocation;
            return new ContextSnapshot(overrideLocation, formattedTime);
        }

        LocationData resolvedLocation = fetchSystemLocation();
        return new ContextSnapshot(resolvedLocation, formattedTime);
    }

    private LocationData fetchSystemLocation() {
        long now = System.currentTimeMillis();
        if (cachedLocation != null && (now - lastLocationFetchTime) < CACHE_TTL_MS) {
            return cachedLocation;
        }

        if (context == null) {
            return cachedLocation;
        }

        try {
            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown == null) {
                    lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (lastKnown != null) {
                    cachedLocation = new LocationData(
                        lastKnown.getLatitude(),
                        lastKnown.getLongitude(),
                        "AutoLocality"
                    );
                    lastLocationFetchTime = now;
                    return cachedLocation;
                }
            }
        } catch (SecurityException ignored) {
            // Missing ACCESS_FINE_LOCATION permission
        } catch (Exception ignored) {
        }

        return cachedLocation;
    }
}
