package com.byd.voiceassistant.audio.stt;

public interface SpeechRecognitionListener {
    void onPartialResult(String hypothesis);
    void onFinalResult(String transcript);
    void onError(String error);
}
