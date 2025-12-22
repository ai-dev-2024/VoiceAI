package com.voiceai.app.processing;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * ProcessingPipeline - Chains TextProcessors in sequence
 * 
 * This is the orchestrator that runs each processor in order.
 * It handles:
 * - Null/empty input safety
 * - Processor skip logic
 * - Debug logging
 * - Error isolation (one processor failing doesn't kill the pipeline)
 */
public class ProcessingPipeline implements TextProcessor {

    private static final String TAG = "VoiceAI.Pipeline";

    private final List<TextProcessor> processors;
    private final String pipelineName;

    public ProcessingPipeline() {
        this("Default");
    }

    public ProcessingPipeline(String name) {
        this.processors = new ArrayList<>();
        this.pipelineName = name;
    }

    /**
     * Add a processor to the pipeline (fluent API)
     */
    public ProcessingPipeline add(TextProcessor processor) {
        if (processor != null) {
            processors.add(processor);
        }
        return this;
    }

    /**
     * Insert a processor at a specific position
     */
    public ProcessingPipeline insert(int index, TextProcessor processor) {
        if (processor != null && index >= 0 && index <= processors.size()) {
            processors.add(index, processor);
        }
        return this;
    }

    /**
     * Remove a processor by class type
     */
    public ProcessingPipeline remove(Class<? extends TextProcessor> type) {
        processors.removeIf(p -> type.isInstance(p));
        return this;
    }

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null) {
            return "";
        }

        String result = text.trim();

        if (result.isEmpty()) {
            return "";
        }

        long pipelineStart = System.currentTimeMillis();

        if (context.isDebugMode()) {
            Log.d(TAG, "[" + pipelineName + "] Input: \"" + truncate(result, 50) + "\"");
        }

        for (TextProcessor processor : processors) {
            // Skip if processor says so
            if (processor.shouldSkip(context)) {
                if (context.isDebugMode()) {
                    Log.d(TAG, "  [SKIP] " + processor.getName());
                }
                continue;
            }

            try {
                long start = System.currentTimeMillis();
                String before = result;
                result = processor.process(result, context);
                long elapsed = System.currentTimeMillis() - start;

                if (context.isDebugMode()) {
                    boolean changed = !before.equals(result);
                    Log.d(TAG, "  [" + (changed ? "✓" : "-") + "] " +
                            processor.getName() + " (" + elapsed + "ms)" +
                            (changed ? " → \"" + truncate(result, 40) + "\"" : ""));
                }

                // Safety: never let a processor return null
                if (result == null) {
                    result = before;
                    Log.w(TAG, "  [WARN] " + processor.getName() + " returned null, reverting");
                }

            } catch (Exception e) {
                Log.e(TAG, "  [ERROR] " + processor.getName() + ": " + e.getMessage());
                // Continue with previous result - don't let one processor kill the pipeline
            }
        }

        long totalElapsed = System.currentTimeMillis() - pipelineStart;

        if (context.isDebugMode()) {
            Log.d(TAG, "[" + pipelineName + "] Output: \"" + truncate(result, 50) +
                    "\" (total: " + totalElapsed + "ms)");
        }

        return result.trim();
    }

    @Override
    public String getName() {
        return pipelineName + "Pipeline";
    }

    /**
     * Get processor count for testing
     */
    public int size() {
        return processors.size();
    }

    private String truncate(String s, int max) {
        if (s == null)
            return "";
        if (s.length() <= max)
            return s;
        return s.substring(0, max) + "...";
    }
}
