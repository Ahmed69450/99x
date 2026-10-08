package com.byd.voiceassistant.audio.stt;

public class VoskSpeechRecognizer {

    public static final long DEFAULT_SILENCE_TIMEOUT_MS = 1200; // 1.2s trailing silence

    private final SpeechRecognitionListener listener;
    private final String modelPath;
    private volatile boolean isRecognizing = false;
    private long lastAudioTime = 0;
    private StringBuilder currentHypothesis = new StringBuilder();

    public VoskSpeechRecognizer(String modelPath, SpeechRecognitionListener listener) {
        this.modelPath = modelPath;
        this.listener = listener;
    }

    public synchronized void start() {
        this.isRecognizing = true;
        this.lastAudioTime = System.currentTimeMillis();
        this.currentHypothesis.setLength(0);
    }

    public synchronized void stop() {
        if (isRecognizing) {
            isRecognizing = false;
            if (listener != null && currentHypothesis.length() > 0) {
                listener.onFinalResult(currentHypothesis.toString().trim());
            }
        }
    }

    public synchronized void processAudioChunk(byte[] pcmData) {
        if (!isRecognizing || pcmData == null) {
            return;
        }

        lastAudioTime = System.currentTimeMillis();
    }

    public synchronized void checkSilenceTimeout() {
        if (isRecognizing && lastAudioTime > 0) {
            long elapsed = System.currentTimeMillis() - lastAudioTime;
            if (elapsed >= DEFAULT_SILENCE_TIMEOUT_MS && currentHypothesis.length() > 0) {
                stop();
            }
        }
    }

    public synchronized void simulateSpeechResult(String transcript) {
        this.currentHypothesis.setLength(0);
        this.currentHypothesis.append(transcript);
        if (listener != null) {
            listener.onFinalResult(transcript);
        }
    }

    public boolean isRecognizing() {
        return isRecognizing;
    }
}
