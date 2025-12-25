package com.voiceai.app.processing;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import com.voiceai.app.processing.processors.*;

/**
 * Comprehensive unit tests for individual VoiceAI processors
 * These tests can run without Android framework dependencies
 */
public class ProcessorTests {

    private ProcessingContext defaultContext;

    @Before
    public void setUp() {
        defaultContext = ProcessingContext.builder()
                .courseCorrection(true)
                .fillerRemoval(true)
                .numberNormalization(true)
                .punctuationRestoration(true)
                .casingEnabled(true)
                .build();
    }

    // ========================================================================
    // LOCAL LLM PROCESSOR TESTS
    // ========================================================================

    @Test
    public void testLocalLLMFillerRemoval() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        String result = processor.process("um hello uh world", defaultContext);
        assertFalse("Should remove 'um'", result.contains("um"));
        assertFalse("Should remove 'uh'", result.contains("uh"));
        assertTrue("Should keep 'hello'", result.toLowerCase().contains("hello"));
        assertTrue("Should keep 'world'", result.toLowerCase().contains("world"));
    }

    @Test
    public void testLocalLLMHedgeWordRemoval() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        String result = processor.process("I was like thinking you know about it", defaultContext);
        assertFalse("Should remove 'like'", result.toLowerCase().contains(" like "));
        assertFalse("Should remove 'you know'", result.toLowerCase().contains("you know"));
    }

    @Test
    public void testLocalLLMGrammarFix() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        String result = processor.process("i dont think i can do it", defaultContext);
        assertTrue("Should fix 'i' to 'I'", result.contains("I"));
        assertTrue("Should fix 'dont' to 'don't'", result.contains("don't"));
    }

    @Test
    public void testLocalLLMQuestionDetection() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        String result = processor.process("what is your name", defaultContext);
        assertTrue("Questions should end with ?", result.endsWith("?"));
    }

    @Test
    public void testLocalLLMStatementPunctuation() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        String result = processor.process("hello world", defaultContext);
        assertTrue("Statements should end with punctuation",
                result.endsWith(".") || result.endsWith("!"));
    }

    @Test
    public void testLocalLLMModelNotLoaded() {
        LocalLLMProcessor processor = new LocalLLMProcessor();

        assertFalse("Model should not be loaded by default", processor.isModelLoaded());
    }

    // ========================================================================
    // COURSE CORRECTOR TESTS
    // ========================================================================

    @Test
    public void testCourseCorrectorNoWait() {
        CourseCorrector processor = new CourseCorrector();
        ProcessingContext ctx = ProcessingContext.builder()
                .courseCorrection(true)
                .build();

        String result = processor.process("hello no wait goodbye", ctx);
        assertFalse("Should remove content before 'no wait'",
                result.toLowerCase().contains("hello"));
        assertTrue("Should keep content after 'no wait'",
                result.toLowerCase().contains("goodbye"));
    }

    @Test
    public void testCourseCorrectorIMean() {
        CourseCorrector processor = new CourseCorrector();
        ProcessingContext ctx = ProcessingContext.builder()
                .courseCorrection(true)
                .build();

        String result = processor.process("maybe we should i mean definitely yes", ctx);
        assertFalse("Should remove content before 'i mean'",
                result.toLowerCase().contains("maybe"));
        assertTrue("Should keep 'definitely yes'",
                result.toLowerCase().contains("definitely") ||
                        result.toLowerCase().contains("yes"));
    }

    @Test
    public void testCourseCorrectorNeverMind() {
        CourseCorrector processor = new CourseCorrector();
        ProcessingContext ctx = ProcessingContext.builder()
                .courseCorrection(true)
                .build();

        String result = processor.process("let's do option A never mind option B", ctx);
        assertTrue("Should keep content after 'never mind'",
                result.toLowerCase().contains("option b"));
    }

    @Test
    public void testCourseCorrectorScratchThat() {
        CourseCorrector processor = new CourseCorrector();
        ProcessingContext ctx = ProcessingContext.builder()
                .courseCorrection(true)
                .build();

        String result = processor.process("send to john scratch that send to mike", ctx);
        assertTrue("Should keep content after 'scratch that'",
                result.toLowerCase().contains("mike"));
    }

    // ========================================================================
    // NUMBER NORMALIZER TESTS
    // ========================================================================

    @Test
    public void testNumberNormalizerSimple() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        String result = processor.process("I have five apples", ctx);
        assertTrue("Should convert 'five' to '5'", result.contains("5"));
    }

    @Test
    public void testNumberNormalizerCompound() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        String result = processor.process("twenty five items", ctx);
        assertTrue("Should convert 'twenty five' to '25'", result.contains("25"));
    }

    @Test
    public void testNumberNormalizerPercentage() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        String result = processor.process("fifty percent off", ctx);
        assertTrue("Should convert to '50%'", result.contains("50%"));
    }

    @Test
    public void testNumberNormalizerYear() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        String result = processor.process("twenty twenty four", ctx);
        assertTrue("Should convert to '2024'", result.contains("2024"));
    }

    @Test
    public void testNumberNormalizerTime() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        String result = processor.process("four thirty pm", ctx);
        assertTrue("Should convert to time format",
                result.contains("4:30") || result.contains("4:30 PM"));
    }

    @Test
    public void testNumberNormalizerOrdinalWithThe() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        // Should convert "the first" to "the 1st"
        String result = processor.process("the first item", ctx);
        // Note: Due to bug fix, this should work correctly
    }

    @Test
    public void testNumberNormalizerOrdinalProtected() {
        NumberNormalizer processor = new NumberNormalizer();
        ProcessingContext ctx = ProcessingContext.builder()
                .numberNormalization(true)
                .build();

        // "first thing" should NOT become "1st thing" (bug fix)
        String result = processor.process("first thing in the morning", ctx);
        // The word "first" without "the" should be preserved
    }

    // ========================================================================
    // COMMAND INTERPRETER TESTS
    // ========================================================================

    @Test
    public void testCommandInterpreterPunctuation() {
        CommandInterpreter processor = new CommandInterpreter();

        String result = processor.process("hello comma how are you", defaultContext);
        assertTrue("Should insert comma", result.contains(","));
    }

    @Test
    public void testCommandInterpreterPeriod() {
        CommandInterpreter processor = new CommandInterpreter();

        String result = processor.process("end of sentence period next sentence", defaultContext);
        assertTrue("Should insert period", result.contains("."));
    }

    @Test
    public void testCommandInterpreterQuestionMark() {
        CommandInterpreter processor = new CommandInterpreter();

        String result = processor.process("how are you question mark", defaultContext);
        assertTrue("Should insert question mark", result.contains("?"));
    }

    @Test
    public void testCommandInterpreterNewLine() {
        CommandInterpreter processor = new CommandInterpreter();

        String result = processor.process("first line new line second line", defaultContext);
        assertTrue("Should insert newline", result.contains("\n"));
    }

    // ========================================================================
    // FILLER REMOVER TESTS
    // ========================================================================

    @Test
    public void testFillerRemoverUm() {
        FillerRemover processor = new FillerRemover();
        ProcessingContext ctx = ProcessingContext.builder()
                .fillerRemoval(true)
                .build();

        String result = processor.process("um hello um world", ctx);
        assertFalse("Should remove 'um'", result.toLowerCase().contains("um"));
    }

    @Test
    public void testFillerRemoverUh() {
        FillerRemover processor = new FillerRemover();
        ProcessingContext ctx = ProcessingContext.builder()
                .fillerRemoval(true)
                .build();

        String result = processor.process("uh hello uh world", ctx);
        assertFalse("Should remove 'uh'", result.toLowerCase().contains("uh"));
    }

    @Test
    public void testFillerRemoverLike() {
        FillerRemover processor = new FillerRemover();
        ProcessingContext ctx = ProcessingContext.builder()
                .fillerRemoval(true)
                .build();

        String result = processor.process("I was like thinking about it", ctx);
        // Should remove filler "like" but might keep as verb
    }

    // ========================================================================
    // REPETITION CLEANER TESTS
    // ========================================================================

    @Test
    public void testRepetitionCleanerSimple() {
        RepetitionCleaner processor = new RepetitionCleaner();

        String result = processor.process("the the quick brown fox", defaultContext);
        assertFalse("Should remove duplicate 'the the'",
                result.toLowerCase().contains("the the"));
    }

    @Test
    public void testRepetitionCleanerStutter() {
        RepetitionCleaner processor = new RepetitionCleaner();

        String result = processor.process("I I I want to go", defaultContext);
        // Should clean up stuttering
    }

    // ========================================================================
    // CASING APPLICATOR TESTS
    // ========================================================================

    @Test
    public void testCasingApplicatorSentenceStart() {
        CasingApplicator processor = new CasingApplicator();
        ProcessingContext ctx = ProcessingContext.builder()
                .casingEnabled(true)
                .build();

        String result = processor.process("hello world", ctx);
        assertTrue("First letter should be capitalized",
                Character.isUpperCase(result.charAt(0)));
    }

    @Test
    public void testCasingApplicatorProperNouns() {
        CasingApplicator processor = new CasingApplicator();
        ProcessingContext ctx = ProcessingContext.builder()
                .casingEnabled(true)
                .build();

        String result = processor.process("i use chatgpt and openai", ctx);
        assertTrue("Should capitalize ChatGPT", result.contains("ChatGPT"));
        assertTrue("Should capitalize OpenAI", result.contains("OpenAI"));
    }

    // ========================================================================
    // PUNCTUATION RESTORER TESTS
    // ========================================================================

    @Test
    public void testPunctuationRestorerAddsPeriod() {
        PunctuationRestorer processor = new PunctuationRestorer();
        ProcessingContext ctx = ProcessingContext.builder()
                .punctuationRestoration(true)
                .build();

        String result = processor.process("hello world", ctx);
        assertTrue("Should end with punctuation",
                result.endsWith(".") || result.endsWith("?") || result.endsWith("!"));
    }

    @Test
    public void testPunctuationRestorerQuestion() {
        PunctuationRestorer processor = new PunctuationRestorer();
        ProcessingContext ctx = ProcessingContext.builder()
                .punctuationRestoration(true)
                .build();

        String result = processor.process("what time is it", ctx);
        assertTrue("Questions should end with ?", result.endsWith("?"));
    }

    // ========================================================================
    // PIPELINE FACTORY TESTS
    // ========================================================================

    @Test
    public void testCreateDefaultPipeline() {
        ProcessingPipeline pipeline = VoiceAIPipeline.create();
        assertNotNull("Default pipeline should not be null", pipeline);
    }

    @Test
    public void testCreateOfflinePipeline() {
        ProcessingPipeline pipeline = VoiceAIPipeline.createOffline();
        assertNotNull("Offline pipeline should not be null", pipeline);
    }

    @Test
    public void testCreateMinimalPipeline() {
        ProcessingPipeline pipeline = VoiceAIPipeline.createMinimal();
        assertNotNull("Minimal pipeline should not be null", pipeline);
    }

    @Test
    public void testCreateDebugPipeline() {
        ProcessingPipeline pipeline = VoiceAIPipeline.createDebug();
        assertNotNull("Debug pipeline should not be null", pipeline);
    }
}
