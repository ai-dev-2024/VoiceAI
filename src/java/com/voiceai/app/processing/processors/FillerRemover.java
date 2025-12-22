package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.regex.Pattern;

/**
 * FillerRemover - Removes verbal fillers and disfluencies
 * 
 * Handles:
 * - "uh", "um", "er", "hmm" - hesitation sounds
 * - "you know", "I mean", "like" - discourse markers (when filler)
 * - "sort of", "kind of" - hedging (sometimes)
 */
public class FillerRemover implements TextProcessor {

    // Pure fillers - always remove
    private static final String[] PURE_FILLERS = {
            "\\buh\\b",
            "\\bum\\b",
            "\\bumm\\b",
            "\\berm\\b",
            "\\ber\\b",
            "\\bhmm\\b",
            "\\bhm\\b",
            "\\bahh?\\b",
            "\\behh?\\b",
    };

    // Discourse fillers - remove when surrounded by other content
    private static final String[] DISCOURSE_FILLERS = {
            "\\byou know\\b",
            "\\bkinda\\b",
            "\\bsort of\\b",
            "\\bkind of\\b",
            "\\bbasically\\b", // Sometimes filler, sometimes meaningful
    };

    // "like" is special - only filler in certain contexts
    // "I was like" = quotative (keep)
    // "like, you know" = filler (remove)
    private static final Pattern FILLER_LIKE = Pattern.compile("(?i),\\s*like\\s*,");
    private static final Pattern FILLER_LIKE_2 = Pattern.compile("(?i)\\blike\\s*,\\s*(uh|um|so|you know)");
    private static final Pattern FILLER_LIKE_3 = Pattern.compile("(?i)^like\\s+");

    // Sentence-initial fillers
    private static final Pattern INITIAL_SO = Pattern.compile("(?i)^so,?\\s+");

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Remove pure fillers
        for (String filler : PURE_FILLERS) {
            result = result.replaceAll("(?i)" + filler + "\\s*,?\\s*", " ");
        }

        // Remove discourse fillers
        for (String filler : DISCOURSE_FILLERS) {
            result = result.replaceAll("(?i)" + filler + "\\s*,?\\s*", " ");
        }

        // Handle "like" carefully
        result = FILLER_LIKE.matcher(result).replaceAll(",");
        result = FILLER_LIKE_2.matcher(result).replaceAll("");
        result = FILLER_LIKE_3.matcher(result).replaceAll("");

        // Remove sentence-initial "so" when it's just a filler
        // (This is aggressive - may want to make configurable)
        // result = INITIAL_SO.matcher(result).replaceAll("");

        // Normalize spaces
        result = result.replaceAll("\\s{2,}", " ");

        return result.trim();
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        return !context.isFillerRemoval();
    }
}
