package com.byd.voiceassistant.ai.llm;

public interface LlmEngine {
    boolean loadModel(String modelPath);
    String generate(String prompt);
    void setThreadCount(int threads);
    int getThreadCount();
    void release();
}
