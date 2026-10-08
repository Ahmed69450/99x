package com.byd.voiceassistant.ai.sanitizer;

import org.junit.Assert;
import org.junit.Test;

public class TtsSanitizerTest {

    @Test
    public void testStripMarkdownAsterisksAndHeaders() {
        String input = "### Hello! **The temperature** is *optimal*.";
        String expected = "Hello! The temperature is optimal.";
        Assert.assertEquals(expected, TtsSanitizer.sanitize(input));
    }

    @Test
    public void testStripCodeBlocks() {
        String input = "Here is the code:\n```kotlin\nval x = 1\n```\nDone.";
        String expected = "Here is the code: (code snippet omitted) Done.";
        Assert.assertEquals(expected, TtsSanitizer.sanitize(input));
    }

    @Test
    public void testStripEmojis() {
        String input = "Sure! Setting AC to cool \u2744\uFE0F\uD83D\uDE97 Have a great drive! \uD83D\uDE0A";
        String expected = "Sure! Setting AC to cool Have a great drive!";
        Assert.assertEquals(expected, TtsSanitizer.sanitize(input));
    }

    @Test
    public void testExpandPhoneticSymbols() {
        String input = "Target temp is 22°C with 80% humidity at 60km/h.";
        String expected = "Target temp is 22 degrees Celsius with 80 percent humidity at 60 kilometers per hour.";
        Assert.assertEquals(expected, TtsSanitizer.sanitize(input));
    }

    @Test
    public void testExpandAmpersandAndDollar() {
        String input = "Turn on AC & music for $5.";
        String expected = "Turn on AC and music for 5 dollars.";
        Assert.assertEquals(expected, TtsSanitizer.sanitize(input));
    }
}
