package com.byd.voiceassistant.audio.wakeword;

public interface WakeWordListener {
    void onWakeWordDetected(String keyword);
}
