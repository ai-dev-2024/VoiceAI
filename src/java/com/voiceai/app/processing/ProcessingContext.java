package com.voiceai.app.processing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ProcessingContext - Carries settings and metadata through the processing
 * pipeline
 * 
 * This is the "bag of state" that processors can read from.
 * It's immutable after construction to ensure thread-safety.
 */
public class ProcessingContext {

    // Personal dictionary: key (lowercase) → value (exact case)
    private final Map<String, String> personalDictionary;

    // Word timestamps for paragraph segmentation (optional)
    private final List<WordTimestamp> timestamps;

    // Feature flags
    private final boolean courseCorrection;
    private final boolean fillerRemoval;
    private final boolean numberNormalization;
    private final boolean punctuationRestoration;
    private final boolean casingEnabled;
    private final boolean debugMode;

    private ProcessingContext(Builder builder) {
        this.personalDictionary = new HashMap<>(builder.personalDictionary);
        this.timestamps = builder.timestamps;
        this.courseCorrection = builder.courseCorrection;
        this.fillerRemoval = builder.fillerRemoval;
        this.numberNormalization = builder.numberNormalization;
        this.punctuationRestoration = builder.punctuationRestoration;
        this.casingEnabled = builder.casingEnabled;
        this.debugMode = builder.debugMode;
    }

    // Getters
    public Map<String, String> getPersonalDictionary() {
        return new HashMap<>(personalDictionary);
    }

    public List<WordTimestamp> getTimestamps() {
        return timestamps;
    }

    public boolean isCourseCorrection() {
        return courseCorrection;
    }

    public boolean isFillerRemoval() {
        return fillerRemoval;
    }

    public boolean isNumberNormalization() {
        return numberNormalization;
    }

    public boolean isPunctuationRestoration() {
        return punctuationRestoration;
    }

    public boolean isCasingEnabled() {
        return casingEnabled;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    // Builder pattern for clean construction
    public static class Builder {
        private Map<String, String> personalDictionary = new HashMap<>();
        private List<WordTimestamp> timestamps = null;
        private boolean courseCorrection = true;
        private boolean fillerRemoval = true;
        private boolean numberNormalization = true;
        private boolean punctuationRestoration = true;
        private boolean casingEnabled = true;
        private boolean debugMode = false;

        public Builder personalDictionary(Map<String, String> dict) {
            if (dict != null)
                this.personalDictionary = dict;
            return this;
        }

        public Builder timestamps(List<WordTimestamp> ts) {
            this.timestamps = ts;
            return this;
        }

        public Builder courseCorrection(boolean enabled) {
            this.courseCorrection = enabled;
            return this;
        }

        public Builder fillerRemoval(boolean enabled) {
            this.fillerRemoval = enabled;
            return this;
        }

        public Builder numberNormalization(boolean enabled) {
            this.numberNormalization = enabled;
            return this;
        }

        public Builder punctuationRestoration(boolean enabled) {
            this.punctuationRestoration = enabled;
            return this;
        }

        public Builder casingEnabled(boolean enabled) {
            this.casingEnabled = enabled;
            return this;
        }

        public Builder debugMode(boolean enabled) {
            this.debugMode = enabled;
            return this;
        }

        public ProcessingContext build() {
            return new ProcessingContext(this);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Word timestamp for paragraph segmentation
     */
    public static class WordTimestamp {
        public final String word;
        public final double startTime;
        public final double endTime;

        public WordTimestamp(String word, double startTime, double endTime) {
            this.word = word;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
