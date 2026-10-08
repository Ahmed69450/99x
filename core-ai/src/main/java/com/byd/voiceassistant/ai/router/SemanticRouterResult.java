package com.byd.voiceassistant.ai.router;

import com.byd.voiceassistant.vehicle.VehicleAction;

public class SemanticRouterResult {
    private final boolean match;
    private final float confidence;
    private final VehicleAction action;

    public SemanticRouterResult(boolean match, float confidence, VehicleAction action) {
        this.match = match;
        this.confidence = confidence;
        this.action = action;
    }

    public static SemanticRouterResult match(VehicleAction action, float confidence) {
        return new SemanticRouterResult(true, confidence, action);
    }

    public static SemanticRouterResult noMatch(float highestConfidence) {
        return new SemanticRouterResult(false, highestConfidence, null);
    }

    public boolean isMatch() {
        return match;
    }

    public float getConfidence() {
        return confidence;
    }

    public VehicleAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "SemanticRouterResult(match=" + match + ", confidence=" + confidence + ", action=" + action + ")";
    }
}
