package com.byd.voiceassistant.mcp;

import com.byd.voiceassistant.mcp.tools.ScreenInspectTool;
import com.byd.voiceassistant.mcp.tools.WebSearchTool;
import org.junit.Assert;
import org.junit.Test;

public class McpClientEngineTest {

    @Test
    public void testScreenInspectToolExecution() {
        McpClientEngine engine = new McpClientEngine();
        ScreenInspectTool screenTool = new ScreenInspectTool(new ScreenInspectTool.ScreenContentProvider() {
            @Override
            public String getScreenContent() {
                return "Active App: Spotify, Playing: Miles Davis";
            }
        });
        engine.registerTool(screenTool);

        ToolResult result = engine.executeTool("screen_inspect", "{}");
        Assert.assertTrue(result.isSuccess());
        Assert.assertTrue(result.getContent().contains("Spotify"));
    }

    @Test
    public void testWebSearchToolOfflineFallback() {
        McpClientEngine engine = new McpClientEngine();
        WebSearchTool webTool = new WebSearchTool(false);
        engine.registerTool(webTool);

        ToolResult result = engine.executeTool("web_search", "{\"query\": \"world cup score\"}");
        Assert.assertFalse(result.isSuccess());
        Assert.assertTrue(result.getContent().toLowerCase().contains("offline") ||
                          result.getContent().toLowerCase().contains("unavailable"));
    }

    @Test
    public void testParseToolCallInvocation() {
        McpClientEngine engine = new McpClientEngine();
        String text = "Let me check that. [TOOL_CALL: web_search {\"query\": \"BYD Seal specs\"}]";
        McpClientEngine.ParsedToolCall parsed = engine.parseToolCall(text);

        Assert.assertNotNull(parsed);
        Assert.assertEquals("web_search", parsed.getToolName());
        Assert.assertTrue(parsed.getArgumentsJson().contains("BYD Seal specs"));
    }
}
