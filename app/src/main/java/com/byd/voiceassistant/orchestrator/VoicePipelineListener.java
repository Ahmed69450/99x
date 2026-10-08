package com.byd.voiceassistant.orchestrator;

public interface VoicePipelineListener {
    void onStateChanged(VoiceState newState);
    void onTranscriptUpdated(String transcript);
    void onResponseGenerated(String response);
    void onError(String message);
}
