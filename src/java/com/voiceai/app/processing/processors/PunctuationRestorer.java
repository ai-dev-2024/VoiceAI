package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.regex.Pattern;

/**
 * PunctuationRestorer - Adds punctuation to unpunctuated ASR output
 * 
 * This is a rule-based implementation. For production quality,
 * consider integrating a small transformer model (like punctuation-bert).
 * 
 * Handles:
 * - Sentence-ending periods
 * - Question marks for question patterns
 * - Commas after introductory words
 * - Cleanup of double punctuation
 */
public class PunctuationRestorer implements TextProcessor {

    // Question starters that indicate a question
    private static final String[] QUESTION_STARTERS = {
            "what", "where", "when", "why", "how", "who", "which", "whose",
            "is it", "are you", "do you", "did you", "can you", "could you",
            "would you", "will you", "have you", "has it", "was it", "were you",
            "does", "don't you", "isn't", "aren't", "won't", "wouldn't",
            "shouldn't", "couldn't", "haven't", "hasn't"
    };

    // Introductory words that take a comma
    private static final String[] INTRO_WORDS = {
            "however", "therefore", "moreover", "furthermore", "meanwhile",
            "nevertheless", "consequently", "finally", "well", "actually",
            "basically", "honestly", "personally", "certainly", "unfortunately",
            "fortunately", "obviously", "clearly", "anyway", "besides",
            "additionally", "alternatively", "specifically", "generally"
    };

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Add question marks for question patterns
        result = addQuestionMarks(result);

        // Add commas after introductory words
        result = addIntroCommas(result);

        // Add period at end if no punctuation
        result = addFinalPunctuation(result);

        // Clean up punctuation errors
        result = cleanupPunctuation(result);

        return result;
    }

    private String addQuestionMarks(String text) {
        String result = text;

        for (String starter : QUESTION_STARTERS) {
            // Question pattern at end of text: "what do you think" → "what do you think?"
            Pattern p = Pattern.compile(
                    "(?i)\\b(" + Pattern.quote(starter) + "[^.!?]{3,}?)\\s*$");
            result = p.matcher(result).replaceAll("$1?");
        }

        return result;
    }

    private String addIntroCommas(String text) {
        String result = text;

        for (String word : INTRO_WORDS) {
            // At start of text
            result = result.replaceAll(
                    "(?i)^" + word + "\\s+(?!,)",
                    word + ", ");

            // After sentence boundary
            result = result.replaceAll(
                    "(?i)([.!?])\\s+" + word + "\\s+(?!,)",
                    "$1 " + word + ", ");
        }

        return result;
    }

    private String addFinalPunctuation(String text) {
        String trimmed = text.trim();

        // Already has ending punctuation
        if (trimmed.matches(".*[.!?]\\s*$")) {
            return text;
        }

        // Is it a question? (checked by addQuestionMarks)
        // If not, add period
        return trimmed + ".";
    }

    private String cleanupPunctuation(String text) {
        String result = text;

        // Multiple periods → single
        result = result.replaceAll("\\.{2,}", ".");

        // Multiple question marks → single
        result = result.replaceAll("\\?{2,}", "?");

        // Multiple exclamation → single
        result = result.replaceAll("!{2,}", "!");

        // Multiple commas → single
        result = result.replaceAll(",{2,}", ",");

        // Space before punctuation
        result = result.replaceAll("\\s+([.!?,])", "$1");

        // No space after punctuation (except end)
        result = result.replaceAll("([.!?])(?=[a-zA-Z])", "$1 ");

        // Comma-period or period-comma
        result = result.replaceAll(",\\.", ".");
        result = result.replaceAll("\\.,", ".");

        return result;
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        return !context.isPunctuationRestoration();
    }
}
