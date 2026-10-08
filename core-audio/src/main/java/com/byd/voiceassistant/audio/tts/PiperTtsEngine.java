package com.byd.voiceassistant.audio.tts;

import java.io.File;

public class PiperTtsEngine implements TtsEngine {

    private final String modelPath;
    private final TtsEngine fallbackEngine;
    private boolean isNativeLoaded = false;
    private long nativeHandle = 0;

    static {
        try {
            System.loadLibrary("piper-android");
        } catch (UnsatisfiedLinkError ignored) {
            // Running in unit tests / JVM environment without .so
        }
    }

    public PiperTtsEngine(String modelPath, TtsEngine fallbackEngine) {
        this.modelPath = modelPath;
        this.fallbackEngine = fallbackEngine;

        if (modelPath != null && new File(modelPath).exists() && isNativeLoaded) {
            try {
                this.nativeHandle = nativeInitPiper(modelPath);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public synchronized String speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // If native Piper model is loaded, synthesize audio directly
        if (nativeHandle != 0) {
            try {
                byte[] audioPcm = nativeSynthesize(nativeHandle, text);
                if (audioPcm != null && audioPcm.length > 0) {
                    return "[Piper VITS Synthesized]: " + text;
                }
            } catch (Exception ignored) {
            }
        }

        // Seamless fallback to system TTS
        if (fallbackEngine != null) {
            return fallbackEngine.speak(text);
        }

        return text;
    }

    @Override
    public synchronized void stop() {
        if (fallbackEngine != null) {
            fallbackEngine.stop();
        }
    }

    @Override
    public synchronized void release() {
        if (nativeHandle != 0) {
            try {
                nativeReleasePiper(nativeHandle);
            } catch (Exception ignored) {
            }
            nativeHandle = 0;
        }
        if (fallbackEngine != null) {
            fallbackEngine.release();
        }
    }

    // Native JNI functions
    private native long nativeInitPiper(String modelPath);
    private native byte[] nativeSynthesize(long handle, String text);
    private native void nativeReleasePiper(long handle);
}
