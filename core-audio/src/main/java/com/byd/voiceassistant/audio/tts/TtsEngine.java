package com.byd.voiceassistant.audio.tts;

public interface TtsEngine {
    String speak(String text);
    void stop();
    void release();
}
