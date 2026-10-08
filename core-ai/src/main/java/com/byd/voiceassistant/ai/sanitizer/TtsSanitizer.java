package com.byd.voiceassistant.ai.sanitizer;

import java.util.regex.Pattern;

public class TtsSanitizer {

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern HEADER_PATTERN = Pattern.compile("(?m)^#{1,6}\\s*");
    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("\\*{1,3}([^*]+)\\*{1,3}");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\([^)]+\\)");
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$(\\d+(\\.\\d+)?)");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s{2,}");

    public static String sanitize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        String result = text;

        // 1. Replace code blocks
        result = CODE_BLOCK_PATTERN.matcher(result).replaceAll(" (code snippet omitted) ");

        // 2. Replace inline code
        result = INLINE_CODE_PATTERN.matcher(result).replaceAll("$1");

        // 3. Remove markdown headers
        result = HEADER_PATTERN.matcher(result).replaceAll("");

        // 4. Remove bold & italic markdown
        result = BOLD_ITALIC_PATTERN.matcher(result).replaceAll("$1");

        // 5. Replace markdown links [Text](URL) -> Text
        result = LINK_PATTERN.matcher(result).replaceAll("$1");

        // 6. Expand dollar values: $5 -> 5 dollars
        result = DOLLAR_PATTERN.matcher(result).replaceAll("$1 dollars");

        // 7. Phonetic unit and symbol replacements
        result = result.replace("°C", " degrees Celsius")
                       .replace("°F", " degrees Fahrenheit")
                       .replace("%", " percent")
                       .replace("km/h", " kilometers per hour")
                       .replace("mph", " miles per hour")
                       .replace(" & ", " and ")
                       .replace("&", " and ");

        // 8. Strip emojis and presentation selectors via code points
        result = stripEmojis(result);

        // 9. Normalize whitespace and trim
        result = MULTIPLE_SPACES.matcher(result).replaceAll(" ").trim();

        // 10. Clean spaces before punctuation
        result = result.replace(" .", ".")
                       .replace(" ,", ",")
                       .replace(" !", "!")
                       .replace(" ?", "?");

        return result;
    }

    public static String stripEmojis(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        int length = text.length();
        for (int offset = 0; offset < length; ) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);

            boolean isEmoji = (codePoint >= 0x1F000 && codePoint <= 0x1FFFF) ||
                              (codePoint >= 0x2600 && codePoint <= 0x27BF) ||
                              (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||
                              (codePoint >= 0x2300 && codePoint <= 0x23FF) ||
                              (codePoint >= 0x2B50 && codePoint <= 0x2B55);

            if (!isEmoji) {
                sb.appendCodePoint(codePoint);
            }
            offset += charCount;
        }
        return sb.toString();
    }
}
