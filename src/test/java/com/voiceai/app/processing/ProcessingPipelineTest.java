package com.voiceai.app.processing;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for VoiceAI Processing Pipeline
 * Tests all processors in isolation and as a complete pipeline
 */
public class ProcessingPipelineTest {

    private ProcessingPipeline pipeline;
    private ProcessingContext defaultContext;

    @Before
    public void setUp() {
        pipeline = VoiceAIPipeline.create();
        defaultContext = ProcessingContext.builder()
                .courseCorrection(true)
                .fillerRemoval(true)
                .numberNormalization(true)
                .punctuationRestoration(true)
                .casingEnabled(true)
                .debugMode(false)
                .build();
    }

    // ========================================================================
    // PIPELINE BASIC TESTS
    // ========================================================================

    @Test
    public void testNullInput() {
        String result = pipeline.process(null, defaultContext);
        assertEquals("", result);
    }

    @Test
    public void testEmptyInput() {
        String result = pipeline.process("", defaultContext);
        assertEquals("", result);
    }

    @Test
    public void testWhitespaceOnlyInput() {
        String result = pipeline.process("   ", defaultContext);
        assertEquals("", result);
    }

    @Test
    public void testSimpleInput() {
        String result = pipeline.process("hello world", defaultContext);
        assertNotNull(result);
        assertTrue("Result should not be empty", result.length() > 0);
    }

    // ========================================================================
    // FILLER REMOVAL TESTS
    // ========================================================================

    @Test
    public void testFillerRemoval() {
        ProcessingContext ctx = ProcessingContext.builder()
                .fillerRemoval(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("um hello uh world", ctx);
        assertFalse("Fillers should be removed", result.toLowerCase().contains("um"));
        assertFalse("Fillers should be removed", result.toLowerCase().contains("uh"));
    }

    @Test
    public void testFillerRemovalPreservesContent() {
        ProcessingContext ctx = ProcessingContext.builder()
                .fillerRemoval(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("like I was like thinking", ctx);
        assertTrue("Should contain actual content", result.toLowerCase().contains("thinking"));
    }

    // ========================================================================
    // NUMBER NORMALIZATION TESTS
    // ========================================================================

    @Test
    public void testNumberNormalization() {
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("I have five apples", ctx);
        assertTrue("Should convert 'five' to '5'", result.contains("5"));
    }

    @Test
    public void testCompoundNumberNormalization() {
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("twenty five dollars", ctx);
        assertTrue("Should convert 'twenty five' to '25'", result.contains("25"));
    }

    @Test
    public void testPercentageNormalization() {
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("thirty percent off", ctx);
        assertTrue("Should convert to '30%'", result.contains("30%"));
    }

    // ========================================================================
    // CASING TESTS
    // ========================================================================

    @Test
    public void testSentenceCasing() {
        ProcessingContext ctx = ProcessingContext.builder()
                .casingEnabled(true)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("hello world", ctx);
        assertTrue("First letter should be capitalized", Character.isUpperCase(result.charAt(0)));
    }

    // ========================================================================
    // PUNCTUATION TESTS
    // ========================================================================

    @Test
    public void testPunctuationAdded() {
        ProcessingContext ctx = ProcessingContext.builder()
                .punctuationRestoration(true)
                .casingEnabled(false)
                .build();

        String result = pipeline.process("hello world", ctx);
        assertTrue("Should end with punctuation",
                result.endsWith(".") || result.endsWith("?") || result.endsWith("!"));
    }

    @Test
    public void testQuestionMarkForQuestions() {
        ProcessingContext ctx = ProcessingContext.builder()
                .punctuationRestoration(true)
                .casingEnabled(false)
                .build();

        String result = pipeline.process("what is your name", ctx);
        assertTrue("Questions should end with ?", result.endsWith("?"));
    }

    // ========================================================================
    // PERSONAL DICTIONARY TESTS
    // ========================================================================

    @Test
    public void testPersonalDictionary() {
        Map<String, String> dict = new HashMap<>();
        dict.put("groq", "Groq");
        dict.put("chatgpt", "ChatGPT");

        ProcessingContext ctx = ProcessingContext.builder()
                .personalDictionary(dict)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("I love groq and chatgpt", ctx);
        assertTrue("Should preserve 'Groq' casing", result.contains("Groq"));
        assertTrue("Should preserve 'ChatGPT' casing", result.contains("ChatGPT"));
    }

    // ========================================================================
    // COURSE CORRECTION TESTS
    // ========================================================================

    @Test
    public void testCourseCorrection() {
        ProcessingContext ctx = ProcessingContext.builder()
                .courseCorrection(true)
                .casingEnabled(false)
                .punctuationRestoration(false)
                .build();

        String result = pipeline.process("hello no wait world", ctx);
        // Course corrector should remove content before "no wait"
        assertFalse("'hello' should be removed by course correction",
                result.toLowerCase().startsWith("hello"));
    }

    // ========================================================================
    // FULL PIPELINE INTEGRATION TESTS
    // ========================================================================

    @Test
    public void testFullPipelineIntegration() {
        String input = "um hello world how are you";
        String result = pipeline.process(input, defaultContext);

        assertNotNull("Result should not be null", result);
        assertTrue("Result should have content", result.length() > 0);
        assertFalse("Fillers should be removed", result.toLowerCase().contains("um"));
    }

    @Test
    public void testComplexInput() {
        Map<String, String> dict = new HashMap<>();
        dict.put("openai", "OpenAI");

        ProcessingContext ctx = ProcessingContext.builder()
                .personalDictionary(dict)
                .courseCorrection(true)
                .fillerRemoval(true)
                .numberNormalization(true)
                .punctuationRestoration(true)
                .casingEnabled(true)
                .build();

        String input = "uh I think openai has like five hundred employees";
        String result = pipeline.process(input, ctx);

        assertNotNull(result);
        assertTrue("Should capitalize first letter", Character.isUpperCase(result.charAt(0)));
        assertTrue("Should preserve 'OpenAI'", result.contains("OpenAI"));
    }

    // ========================================================================
    // EDGE CASES
    // ========================================================================

    @Test
    public void testSpecialCharacters() {
        String result = pipeline.process("hello @ world # test", defaultContext);
        assertNotNull(result);
    }

    @Test
    public void testUnicodeInput() {
        String result = pipeline.process("hello 你好 world", defaultContext);
        assertNotNull(result);
    }

    @Test
    public void testVeryLongInput() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("word ");
        }
        String result = pipeline.process(sb.toString(), defaultContext);
        assertNotNull(result);
    }
}
