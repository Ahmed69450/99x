package com.byd.voiceassistant.context.location;

public class ContextSnapshot {
    private final LocationData location;
    private final String formattedTime;

    public ContextSnapshot(LocationData location, String formattedTime) {
        this.location = location;
        this.formattedTime = formattedTime;
    }

    public LocationData getLocation() {
        return location;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    @Override
    public String toString() {
        return "ContextSnapshot(location=" + location + ", time='" + formattedTime + "')";
    }
}
