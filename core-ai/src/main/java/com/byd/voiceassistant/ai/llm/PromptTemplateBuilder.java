package com.byd.voiceassistant.ai.llm;

import java.util.List;

public class PromptTemplateBuilder {

    public static String build(
        String systemPrompt,
        String locationContext,
        String timeContext,
        String screenContext,
        List<String> memoryFacts,
        String userQuery
    ) {
        StringBuilder sb = new StringBuilder();

        // System Prompt
        sb.append("<|im_start|>system\n");
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            sb.append(systemPrompt.trim()).append("\n\n");
        } else {
            sb.append("You are an intelligent car voice assistant for BYD DiLink.\n");
            sb.append("Keep answers brief, helpful, and natural for speech playback. Do not use markdown, emojis, or bullet points.\n\n");
        }

        sb.append("Tool Calling Instructions:\n");
        sb.append("If real-time web info or screen details are required, reply ONLY with: [TOOL_CALL: tool_name {\"param\": \"value\"}]\n\n");

        // Context Blocks
        if (locationContext != null && !locationContext.trim().isEmpty()) {
            sb.append("[Location Context]: ").append(locationContext.trim()).append("\n");
        }

        if (timeContext != null && !timeContext.trim().isEmpty()) {
            sb.append("[Time Context]: ").append(timeContext.trim()).append("\n");
        }

        if (screenContext != null && !screenContext.trim().isEmpty()) {
            sb.append("[Current Screen]: ").append(screenContext.trim()).append("\n");
        }

        if (memoryFacts != null && !memoryFacts.isEmpty()) {
            sb.append("[Remembered User Facts]:\n");
            for (String fact : memoryFacts) {
                if (fact != null && !fact.trim().isEmpty()) {
                    sb.append("- ").append(fact.trim()).append("\n");
                }
            }
        }

        sb.append("<|im_end|>\n");

        // User Utterance
        sb.append("<|im_start|>user\n");
        sb.append(userQuery != null ? userQuery.trim() : "").append("\n");
        sb.append("<|im_end|>\n");

        // Assistant trigger
        sb.append("<|im_start|>assistant\n");

        return sb.toString();
    }
}
