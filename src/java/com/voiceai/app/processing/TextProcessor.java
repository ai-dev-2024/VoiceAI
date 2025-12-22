package com.voiceai.app.processing;

/**
 * TextProcessor - Core interface for modular text processing pipeline
 * 
 * Design Philosophy:
 * - Single Responsibility: Each processor does ONE thing well
 * - Chain of Responsibility: Processors can be composed in any order
 * - Stateless: Processors are pure functions (input → output)
 * - Testable: Each processor can be unit tested in isolation
 * 
 * Usage:
 * TextProcessor pipeline = new ProcessingPipeline()
 * .add(new CourseCorrector())
 * .add(new FillerRemover())
 * .add(new NumberNormalizer())
 * .add(new PunctuationRestorer())
 * .add(new CasingApplicator());
 * 
 * String result = pipeline.process(rawText, context);
 */
public interface TextProcessor {

    /**
     * Process text through this processor
     * 
     * @param text    Input text (may be null or empty)
     * @param context Processing context with settings and metadata
     * @return Processed text (never null, may be empty)
     */
    String process(String text, ProcessingContext context);

    /**
     * Human-readable name for logging and debugging
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Whether this processor should be skipped (e.g., disabled in settings)
     */
    default boolean shouldSkip(ProcessingContext context) {
        return false;
    }
}
