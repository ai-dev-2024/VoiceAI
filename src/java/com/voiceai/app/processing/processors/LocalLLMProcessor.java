package com.voiceai.app.processing.processors;

import android.content.Context;
import android.util.Log;
import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.io.File;
import java.util.regex.Pattern;

/**
 * LocalLLMProcessor - Enhanced rule-based text formatting
 * 
 * Provides Wispr Flow-style text polishing using sophisticated rule-based
 * processing.
 * This runs entirely on-device without requiring internet connectivity.
 * 
 * Features:
 * - Filler word removal (um, uh, like, you know)
 * - Stutter/repetition cleanup
 * - Grammar corrections (contractions, "i" → "I")
 * - Smart punctuation
 * - Question detection
 * 
 * Note: Native LLM inference (llama.cpp) not available due to Android
 * cross-compilation issues. This enhanced rule-based processing provides
 * similar results for common dictation use cases.
 */
public class LocalLLMProcessor implements TextProcessor {

    private static final String TAG = "VoiceAI.LocalLLM";
    private static final String MODEL_FILENAME = "Qwen3-0.6B-UD-Q4_K_XL.gguf";

    // Model state
    private boolean enabled = false;
    private Context appContext;
    private File modelFile;

    // Rule-based patterns for Wispr Flow-style processing
    private static final Pattern FILLER_PATTERN = Pattern.compile(
            "(?i)\\b(um+|uh+|er+|ah+|hmm+)\\b\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern HEDGE_PATTERN = Pattern.compile(
            "(?i)\\b(you know|i mean|like|basically|actually|literally|sort of|kind of)\\b\\s*,?\\s*",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEAT_PATTERN = Pattern.compile(
            "\\b(\\w+)\\s+\\1\\b", Pattern.CASE_INSENSITIVE);

    public LocalLLMProcessor() {
        // Default constructor
    }

    public LocalLLMProcessor(Context context) {
        this.appContext = context;
        if (context != null) {
            this.modelFile = new File(context.getFilesDir(), MODEL_FILENAME);
        }
    }

    /**
     * Initialize/enable the processor
     */
    public boolean initModel(Context context) {
        if (context == null) {
            return false;
        }

        this.appContext = context;
        this.modelFile = new File(context.getFilesDir(), MODEL_FILENAME);
        this.enabled = true;

        Log.d(TAG, "LocalLLMProcessor enabled (using enhanced rule-based processing)");
        return true;
    }

    /**
     * Check if the processor is enabled
     */
    public boolean isModelLoaded() {
        return enabled;
    }

    /**
     * Check if the model file exists (for UI status display)
     */
    public boolean isModelDownloaded() {
        return modelFile != null && modelFile.exists();
    }

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Apply Wispr Flow-style processing
        return enhancedRuleBasedProcessing(text);
    }

    /**
     * Enhanced rule-based processing - Wispr Flow style
     * Provides sophisticated text cleanup without requiring ML model
     */
    private String enhancedRuleBasedProcessing(String text) {
        String result = text;

        // 1. Remove filler words (um, uh, er, ah, hmm)
        result = FILLER_PATTERN.matcher(result).replaceAll("");

        // 2. Remove hedge words (you know, like, basically, actually)
        result = HEDGE_PATTERN.matcher(result).replaceAll("");

        // 3. Remove immediate word repetitions (stutters)
        result = REPEAT_PATTERN.matcher(result).replaceAll("$1");

        // 4. Clean up excessive punctuation
        result = result.replaceAll("\\.{2,}", ".");
        result = result.replaceAll(",{2,}", ",");
        result = result.replaceAll("!{2,}", "!");
        result = result.replaceAll("\\?{2,}", "?");

        // 5. Fix common grammar issues
        result = fixCommonGrammarIssues(result);

        // 6. Clean up whitespace
        result = result.replaceAll("\\s+", " ").trim();

        // 7. Ensure sentence ending punctuation
        if (!result.isEmpty() && !result.matches(".*[.!?]$")) {
            if (isLikelyQuestion(result)) {
                result += "?";
            } else {
                result += ".";
            }
        }

        return result;
    }

    /**
     * Fix common grammar issues in dictation
     */
    private String fixCommonGrammarIssues(String text) {
        String result = text;

        // Fix "i" to "I"
        result = result.replaceAll("\\bi\\b", "I");

        // Fix common contractions
        result = result.replaceAll("(?i)\\bim\\b", "I'm");
        result = result.replaceAll("(?i)\\bdont\\b", "don't");
        result = result.replaceAll("(?i)\\bcant\\b", "can't");
        result = result.replaceAll("(?i)\\bwont\\b", "won't");
        result = result.replaceAll("(?i)\\bdidnt\\b", "didn't");
        result = result.replaceAll("(?i)\\bcouldnt\\b", "couldn't");
        result = result.replaceAll("(?i)\\bwouldnt\\b", "wouldn't");
        result = result.replaceAll("(?i)\\bisnt\\b", "isn't");
        result = result.replaceAll("(?i)\\barent\\b", "aren't");
        result = result.replaceAll("(?i)\\bwasnt\\b", "wasn't");
        result = result.replaceAll("(?i)\\bwerent\\b", "weren't");
        result = result.replaceAll("(?i)\\bhasnt\\b", "hasn't");
        result = result.replaceAll("(?i)\\bhavent\\b", "haven't");
        result = result.replaceAll("(?i)\\bhadnt\\b", "hadn't");
        result = result.replaceAll("(?i)\\bthats\\b", "that's");
        result = result.replaceAll("(?i)\\bwhats\\b", "what's");
        result = result.replaceAll("(?i)\\bheres\\b", "here's");
        result = result.replaceAll("(?i)\\btheres\\b", "there's");
        result = result.replaceAll("(?i)\\blets\\b", "let's");
        result = result.replaceAll("(?i)\\bweve\\b", "we've");
        result = result.replaceAll("(?i)\\btheyve\\b", "they've");
        result = result.replaceAll("(?i)\\byoure\\b", "you're");
        result = result.replaceAll("(?i)\\bwere\\b(?!\\s+(not|able|going|to))", "we're");

        return result;
    }

    /**
     * Check if text is likely a question
     */
    private boolean isLikelyQuestion(String text) {
        String lower = text.toLowerCase().trim();
        return lower.startsWith("what ") ||
                lower.startsWith("where ") ||
                lower.startsWith("when ") ||
                lower.startsWith("why ") ||
                lower.startsWith("who ") ||
                lower.startsWith("how ") ||
                lower.startsWith("is ") ||
                lower.startsWith("are ") ||
                lower.startsWith("was ") ||
                lower.startsWith("were ") ||
                lower.startsWith("can ") ||
                lower.startsWith("could ") ||
                lower.startsWith("would ") ||
                lower.startsWith("should ") ||
                lower.startsWith("do ") ||
                lower.startsWith("does ") ||
                lower.startsWith("did ") ||
                lower.startsWith("will ") ||
                lower.startsWith("have ") ||
                lower.startsWith("has ");
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        // Always process - this is the enhanced formatting step
        return false;
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        enabled = false;
    }
}
