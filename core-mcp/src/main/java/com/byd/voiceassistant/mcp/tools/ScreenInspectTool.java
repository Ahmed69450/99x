package com.byd.voiceassistant.mcp.tools;

import com.byd.voiceassistant.mcp.McpTool;
import com.byd.voiceassistant.mcp.ToolResult;

public class ScreenInspectTool implements McpTool {

    public interface ScreenContentProvider {
        String getScreenContent();
    }

    private final ScreenContentProvider provider;

    public ScreenInspectTool(ScreenContentProvider provider) {
        this.provider = provider;
    }

    @Override
    public String getName() {
        return "screen_inspect";
    }

    @Override
    public String getDescription() {
        return "Inspects the current foreground screen and active UI nodes on the car head unit.";
    }

    @Override
    public String getSchemaJson() {
        return "{\"type\": \"object\", \"properties\": {}}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        if (provider == null) {
            return ToolResult.error("Screen inspector service is not connected.");
        }
        String content = provider.getScreenContent();
        if (content == null || content.trim().isEmpty()) {
            return ToolResult.success("Screen is currently showing the system home dashboard.");
        }
        return ToolResult.success(content);
    }
}
