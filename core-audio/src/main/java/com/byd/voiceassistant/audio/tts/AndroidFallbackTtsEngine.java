package com.byd.voiceassistant.audio.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class AndroidFallbackTtsEngine implements TtsEngine {

    private final Context context;
    private TextToSpeech tts;
    private boolean isInitialized = false;

    public AndroidFallbackTtsEngine(Context context) {
        this.context = context;
        if (context != null) {
            try {
                this.tts = new TextToSpeech(context, status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        tts.setLanguage(Locale.US);
                        isInitialized = true;
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public synchronized String speak(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        if (tts != null && isInitialized) {
            try {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "voice_assistant_fallback");
            } catch (Exception ignored) {
            }
        }

        return "[Fallback TTS]: " + text;
    }

    @Override
    public synchronized void stop() {
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public synchronized void release() {
        if (tts != null) {
            try {
                tts.shutdown();
            } catch (Exception ignored) {
            }
            tts = null;
        }
    }
}
