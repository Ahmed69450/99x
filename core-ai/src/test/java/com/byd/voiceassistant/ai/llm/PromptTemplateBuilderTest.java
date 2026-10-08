package com.byd.voiceassistant.ai.llm;

import org.junit.Assert;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

public class PromptTemplateBuilderTest {

    @Test
    public void testBuildPromptInjectsContextAndFacts() {
        List<String> facts = Arrays.asList("User prefers cabin temp at 23C", "User likes jazz music");

        String prompt = PromptTemplateBuilder.build(
            "You are a car voice assistant.",
            "Latitude: 22.54, Longitude: 114.05, Shenzhen",
            "2026-10-08 14:30:00, Thursday",
            "App: Maps, Showing: Navigation to Airport",
            facts,
            "What is the next turn and what music should I play?"
        );

        Assert.assertTrue(prompt.contains("Latitude: 22.54"));
        Assert.assertTrue(prompt.contains("2026-10-08 14:30:00"));
        Assert.assertTrue(prompt.contains("User prefers cabin temp at 23C"));
        Assert.assertTrue(prompt.contains("App: Maps"));
        Assert.assertTrue(prompt.contains("What is the next turn"));
    }
}
