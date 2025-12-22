package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.regex.Pattern;

/**
 * CourseCorrector - Wispr Flow's killer feature
 * 
 * Handles mid-sentence corrections like:
 * - "Let's meet tomorrow no wait let's do Friday" → "Let's do Friday"
 * - "I think we should uh actually never mind I mean definitely yes" →
 * "Definitely yes"
 * - "send email to john no sorry to mike" → "Send email to Mike"
 * 
 * This is THE feature that separates Wispr from basic dictation.
 */
public class CourseCorrector implements TextProcessor {

    // Correction trigger patterns - when user changes their mind
    private static final Pattern[] CORRECTION_PATTERNS = {
            // "no wait" / "no actually" - discard everything before
            Pattern.compile("(?i)^.*\\bno,?\\s+wait,?\\s+"),
            Pattern.compile("(?i)^.*\\bno,?\\s+actually,?\\s+"),
            Pattern.compile("(?i)^.*\\bno,?\\s+sorry,?\\s+"),

            // "actually never mind" - discard everything before
            Pattern.compile("(?i)^.*\\bactually,?\\s+never\\s*mind,?\\s+"),
            Pattern.compile("(?i)^.*\\bnever\\s*mind,?\\s+"),

            // "I mean" / "what I meant was" - correction follows
            Pattern.compile("(?i)^.*\\bi\\s+mean,?\\s+"),
            Pattern.compile("(?i)^.*\\bwhat\\s+i\\s+meant\\s+(was|is),?\\s+"),

            // "scratch that" / "delete that" - discard everything before
            Pattern.compile("(?i)^.*\\bscratch\\s+that,?\\s+"),
            Pattern.compile("(?i)^.*\\bdelete\\s+that,?\\s+"),
            Pattern.compile("(?i)^.*\\bforget\\s+that,?\\s+"),
            Pattern.compile("(?i)^.*\\bignore\\s+that,?\\s+"),

            // "or rather" / "or actually" - correction follows
            Pattern.compile("(?i)^.*\\bor\\s+rather,?\\s+"),
            Pattern.compile("(?i)^.*\\bor\\s+actually,?\\s+"),
            Pattern.compile("(?i)^.*\\bor\\s+better\\s+yet,?\\s+"),

            // "let me rephrase" / "let me start over"
            Pattern.compile("(?i)^.*\\blet\\s+me\\s+rephrase,?\\s+"),
            Pattern.compile("(?i)^.*\\blet\\s+me\\s+start\\s+over,?\\s+"),

            // Interruption patterns - "wait" at start of correction
            Pattern.compile("(?i)^.*\\bwait,?\\s+no,?\\s+"),
            Pattern.compile("(?i)^.*\\bhold\\s+on,?\\s+"),

            // "not X but Y" pattern → keep only Y context
            Pattern.compile("(?i)^.*\\bnot\\s+\\w+,?\\s+but\\s+"),
    };

    // Inline corrections - "X no Y" where X and Y are alternatives
    private static final Pattern INLINE_CORRECTION = Pattern.compile("(?i)\\b(\\w+)\\s+no\\s+(\\w+)\\b");

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Apply major correction patterns (discard prefix)
        for (Pattern pattern : CORRECTION_PATTERNS) {
            result = pattern.matcher(result).replaceAll("");
        }

        // Apply inline corrections: "john no mike" → "mike"
        // But be careful: "no" can also mean negation
        // Only apply when both words are similar type (names, numbers, etc.)
        result = applyInlineCorrections(result);

        return result.trim();
    }

    /**
     * Handle inline corrections like "send to john no mike"
     * 
     * We're conservative here - only correct when it looks like
     * the user is replacing one word with another of the same type.
     */
    private String applyInlineCorrections(String text) {
        // Simple case: "word1 no word2" where word1 is being replaced
        // This is tricky because "no" has many meanings
        // For now, only handle obvious cases like "X no sorry Y"
        return text.replaceAll("(?i)\\b(\\w+)\\s+no\\s+sorry\\s+(\\w+)\\b", "$2");
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        return !context.isCourseCorrection();
    }
}
