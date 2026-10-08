package com.byd.voiceassistant.vehicle;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

public class BYDVehicleController implements CarController {

    public static final String ACTION_BYD_AC = "byd.intent.action.AC_CONTROL";
    public static final String ACTION_BYD_WINDOW = "byd.intent.action.WINDOW_CONTROL";
    public static final String ACTION_BYD_SEAT = "byd.intent.action.SEAT_HEATING_CONTROL";

    private final Context context;
    private final SimulatedVehicleController fallbackSimulation;

    public BYDVehicleController(Context context) {
        this.context = context;
        this.fallbackSimulation = new SimulatedVehicleController();
    }

    @Override
    public VehicleState getCurrentState() {
        return fallbackSimulation.getCurrentState();
    }

    @Override
    public CarExecutionResult execute(VehicleAction action) {
        // Update local state model
        CarExecutionResult result = fallbackSimulation.execute(action);

        if (context == null) {
            return result;
        }

        try {
            if (action instanceof VehicleAction.SetAcTemperature) {
                float rawTemp = ((VehicleAction.SetAcTemperature) action).getTargetCelsius();
                float clampedTemp = Math.max(SimulatedVehicleController.MIN_TEMP,
                                    Math.min(SimulatedVehicleController.MAX_TEMP, rawTemp));
                Intent intent = new Intent(ACTION_BYD_AC);
                intent.putExtra("temperature", clampedTemp);
                intent.putExtra("ac_switch", true);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.AdjustAcTemperature) {
                float newTemp = fallbackSimulation.getCurrentState().getCabinTemperature();
                Intent intent = new Intent(ACTION_BYD_AC);
                intent.putExtra("temperature", newTemp);
                intent.putExtra("ac_switch", true);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.SetAcEnabled) {
                boolean enabled = ((VehicleAction.SetAcEnabled) action).isEnabled();
                Intent intent = new Intent(ACTION_BYD_AC);
                intent.putExtra("ac_switch", enabled);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.SetFanSpeed) {
                int rawSpeed = ((VehicleAction.SetFanSpeed) action).getSpeed();
                int clampedSpeed = Math.max(SimulatedVehicleController.MIN_FAN,
                                   Math.min(SimulatedVehicleController.MAX_FAN, rawSpeed));
                Intent intent = new Intent(ACTION_BYD_AC);
                intent.putExtra("fan_speed", clampedSpeed);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.SetWindowPosition) {
                VehicleAction.SetWindowPosition win = (VehicleAction.SetWindowPosition) action;
                int clampedOpen = Math.max(0, Math.min(100, win.getOpenPercent()));
                Intent intent = new Intent(ACTION_BYD_WINDOW);
                intent.putExtra("window_type", win.getPosition().name());
                intent.putExtra("open_percent", clampedOpen);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.SetSeatHeating) {
                VehicleAction.SetSeatHeating seat = (VehicleAction.SetSeatHeating) action;
                int clampedLevel = Math.max(0, Math.min(SimulatedVehicleController.MAX_SEAT_LEVEL, seat.getLevel()));
                Intent intent = new Intent(ACTION_BYD_SEAT);
                intent.putExtra("seat_position", seat.getPosition().name());
                intent.putExtra("level", clampedLevel);
                context.sendBroadcast(intent);
            } else if (action instanceof VehicleAction.MediaPlay) {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY);
            } else if (action instanceof VehicleAction.MediaPause) {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
            } else if (action instanceof VehicleAction.MediaNext) {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            } else if (action instanceof VehicleAction.MediaPrevious) {
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        } catch (Exception ignored) {
            // Safe fallback if vendor intent fails on standard android
        }

        return result;
    }

    private void dispatchMediaKey(int keyCode) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                KeyEvent eventDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                KeyEvent eventUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
                audioManager.dispatchMediaKeyEvent(eventDown);
                audioManager.dispatchMediaKeyEvent(eventUp);
            }
        } catch (Exception ignored) {
        }
    }
}
