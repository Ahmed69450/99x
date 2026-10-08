package com.byd.voiceassistant.mcp.tools;

import com.byd.voiceassistant.mcp.McpTool;
import com.byd.voiceassistant.mcp.ToolResult;

public class WebSearchTool implements McpTool {

    private final boolean networkAvailable;

    public WebSearchTool() {
        this(true);
    }

    public WebSearchTool(boolean networkAvailable) {
        this.networkAvailable = networkAvailable;
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "Fetches real-time internet information and search results for current events.";
    }

    @Override
    public String getSchemaJson() {
        return "{\"type\": \"object\", \"properties\": {\"query\": {\"type\": \"string\"}}, \"required\": [\"query\"]}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        if (!networkAvailable) {
            return ToolResult.error("Vehicle network connection is currently offline. Web search unavailable.");
        }

        // Simulated online search payload for edge integration
        return ToolResult.success("Search results for query: Latest headlines and weather conditions retrieved successfully.");
    }
}
