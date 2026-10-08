package com.byd.voiceassistant.vehicle;

public interface CarController {
    VehicleState getCurrentState();
    CarExecutionResult execute(VehicleAction action);
}
