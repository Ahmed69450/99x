package com.byd.voiceassistant;

import android.app.Application;

public class VoiceAssistantApp extends Application {
    private static VoiceAssistantApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static VoiceAssistantApp getInstance() {
        return instance;
    }
}
