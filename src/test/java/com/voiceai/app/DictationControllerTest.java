package com.voiceai.app;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Unit tests for DictationController
 * Note: These tests use mock objects since DictationController
 * depends on Android Context and SharedPreferences
 */
public class DictationControllerTest {

    // ========================================================================
    // CONSTANTS TESTS
    // ========================================================================

    @Test
    public void testDefaultTimeLimitConstant() {
        assertEquals("Default time limit should be 30 seconds",
                30, DictationController.DEFAULT_TIME_LIMIT);
    }

    @Test
    public void testDefaultSilenceThresholdConstant() {
        assertEquals("Default silence threshold should be 1.5 seconds",
                1.5f, DictationController.DEFAULT_SILENCE_THRESHOLD, 0.01f);
    }

    @Test
    public void testSilenceRmsThresholdConstant() {
        assertEquals("Silence RMS threshold should be 0.02",
                0.02f, DictationController.SILENCE_RMS_THRESHOLD, 0.001f);
    }

    // ========================================================================
    // ENUM TESTS
    // ========================================================================

    @Test
    public void testStopReasonValues() {
        DictationController.StopReason[] reasons = DictationController.StopReason.values();
        assertEquals("Should have 4 stop reasons", 4, reasons.length);
    }

    @Test
    public void testStopReasonUserStopped() {
        assertNotNull(DictationController.StopReason.USER_STOPPED);
    }

    @Test
    public void testStopReasonTimeLimit() {
        assertNotNull(DictationController.StopReason.TIME_LIMIT);
    }

    @Test
    public void testStopReasonSilenceDetected() {
        assertNotNull(DictationController.StopReason.SILENCE_DETECTED);
    }

    @Test
    public void testStopReasonError() {
        assertNotNull(DictationController.StopReason.ERROR);
    }

    // ========================================================================
    // PREF KEY TESTS
    // ========================================================================

    @Test
    public void testTimeLimitPrefKey() {
        assertEquals("transcription_time_limit",
                DictationController.PREF_TIME_LIMIT_ENABLED);
    }

    @Test
    public void testSilenceDetectionPrefKey() {
        assertEquals("auto_stop_on_silence",
                DictationController.PREF_SILENCE_DETECTION_ENABLED);
    }
}
