package com.byd.voiceassistant.mcp;

public interface McpTool {
    String getName();
    String getDescription();
    String getSchemaJson();
    ToolResult execute(String paramsJson);
}
