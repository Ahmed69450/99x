package com.byd.voiceassistant.vehicle;

public abstract class VehicleAction {

    public static class SetAcTemperature extends VehicleAction {
        private final float targetCelsius;

        public SetAcTemperature(float targetCelsius) {
            this.targetCelsius = targetCelsius;
        }

        public float getTargetCelsius() {
            return targetCelsius;
        }

        @Override
        public String toString() {
            return "SetAcTemperature(" + targetCelsius + "C)";
        }
    }

    public static class AdjustAcTemperature extends VehicleAction {
        private final float deltaCelsius;

        public AdjustAcTemperature(float deltaCelsius) {
            this.deltaCelsius = deltaCelsius;
        }

        public float getDeltaCelsius() {
            return deltaCelsius;
        }

        @Override
        public String toString() {
            return "AdjustAcTemperature(" + deltaCelsius + "C)";
        }
    }

    public static class SetAcEnabled extends VehicleAction {
        private final boolean enabled;

        public SetAcEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public String toString() {
            return "SetAcEnabled(" + enabled + ")";
        }
    }

    public static class SetFanSpeed extends VehicleAction {
        private final int speed;

        public SetFanSpeed(int speed) {
            this.speed = speed;
        }

        public int getSpeed() {
            return speed;
        }

        @Override
        public String toString() {
            return "SetFanSpeed(" + speed + ")";
        }
    }

    public static class SetWindowPosition extends VehicleAction {
        private final WindowPosition position;
        private final int openPercent;

        public SetWindowPosition(WindowPosition position, int openPercent) {
            this.position = position;
            this.openPercent = openPercent;
        }

        public WindowPosition getPosition() {
            return position;
        }

        public int getOpenPercent() {
            return openPercent;
        }

        @Override
        public String toString() {
            return "SetWindowPosition(" + position + ", " + openPercent + "%)";
        }
    }

    public static class SetSeatHeating extends VehicleAction {
        private final SeatPosition position;
        private final int level;

        public SetSeatHeating(SeatPosition position, int level) {
            this.position = position;
            this.level = level;
        }

        public SeatPosition getPosition() {
            return position;
        }

        public int getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return "SetSeatHeating(" + position + ", level=" + level + ")";
        }
    }

    public static class MediaPlay extends VehicleAction {
        @Override
        public String toString() {
            return "MediaPlay";
        }
    }

    public static class MediaPause extends VehicleAction {
        @Override
        public String toString() {
            return "MediaPause";
        }
    }

    public static class MediaNext extends VehicleAction {
        @Override
        public String toString() {
            return "MediaNext";
        }
    }

    public static class MediaPrevious extends VehicleAction {
        @Override
        public String toString() {
            return "MediaPrevious";
        }
    }
}
