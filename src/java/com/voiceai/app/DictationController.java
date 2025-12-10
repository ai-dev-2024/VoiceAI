package com.voiceai.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * DictationController - FUTO-style dictation management
 * 
 * Features:
 * - 30-second dictation limit (toggle)
 * - Silence detection auto-stop (toggle)
 * - Settings persistence via SharedPreferences
 * - Real-time audio level monitoring
 */
public class DictationController {

    private static final String TAG = "DictationController";

    // Must match SettingsActivity
    private static final String PREFS_NAME = "VoiceAIPrefs";
    public static final String PREF_TIME_LIMIT_ENABLED = "transcription_time_limit";
    public static final String PREF_SILENCE_DETECTION_ENABLED = "auto_stop_on_silence";

    // Default values
    public static final int DEFAULT_TIME_LIMIT = 30;
    public static final float DEFAULT_SILENCE_THRESHOLD = 1.5f;
    public static final float SILENCE_RMS_THRESHOLD = 0.02f; // Audio level below this = silence

    // State
    private Context context;
    private SharedPreferences prefs;
    private Handler handler;
    private DictationListener listener;

    // Timer state
    private Runnable timeLimitRunnable;
    private long dictationStartTime;
    private boolean isRunning;

    // Silence detection state
    private float lastAudioLevel;
    private long silenceStartTime;
    private Runnable silenceCheckRunnable;

    // Settings
    private boolean timeLimitEnabled;
    private int timeLimitSeconds;
    private boolean silenceDetectionEnabled;
    private float silenceThresholdSeconds;

    public interface DictationListener {
        void onDictationStarted();

        void onDictationStopped(StopReason reason);

        void onTimeRemaining(int secondsRemaining);

        void onSilenceDetected();
    }

    public enum StopReason {
        USER_STOPPED,
        TIME_LIMIT,
        SILENCE_DETECTED,
        ERROR
    }

    public DictationController(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.handler = new Handler(Looper.getMainLooper());
        this.isRunning = false;

        loadSettings();
    }

    // ========================================================================
    // SETTINGS MANAGEMENT
    // ========================================================================

    public void loadSettings() {
        timeLimitEnabled = prefs.getBoolean(PREF_TIME_LIMIT_ENABLED, true);
        timeLimitSeconds = DEFAULT_TIME_LIMIT; // Fixed at 30 seconds
        silenceDetectionEnabled = prefs.getBoolean(PREF_SILENCE_DETECTION_ENABLED, true);
        silenceThresholdSeconds = DEFAULT_SILENCE_THRESHOLD;

        Log.d(TAG, "Settings loaded: timeLimit=" + timeLimitEnabled + " (" + timeLimitSeconds + "s), " +
                "silenceDetection=" + silenceDetectionEnabled + " (" + silenceThresholdSeconds + "s)");
    }

    public void saveSettings() {
        prefs.edit()
                .putBoolean(PREF_TIME_LIMIT_ENABLED, timeLimitEnabled)
                .putBoolean(PREF_SILENCE_DETECTION_ENABLED, silenceDetectionEnabled)
                .apply();

        Log.d(TAG, "Settings saved");
    }

    public void setTimeLimitEnabled(boolean enabled) {
        this.timeLimitEnabled = enabled;
        saveSettings();
    }

    public void setTimeLimitSeconds(int seconds) {
        this.timeLimitSeconds = seconds;
        saveSettings();
    }

    public void setSilenceDetectionEnabled(boolean enabled) {
        this.silenceDetectionEnabled = enabled;
        saveSettings();
    }

    public void setSilenceThresholdSeconds(float seconds) {
        this.silenceThresholdSeconds = seconds;
        saveSettings();
    }

    public boolean isTimeLimitEnabled() {
        return timeLimitEnabled;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public boolean isSilenceDetectionEnabled() {
        return silenceDetectionEnabled;
    }

    public float getSilenceThresholdSeconds() {
        return silenceThresholdSeconds;
    }

    // ========================================================================
    // DICTATION CONTROL
    // ========================================================================

    public void setListener(DictationListener listener) {
        this.listener = listener;
    }

    public void startDictation() {
        if (isRunning) {
            Log.w(TAG, "Dictation already running");
            return;
        }

        loadSettings(); // Reload in case settings changed

        isRunning = true;
        dictationStartTime = System.currentTimeMillis();
        silenceStartTime = 0;
        lastAudioLevel = 1.0f;

        Log.d(TAG, "Dictation started");

        // Start time limit timer if enabled
        if (timeLimitEnabled) {
            startTimeLimitTimer();
        }

        // Start silence detection if enabled
        if (silenceDetectionEnabled) {
            startSilenceDetection();
        }

        if (listener != null) {
            listener.onDictationStarted();
        }
    }

    public void stopDictation(StopReason reason) {
        if (!isRunning) {
            return;
        }

        isRunning = false;

        // Cancel timers
        if (timeLimitRunnable != null) {
            handler.removeCallbacks(timeLimitRunnable);
            timeLimitRunnable = null;
        }

        if (silenceCheckRunnable != null) {
            handler.removeCallbacks(silenceCheckRunnable);
            silenceCheckRunnable = null;
        }

        Log.d(TAG, "Dictation stopped: " + reason);

        // Show toast notification
        String message;
        switch (reason) {
            case TIME_LIMIT:
                message = "Dictation stopped: " + timeLimitSeconds + "s limit reached";
                break;
            case SILENCE_DETECTED:
                message = "Dictation stopped: silence detected";
                break;
            case USER_STOPPED:
                message = null; // No toast for user stop
                break;
            default:
                message = "Dictation stopped";
        }

        if (message != null) {
            handler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        }

        if (listener != null) {
            listener.onDictationStopped(reason);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    // ========================================================================
    // TIME LIMIT TIMER
    // ========================================================================

    private void startTimeLimitTimer() {
        Log.d(TAG, "Starting " + timeLimitSeconds + "s time limit timer");

        // Countdown updates every second
        final int[] secondsRemaining = { timeLimitSeconds };

        timeLimitRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning)
                    return;

                secondsRemaining[0]--;

                if (listener != null) {
                    listener.onTimeRemaining(secondsRemaining[0]);
                }

                if (secondsRemaining[0] <= 0) {
                    stopDictation(StopReason.TIME_LIMIT);
                } else {
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.postDelayed(timeLimitRunnable, 1000);
    }

    // ========================================================================
    // SILENCE DETECTION
    // ========================================================================

    private void startSilenceDetection() {
        Log.d(TAG, "Starting silence detection (threshold: " + silenceThresholdSeconds + "s)");

        silenceStartTime = 0;

        silenceCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning)
                    return;

                // Check if currently silent
                if (lastAudioLevel < SILENCE_RMS_THRESHOLD) {
                    if (silenceStartTime == 0) {
                        silenceStartTime = System.currentTimeMillis();
                    }

                    float silenceDuration = (System.currentTimeMillis() - silenceStartTime) / 1000f;

                    if (silenceDuration >= silenceThresholdSeconds) {
                        if (listener != null) {
                            listener.onSilenceDetected();
                        }
                        stopDictation(StopReason.SILENCE_DETECTED);
                        return;
                    }
                } else {
                    // Reset silence timer
                    silenceStartTime = 0;
                }

                // Check every 100ms
                handler.postDelayed(this, 100);
            }
        };

        handler.postDelayed(silenceCheckRunnable, 100);
    }

    /**
     * Call this from audio capture to update audio level
     * 
     * @param level RMS audio level (0.0 - 1.0)
     */
    public void updateAudioLevel(float level) {
        this.lastAudioLevel = level;
    }

    // ========================================================================
    // ELAPSED TIME
    // ========================================================================

    public int getElapsedSeconds() {
        if (!isRunning || dictationStartTime == 0) {
            return 0;
        }
        return (int) ((System.currentTimeMillis() - dictationStartTime) / 1000);
    }

    public int getRemainingSeconds() {
        if (!timeLimitEnabled) {
            return -1; // Unlimited
        }
        return Math.max(0, timeLimitSeconds - getElapsedSeconds());
    }

    // ========================================================================
    // CLEANUP
    // ========================================================================

    public void cleanup() {
        if (isRunning) {
            stopDictation(StopReason.USER_STOPPED);
        }

        if (timeLimitRunnable != null) {
            handler.removeCallbacks(timeLimitRunnable);
        }

        if (silenceCheckRunnable != null) {
            handler.removeCallbacks(silenceCheckRunnable);
        }

        listener = null;
    }
}
