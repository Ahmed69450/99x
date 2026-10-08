package com.byd.voiceassistant.context.location;

public class LocationData {
    private final double latitude;
    private final double longitude;
    private final String locality;

    public LocationData(double latitude, double longitude, String locality) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locality = locality;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getLocality() {
        return locality;
    }

    @Override
    public String toString() {
        return "Latitude: " + latitude + ", Longitude: " + longitude + (locality != null ? ", " + locality : "");
    }
}
