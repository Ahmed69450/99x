package com.byd.voiceassistant.mcp;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class McpClientEngine {

    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
        "\\[TOOL_CALL:\\s*([a-zA-Z0-9_-]+)\\s*(\\{.*?\\})\\]"
    );

    public static class ParsedToolCall {
        private final String toolName;
        private final String argumentsJson;

        public ParsedToolCall(String toolName, String argumentsJson) {
            this.toolName = toolName;
            this.argumentsJson = argumentsJson;
        }

        public String getToolName() {
            return toolName;
        }

        public String getArgumentsJson() {
            return argumentsJson;
        }
    }

    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    public void registerTool(McpTool tool) {
        if (tool != null) {
            tools.put(tool.getName(), tool);
        }
    }

    public ToolResult executeTool(String toolName, String paramsJson) {
        if (toolName == null || !tools.containsKey(toolName)) {
            return ToolResult.error("Tool '" + toolName + "' is not registered.");
        }
        try {
            return tools.get(toolName).execute(paramsJson);
        } catch (Exception e) {
            return ToolResult.error("Error executing tool '" + toolName + "': " + e.getMessage());
        }
    }

    public ParsedToolCall parseToolCall(String text) {
        if (text == null) return null;
        Matcher matcher = TOOL_CALL_PATTERN.matcher(text);
        if (matcher.find()) {
            String toolName = matcher.group(1);
            String args = matcher.group(2);
            return new ParsedToolCall(toolName, args);
        }
        return null;
    }

    public List<McpTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
}
