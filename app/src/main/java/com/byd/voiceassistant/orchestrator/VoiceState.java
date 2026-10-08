package com.byd.voiceassistant.orchestrator;

public enum VoiceState {
    IDLE,
    WAKE_LISTENING,
    WAKE_DETECTED,
    QUERY_CAPTURING,
    EVALUATING_ENGINE_A,
    EXECUTING_VEHICLE_CMD,
    ENRICHING_CONTEXT_MCP,
    RUNNING_ENGINE_B_LLM,
    TTS_SYNTHESIS,
    SPEAKING,
    ERROR
}
