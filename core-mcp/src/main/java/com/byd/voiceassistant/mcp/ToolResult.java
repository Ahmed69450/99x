package com.byd.voiceassistant.mcp;

public class ToolResult {
    private final boolean success;
    private final String content;

    public ToolResult(boolean success, String content) {
        this.success = success;
        this.content = content != null ? content : "";
    }

    public static ToolResult success(String content) {
        return new ToolResult(true, content);
    }

    public static ToolResult error(String errorMessage) {
        return new ToolResult(false, errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "ToolResult(success=" + success + ", content='" + content + "')";
    }
}
