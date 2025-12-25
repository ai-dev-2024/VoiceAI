package com.voiceai.app.processing;

import com.voiceai.app.processing.processors.*;

/**
 * VoiceAIPipeline - Factory for creating the production post-processing
 * pipeline
 * 
 * This is the single entry point for all text processing.
 * It creates a properly ordered pipeline of processors.
 * 
 * Usage:
 * ProcessingPipeline pipeline = VoiceAIPipeline.create();
 * ProcessingContext context = ProcessingContext.builder()
 * .personalDictionary(myDict)
 * .build();
 * String result = pipeline.process(rawText, context);
 */
public final class VoiceAIPipeline {

    // Singleton instance for efficiency (all processors are stateless)
    private static ProcessingPipeline instance;

    private VoiceAIPipeline() {
    }

    /**
     * Create the default VoiceAI processing pipeline
     * 
     * Pipeline order matters:
     * 0. CommandInterpreter - Detect "delete that", "make formal" etc. (FIRST!)
     * 1. CourseCorrector - Handle "no wait" corrections (may delete text)
     * 2. RepetitionCleaner - Remove stutters
     * 3. PersonalDictionaryApplicator - Apply custom words early
     * 4. FillerRemover - Remove "uh", "um" etc.
     * 5. NumberNormalizer - Convert spoken numbers to digits
     * 6. PunctuationRestorer - Add punctuation
     * 7. CasingApplicator - Apply capitalization last (preserves structure)
     */
    public static ProcessingPipeline create() {
        if (instance == null) {
            instance = new ProcessingPipeline("VoiceAI")
                    .add(new CommandInterpreter()) // Commands detected first!
                    .add(new CourseCorrector())
                    .add(new RepetitionCleaner())
                    .add(new PersonalDictionaryApplicator())
                    .add(new FillerRemover())
                    .add(new NumberNormalizer())
                    .add(new PunctuationRestorer())
                    .add(new CasingApplicator());
        }
        return instance;
    }

    /**
     * Create pipeline with LLM post-processing (Wispr Flow-style)
     * LLM runs LAST to polish the output with AI
     * 
     * @param groqApiKey API key for Groq (or null for rule-based fallback)
     */
    public static ProcessingPipeline createWithLLM(String groqApiKey) {
        ProcessingPipeline pipeline = new ProcessingPipeline("VoiceAI+LLM")
                .add(new CommandInterpreter())
                .add(new CourseCorrector())
                .add(new RepetitionCleaner())
                .add(new PersonalDictionaryApplicator())
                .add(new FillerRemover())
                .add(new NumberNormalizer())
                .add(new PunctuationRestorer())
                .add(new CasingApplicator());

        // Add LLM as final polish step
        if (groqApiKey != null && !groqApiKey.isEmpty()) {
            pipeline.add(new LLMPostProcessor(groqApiKey));
        }

        return pipeline;
    }

    /**
     * Create pipeline with fully OFFLINE ML post-processing
     * Uses LocalLLMProcessor for on-device inference (no internet required)
     * 
     * This is the recommended pipeline for Android devices that need
     * to work completely offline while still having ML-enhanced output.
     */
    public static ProcessingPipeline createOffline() {
        return new ProcessingPipeline("VoiceAI+OfflineLLM")
                .add(new CommandInterpreter())
                .add(new CourseCorrector())
                .add(new RepetitionCleaner())
                .add(new PersonalDictionaryApplicator())
                .add(new FillerRemover())
                .add(new NumberNormalizer())
                .add(new PunctuationRestorer())
                .add(new CasingApplicator())
                .add(new LocalLLMProcessor()); // Offline ML polish
    }

    /**
     * Create a minimal pipeline for fast processing
     * (Skip course correction and advanced formatting)
     */
    public static ProcessingPipeline createMinimal() {
        return new ProcessingPipeline("Minimal")
                .add(new RepetitionCleaner())
                .add(new FillerRemover())
                .add(new CasingApplicator());
    }

    /**
     * Create a debug pipeline that logs each step
     */
    public static ProcessingPipeline createDebug() {
        return new ProcessingPipeline("Debug")
                .add(new CourseCorrector())
                .add(new RepetitionCleaner())
                .add(new PersonalDictionaryApplicator())
                .add(new FillerRemover())
                .add(new NumberNormalizer())
                .add(new PunctuationRestorer())
                .add(new CasingApplicator());
    }

    /**
     * Process text with default settings
     * Convenience method for simple use cases
     */
    public static String process(String rawText) {
        return create().process(rawText, ProcessingContext.builder().build());
    }

    /**
     * Process text with personal dictionary
     * Convenience method for common use case
     */
    public static String process(String rawText, java.util.Map<String, String> personalDict) {
        return create().process(rawText, ProcessingContext.builder()
                .personalDictionary(personalDict)
                .build());
    }
}
