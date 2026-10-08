package com.byd.voiceassistant.vehicle;

import java.util.EnumMap;
import java.util.Map;

public class SimulatedVehicleController implements CarController {
    public static final float MIN_TEMP = 16.0f;
    public static final float MAX_TEMP = 32.0f;
    public static final int MIN_FAN = 1;
    public static final int MAX_FAN = 7;
    public static final int MAX_SEAT_LEVEL = 3;

    private float cabinTemperature = 22.0f;
    private boolean acEnabled = false;
    private int fanSpeed = 1;
    private final Map<WindowPosition, Integer> windows = new EnumMap<>(WindowPosition.class);
    private final Map<SeatPosition, Integer> seatHeating = new EnumMap<>(SeatPosition.class);
    private boolean mediaPlaying = false;

    public SimulatedVehicleController() {
        for (WindowPosition pos : WindowPosition.values()) {
            windows.put(pos, 0);
        }
        for (SeatPosition pos : SeatPosition.values()) {
            seatHeating.put(pos, 0);
        }
    }

    @Override
    public synchronized VehicleState getCurrentState() {
        return new VehicleState(cabinTemperature, acEnabled, fanSpeed, windows, seatHeating, mediaPlaying);
    }

    @Override
    public synchronized CarExecutionResult execute(VehicleAction action) {
        if (action instanceof VehicleAction.SetAcTemperature) {
            float requested = ((VehicleAction.SetAcTemperature) action).getTargetCelsius();
            float clamped = Math.max(MIN_TEMP, Math.min(MAX_TEMP, requested));
            this.cabinTemperature = clamped;
            this.acEnabled = true;
            return new CarExecutionResult(
                true,
                "Setting cabin temperature to " + clamped + " degrees Celsius.",
                action
            );
        } else if (action instanceof VehicleAction.AdjustAcTemperature) {
            float delta = ((VehicleAction.AdjustAcTemperature) action).getDeltaCelsius();
            float newTemp = Math.max(MIN_TEMP, Math.min(MAX_TEMP, this.cabinTemperature + delta));
            this.cabinTemperature = newTemp;
            return new CarExecutionResult(
                true,
                "Adjusted cabin temperature to " + newTemp + " degrees Celsius.",
                action
            );
        } else if (action instanceof VehicleAction.SetAcEnabled) {
            boolean enabled = ((VehicleAction.SetAcEnabled) action).isEnabled();
            this.acEnabled = enabled;
            String stateStr = enabled ? "turned on" : "turned off";
            return new CarExecutionResult(
                true,
                "Air conditioning is now " + stateStr + ".",
                action
            );
        } else if (action instanceof VehicleAction.SetFanSpeed) {
            int speed = ((VehicleAction.SetFanSpeed) action).getSpeed();
            int clamped = Math.max(MIN_FAN, Math.min(MAX_FAN, speed));
            this.fanSpeed = clamped;
            return new CarExecutionResult(
                true,
                "Fan speed set to " + clamped + ".",
                action
            );
        } else if (action instanceof VehicleAction.SetWindowPosition) {
            VehicleAction.SetWindowPosition windowAction = (VehicleAction.SetWindowPosition) action;
            int clamped = Math.max(0, Math.min(100, windowAction.getOpenPercent()));
            if (windowAction.getPosition() == WindowPosition.ALL) {
                for (WindowPosition pos : WindowPosition.values()) {
                    windows.put(pos, clamped);
                }
            } else {
                windows.put(windowAction.getPosition(), clamped);
            }
            return new CarExecutionResult(
                true,
                "Windows adjusted to " + clamped + " percent open.",
                action
            );
        } else if (action instanceof VehicleAction.SetSeatHeating) {
            VehicleAction.SetSeatHeating seatAction = (VehicleAction.SetSeatHeating) action;
            int clamped = Math.max(0, Math.min(MAX_SEAT_LEVEL, seatAction.getLevel()));
            if (seatAction.getPosition() == SeatPosition.ALL) {
                for (SeatPosition pos : SeatPosition.values()) {
                    seatHeating.put(pos, clamped);
                }
            } else {
                seatHeating.put(seatAction.getPosition(), clamped);
            }
            return new CarExecutionResult(
                true,
                "Seat heating set to level " + clamped + ".",
                action
            );
        } else if (action instanceof VehicleAction.MediaPlay) {
            this.mediaPlaying = true;
            return new CarExecutionResult(true, "Resuming media playback.", action);
        } else if (action instanceof VehicleAction.MediaPause) {
            this.mediaPlaying = false;
            return new CarExecutionResult(true, "Pausing media playback.", action);
        } else if (action instanceof VehicleAction.MediaNext) {
            return new CarExecutionResult(true, "Skipping to next track.", action);
        } else if (action instanceof VehicleAction.MediaPrevious) {
            return new CarExecutionResult(true, "Playing previous track.", action);
        }

        return new CarExecutionResult(false, "Unknown vehicle action.", action);
    }
}
