package com.byd.voiceassistant.vehicle;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class VehicleState {
    private final float cabinTemperature;
    private final boolean acEnabled;
    private final int fanSpeed;
    private final Map<WindowPosition, Integer> windows;
    private final Map<SeatPosition, Integer> seatHeating;
    private final boolean mediaPlaying;

    public VehicleState() {
        this(22.0f, false, 1, createDefaultWindows(), createDefaultSeats(), false);
    }

    public VehicleState(float cabinTemperature, boolean acEnabled, int fanSpeed,
                        Map<WindowPosition, Integer> windows,
                        Map<SeatPosition, Integer> seatHeating,
                        boolean mediaPlaying) {
        this.cabinTemperature = cabinTemperature;
        this.acEnabled = acEnabled;
        this.fanSpeed = fanSpeed;
        this.windows = Collections.unmodifiableMap(new EnumMap<>(windows));
        this.seatHeating = Collections.unmodifiableMap(new EnumMap<>(seatHeating));
        this.mediaPlaying = mediaPlaying;
    }

    private static Map<WindowPosition, Integer> createDefaultWindows() {
        Map<WindowPosition, Integer> map = new EnumMap<>(WindowPosition.class);
        for (WindowPosition pos : WindowPosition.values()) {
            map.put(pos, 0);
        }
        return map;
    }

    private static Map<SeatPosition, Integer> createDefaultSeats() {
        Map<SeatPosition, Integer> map = new EnumMap<>(SeatPosition.class);
        for (SeatPosition pos : SeatPosition.values()) {
            map.put(pos, 0);
        }
        return map;
    }

    public float getCabinTemperature() {
        return cabinTemperature;
    }

    public boolean isAcEnabled() {
        return acEnabled;
    }

    public int getFanSpeed() {
        return fanSpeed;
    }

    public Map<WindowPosition, Integer> getWindows() {
        return windows;
    }

    public Map<SeatPosition, Integer> getSeatHeating() {
        return seatHeating;
    }

    public boolean isMediaPlaying() {
        return mediaPlaying;
    }
}
