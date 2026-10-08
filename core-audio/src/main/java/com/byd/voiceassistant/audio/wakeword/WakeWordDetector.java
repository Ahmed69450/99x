package com.byd.voiceassistant.audio.wakeword;

public class WakeWordDetector {

    private final WakeWordListener listener;
    private boolean isListening = false;

    public WakeWordDetector(WakeWordListener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        this.isListening = true;
    }

    public synchronized void stop() {
        this.isListening = false;
    }

    public synchronized boolean isListening() {
        return isListening;
    }

    public synchronized boolean processPcmChunk(byte[] pcmData) {
        if (!isListening || pcmData == null) {
            return false;
        }

        // Acoustic wake-word keyword checking
        return false;
    }

    public synchronized void simulateDetection(String keyword) {
        if (listener != null) {
            listener.onWakeWordDetected(keyword != null ? keyword : "hey byd");
        }
    }
}
