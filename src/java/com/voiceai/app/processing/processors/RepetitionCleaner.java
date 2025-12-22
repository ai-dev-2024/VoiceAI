package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.regex.Pattern;

/**
 * RepetitionCleaner - Removes stutters and accidental word repetitions
 * 
 * Handles:
 * - "I I I think" → "I think"
 * - "the the problem" → "the problem"
 * - "you know you know" → "you know" (phrase repetition)
 */
public class RepetitionCleaner implements TextProcessor {

    // Triple+ word repeats: "I I I think" → "I think"
    private static final Pattern TRIPLE_REPEAT = Pattern.compile("(?i)\\b(\\w+)(\\s+\\1){2,}\\b");

    // Double word repeats: "the the" → "the"
    private static final Pattern DOUBLE_REPEAT = Pattern.compile("(?i)\\b(\\w+)\\s+\\1\\b");

    // Two-word phrase repeats: "you know you know" → "you know"
    private static final Pattern PHRASE_REPEAT_2 = Pattern.compile("(?i)\\b(\\w+\\s+\\w+)(\\s+\\1)+\\b");

    // Three-word phrase repeats
    private static final Pattern PHRASE_REPEAT_3 = Pattern.compile("(?i)\\b(\\w+\\s+\\w+\\s+\\w+)(\\s+\\1)+\\b");

    // Multiple spaces cleanup
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Order matters: longest patterns first
        result = PHRASE_REPEAT_3.matcher(result).replaceAll("$1");
        result = PHRASE_REPEAT_2.matcher(result).replaceAll("$1");
        result = TRIPLE_REPEAT.matcher(result).replaceAll("$1");
        result = DOUBLE_REPEAT.matcher(result).replaceAll("$1");

        // Normalize spaces
        result = MULTI_SPACE.matcher(result).replaceAll(" ");

        return result.trim();
    }
}
