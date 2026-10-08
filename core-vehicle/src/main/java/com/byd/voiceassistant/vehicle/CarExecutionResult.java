package com.byd.voiceassistant.vehicle;

public class CarExecutionResult {
    private final boolean success;
    private final String speechResponse;
    private final VehicleAction actionTaken;

    public CarExecutionResult(boolean success, String speechResponse, VehicleAction actionTaken) {
        this.success = success;
        this.speechResponse = speechResponse;
        this.actionTaken = actionTaken;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSpeechResponse() {
        return speechResponse;
    }

    public VehicleAction getActionTaken() {
        return actionTaken;
    }

    @Override
    public String toString() {
        return "CarExecutionResult(success=" + success + ", speech='" + speechResponse + "')";
    }
}
