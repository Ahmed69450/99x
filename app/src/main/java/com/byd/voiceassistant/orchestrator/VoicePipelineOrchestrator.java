package com.byd.voiceassistant.orchestrator;

import com.byd.voiceassistant.ai.llm.LlmEngine;
import com.byd.voiceassistant.ai.llm.PromptTemplateBuilder;
import com.byd.voiceassistant.ai.router.SemanticRouter;
import com.byd.voiceassistant.ai.router.SemanticRouterResult;
import com.byd.voiceassistant.ai.sanitizer.TtsSanitizer;
import com.byd.voiceassistant.context.location.ContextSnapshot;
import com.byd.voiceassistant.context.location.LocationTimeProvider;
import com.byd.voiceassistant.context.memory.MemoryRetrievalService;
import com.byd.voiceassistant.mcp.McpClientEngine;
import com.byd.voiceassistant.mcp.ToolResult;
import com.byd.voiceassistant.vehicle.CarController;
import com.byd.voiceassistant.vehicle.CarExecutionResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class VoicePipelineOrchestrator {

    private final SemanticRouter semanticRouter;
    private final CarController vehicleController;
    private final LlmEngine llmEngine;
    private final McpClientEngine mcpClient;
    private final LocationTimeProvider locationTimeProvider;
    private final MemoryRetrievalService memoryRetrievalService;

    private VoiceState currentState = VoiceState.IDLE;
    private String currentScreenContext = "";
    private final List<VoicePipelineListener> listeners = new CopyOnWriteArrayList<>();

    public VoicePipelineOrchestrator(
        SemanticRouter semanticRouter,
        CarController vehicleController,
        LlmEngine llmEngine,
        McpClientEngine mcpClient,
        LocationTimeProvider locationTimeProvider,
        MemoryRetrievalService memoryRetrievalService
    ) {
        this.semanticRouter = semanticRouter;
        this.vehicleController = vehicleController;
        this.llmEngine = llmEngine;
        this.mcpClient = mcpClient;
        this.locationTimeProvider = locationTimeProvider;
        this.memoryRetrievalService = memoryRetrievalService;
    }

    public void addListener(VoicePipelineListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(VoicePipelineListener listener) {
        listeners.remove(listener);
    }

    public synchronized VoiceState getCurrentState() {
        return currentState;
    }

    public synchronized void setScreenContext(String screenContext) {
        this.currentScreenContext = screenContext != null ? screenContext : "";
    }

    private synchronized void transitionTo(VoiceState newState) {
        this.currentState = newState;
        for (VoicePipelineListener l : listeners) {
            l.onStateChanged(newState);
        }
    }

    public synchronized String processUtterance(String utterance) {
        if (utterance == null || utterance.trim().isEmpty()) {
            transitionTo(VoiceState.IDLE);
            return "";
        }

        for (VoicePipelineListener l : listeners) {
            l.onTranscriptUpdated(utterance);
        }

        // 1. Evaluate Engine A (Fast Semantic Router)
        transitionTo(VoiceState.EVALUATING_ENGINE_A);
        SemanticRouterResult routeResult = semanticRouter.route(utterance);

        if (routeResult.isMatch()) {
            // Fast route match (confidence >= 0.70)
            transitionTo(VoiceState.EXECUTING_VEHICLE_CMD);
            CarExecutionResult carResult = vehicleController.execute(routeResult.getAction());
            String sanitizedResponse = TtsSanitizer.sanitize(carResult.getSpeechResponse());

            transitionTo(VoiceState.SPEAKING);
            for (VoicePipelineListener l : listeners) {
                l.onResponseGenerated(sanitizedResponse);
            }

            transitionTo(VoiceState.IDLE);
            return sanitizedResponse;
        }

        // 2. Engine B Fallback (LLM + Context + MCP)
        transitionTo(VoiceState.ENRICHING_CONTEXT_MCP);

        ContextSnapshot contextSnapshot = locationTimeProvider != null
            ? locationTimeProvider.getCurrentContext()
            : null;

        String locText = (contextSnapshot != null && contextSnapshot.getLocation() != null)
            ? contextSnapshot.getLocation().toString()
            : null;

        String timeText = contextSnapshot != null ? contextSnapshot.getFormattedTime() : null;

        List<String> facts = memoryRetrievalService != null
            ? memoryRetrievalService.retrieveRelevantFacts(utterance, 3)
            : null;

        String prompt = PromptTemplateBuilder.build(
            "You are an intelligent offline car voice assistant.",
            locText,
            timeText,
            currentScreenContext,
            facts,
            utterance
        );

        transitionTo(VoiceState.RUNNING_ENGINE_B_LLM);
        String rawResponse = llmEngine.generate(prompt);

        // Check for MCP tool calls in LLM output
        McpClientEngine.ParsedToolCall toolCall = mcpClient != null
            ? mcpClient.parseToolCall(rawResponse)
            : null;

        String finalContent;
        if (toolCall != null) {
            ToolResult toolResult = mcpClient.executeTool(
                toolCall.getToolName(),
                toolCall.getArgumentsJson()
            );

            // Re-prompt or return synthesized tool response
            String secondPrompt = PromptTemplateBuilder.build(
                "You are an intelligent offline car voice assistant.",
                locText,
                timeText,
                currentScreenContext,
                facts,
                "Tool result from " + toolCall.getToolName() + ": " + toolResult.getContent() +
                ". Formulate a concise voice response for the user's question: " + utterance
            );
            finalContent = llmEngine.generate(secondPrompt);
        } else {
            finalContent = rawResponse;
        }

        String sanitizedResponse = TtsSanitizer.sanitize(finalContent);

        transitionTo(VoiceState.SPEAKING);
        for (VoicePipelineListener l : listeners) {
            l.onResponseGenerated(sanitizedResponse);
        }

        transitionTo(VoiceState.IDLE);
        return sanitizedResponse;
    }
}
