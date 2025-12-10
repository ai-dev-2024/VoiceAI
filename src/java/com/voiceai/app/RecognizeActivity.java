package com.voiceai.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * RecognizeActivity - Transparent overlay voice dictation UI
 * Handles android.speech.action.RECOGNIZE_SPEECH from any keyboard
 * Uses DictationController for 30s timer and silence detection (FUTO-style)
 */
public class RecognizeActivity extends Activity implements DictationController.DictationListener {

    private static final String TAG = "VoiceAI";
    private static final String PREFS_NAME = "voiceai_settings";

    static {
        try {
            System.loadLibrary("android_transcribe_app");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }

    private TextView statusText;
    private TextView resultText;
    private TextView timerText;
    private SoundWaveView waveformView;
    private Handler mainHandler;
    private boolean isRecording = false;
    private String transcribedText = "";
    private boolean fromService = false;

    // Dictation Controller for timer and silence detection
    private DictationController dictationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make window transparent overlay
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setDimAmount(0.5f); // 50% dim

        mainHandler = new Handler(Looper.getMainLooper());

        // Initialize DictationController
        dictationController = new DictationController(this);
        dictationController.setListener(this);

        // Check if launched from VoiceRecognitionService (HeliBoard, etc.)
        fromService = getIntent().getBooleanExtra("from_service", false);

        createOverlayUI();

        // Start recording immediately
        try {
            initNative(this);
            startRecording();
            isRecording = true;
            statusText.setText("Listening...");
            waveformView.startAnimation();

            // Start DictationController (handles timer and silence detection)
            dictationController.startDictation();
            updateTimerDisplay();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            statusText.setText("Error: " + e.getMessage());
        }
    }

    // DictationController.DictationListener callbacks
    @Override
    public void onDictationStarted() {
        Log.d(TAG, "DictationController: started");
    }

    @Override
    public void onDictationStopped(DictationController.StopReason reason) {
        Log.d(TAG, "DictationController: stopped - " + reason);
        if (isRecording) {
            stopAndReturn();
        }
    }

    @Override
    public void onTimeRemaining(int secondsRemaining) {
        mainHandler.post(() -> {
            if (timerText != null && dictationController.isTimeLimitEnabled()) {
                timerText.setText(secondsRemaining + "s");
            }
        });
    }

    @Override
    public void onSilenceDetected() {
        Log.d(TAG, "Silence detected");
    }

    private void updateTimerDisplay() {
        if (timerText != null) {
            if (dictationController.isTimeLimitEnabled()) {
                timerText.setVisibility(View.VISIBLE);
                timerText.setText(dictationController.getTimeLimitSeconds() + "s");
            } else {
                timerText.setVisibility(View.GONE);
            }
        }
    }

    private void createOverlayUI() {
        // Root frame with semi-transparent dim background
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0x80000000); // 50% black dim - the dim effect
        // NO click listener on root - only card tap should stop dictation

        // Overlay card - dark transparent
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);

        // Dark transparent background (50% black)
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x80000000); // 50% black
        bg.setCornerRadius(32f);
        card.setBackground(bg);
        card.setPadding(48, 40, 48, 40);
        card.setElevation(16f);

        // Card click also stops dictation
        card.setOnClickListener(v -> {
            if (isRecording)
                stopAndReturn();
            else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        // Status text
        statusText = new TextView(this);
        statusText.setText("Listening...");
        statusText.setTextSize(18);
        statusText.setTextColor(Color.WHITE);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, 0, 0, 8);
        card.addView(statusText);

        // Timer display (shows countdown when time limit enabled)
        timerText = new TextView(this);
        timerText.setTextSize(14);
        timerText.setTextColor(0xFFFFAA00); // Orange
        timerText.setGravity(Gravity.CENTER);
        timerText.setPadding(0, 0, 0, 12);
        if (dictationController.isTimeLimitEnabled()) {
            timerText.setText(dictationController.getTimeLimitSeconds() + "s");
            timerText.setVisibility(View.VISIBLE);
        } else {
            timerText.setVisibility(View.GONE);
        }
        card.addView(timerText);

        // Waveform view - responsive to audio
        waveformView = new SoundWaveView(this);
        LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(
                dpToPx(260), dpToPx(100));
        waveParams.setMargins(0, 8, 0, 8);
        waveformView.setLayoutParams(waveParams);
        card.addView(waveformView);

        // Result text (shows live transcription)
        resultText = new TextView(this);
        resultText.setText("");
        resultText.setTextSize(16);
        resultText.setTextColor(0xFFCCCCCC);
        resultText.setGravity(Gravity.CENTER);
        resultText.setPadding(0, 12, 0, 0);
        resultText.setMaxLines(3);
        card.addView(resultText);

        // Hint text
        TextView hint = new TextView(this);
        hint.setText("Tap to stop");
        hint.setTextSize(12);
        hint.setTextColor(0x99FFFFFF);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, 12, 0, 0);
        card.addView(hint);

        // Center the card
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        root.addView(card, cardParams);

        setContentView(root);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (isRecording)
            stopAndReturn();
        else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void stopAndReturn() {
        isRecording = false;
        statusText.setText("Processing...");
        waveformView.stopAnimation();

        try {
            stopRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            returnResult("");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (waveformView != null)
            waveformView.stopAnimation();
        try {
            cleanupNative();
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up", e);
        }
    }

    // Native methods
    private native void initNative(RecognizeActivity activity);

    private native void cleanupNative();

    private native void startRecording();

    private native void stopRecording();

    // Called from Rust with status updates
    public void onStatusUpdate(String status) {
        mainHandler.post(() -> {
            Log.d(TAG, "Status: " + status);
            statusText.setText(status);
        });
    }

    // Called from Rust when transcription complete
    public void onTextTranscribed(String text) {
        mainHandler.post(() -> {
            Log.d(TAG, "Raw transcribed: " + text);

            // Run through Stage 2 post-processing pipeline
            String processed = PostProcessor.processTranscript(text);

            // Apply personal dictionary on top
            transcribedText = applyPersonalDictionary(processed);

            Log.d(TAG, "Processed: " + transcribedText);
            resultText.setText(transcribedText);
            statusText.setText("Done!");
            mainHandler.postDelayed(() -> returnResult(transcribedText), 200);
        });
    }

    // Apply personal dictionary replacements
    private String applyPersonalDictionary(String text) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String dictStr = prefs.getString("personal_dictionary", "");

        if (dictStr.isEmpty())
            return text;

        String result = text;
        String[] words = dictStr.split(",");
        for (String word : words) {
            word = word.trim();
            if (word.isEmpty())
                continue;

            if (word.startsWith("@") && word.length() > 1) {
                // @mention: "at groq" → "@Groq"
                String withoutAt = word.substring(1);
                result = result.replaceAll("(?i)\\bat\\s+" +
                        java.util.regex.Pattern.quote(withoutAt) + "\\b", word);
            } else {
                // Normal word replacement
                result = result.replaceAll("(?i)\\b" +
                        java.util.regex.Pattern.quote(word) + "\\b", word);
            }
        }
        return result;
    }

    // Called from Rust with audio level (0.0 - 1.0) - RESPONSIVE WAVEFORM
    public void onAudioLevel(float level) {
        mainHandler.post(() -> {
            if (waveformView != null) {
                waveformView.setAudioLevel(level);
            }

            // Feed audio level to DictationController for silence detection
            if (dictationController != null) {
                dictationController.updateAudioLevel(level);
            }
        });
    }

    private void returnResult(String text) {
        Log.d(TAG, "returnResult: text=" + text + ", fromService=" + fromService);

        if (text != null && !text.isEmpty()) {
            // ALWAYS inject text via accessibility for ALL apps (Word, Google, etc.)
            // This is the most reliable method that works everywhere
            Log.d(TAG, "Injecting text via accessibility service");
            boolean injected = VoiceTextInjectionService.injectText(this, text);

            if (!injected) {
                Log.d(TAG, "Accessibility injection failed, text copied to clipboard");
            }

            // If launched from VoiceRecognitionService (HeliBoard), also send results
            // via service callback (some keyboards handle this better)
            if (fromService) {
                VoiceRecognitionService.sendResults(text);
            }

            // Also set activity result for keyboards that support it
            ArrayList<String> results = new ArrayList<>();
            results.add(text);
            Intent resultIntent = new Intent();
            resultIntent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, results);
            setResult(RESULT_OK, resultIntent);
        } else {
            if (fromService) {
                VoiceRecognitionService.sendCancelled();
            }
            setResult(RESULT_CANCELED);
        }
        finish();
    }

    // Post-processing with personal dictionary, @mentions, currency, percentages
    private String postProcess(String text) {
        String result = text;

        // === @Mentions: "at Groq" → "@Groq" ===
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String dictStr = prefs.getString("personal_dictionary", "");
        if (!dictStr.isEmpty()) {
            String[] words = dictStr.split(",");
            for (String word : words) {
                word = word.trim();
                if (word.startsWith("@") && word.length() > 1) {
                    String withoutAt = word.substring(1);
                    result = result.replaceAll("(?i)\\bat\\s+" + escapeRegex(withoutAt) + "\\b", word);
                }
            }
        }

        // === Compound numbers: "twenty five" → "25", "one hundred" → "100" ===
        // Handle teens first
        result = result.replaceAll("(?i)\\bthirteen\\b", "13");
        result = result.replaceAll("(?i)\\bfourteen\\b", "14");
        result = result.replaceAll("(?i)\\bfifteen\\b", "15");
        result = result.replaceAll("(?i)\\bsixteen\\b", "16");
        result = result.replaceAll("(?i)\\bseventeen\\b", "17");
        result = result.replaceAll("(?i)\\beighteen\\b", "18");
        result = result.replaceAll("(?i)\\bnineteen\\b", "19");

        // Handle compound tens (twenty one → 21)
        result = result.replaceAll("(?i)\\btwenty\\s*one\\b", "21");
        result = result.replaceAll("(?i)\\btwenty\\s*two\\b", "22");
        result = result.replaceAll("(?i)\\btwenty\\s*three\\b", "23");
        result = result.replaceAll("(?i)\\btwenty\\s*four\\b", "24");
        result = result.replaceAll("(?i)\\btwenty\\s*five\\b", "25");
        result = result.replaceAll("(?i)\\btwenty\\s*six\\b", "26");
        result = result.replaceAll("(?i)\\btwenty\\s*seven\\b", "27");
        result = result.replaceAll("(?i)\\btwenty\\s*eight\\b", "28");
        result = result.replaceAll("(?i)\\btwenty\\s*nine\\b", "29");
        result = result.replaceAll("(?i)\\bthirty\\s*one\\b", "31");
        result = result.replaceAll("(?i)\\bthirty\\s*two\\b", "32");
        result = result.replaceAll("(?i)\\bthirty\\s*three\\b", "33");
        result = result.replaceAll("(?i)\\bthirty\\s*four\\b", "34");
        result = result.replaceAll("(?i)\\bthirty\\s*five\\b", "35");
        result = result.replaceAll("(?i)\\bthirty\\s*six\\b", "36");
        result = result.replaceAll("(?i)\\bthirty\\s*seven\\b", "37");
        result = result.replaceAll("(?i)\\bthirty\\s*eight\\b", "38");
        result = result.replaceAll("(?i)\\bthirty\\s*nine\\b", "39");
        result = result.replaceAll("(?i)\\bforty\\s*one\\b", "41");
        result = result.replaceAll("(?i)\\bforty\\s*two\\b", "42");
        result = result.replaceAll("(?i)\\bforty\\s*three\\b", "43");
        result = result.replaceAll("(?i)\\bforty\\s*four\\b", "44");
        result = result.replaceAll("(?i)\\bforty\\s*five\\b", "45");
        result = result.replaceAll("(?i)\\bforty\\s*six\\b", "46");
        result = result.replaceAll("(?i)\\bforty\\s*seven\\b", "47");
        result = result.replaceAll("(?i)\\bforty\\s*eight\\b", "48");
        result = result.replaceAll("(?i)\\bforty\\s*nine\\b", "49");
        result = result.replaceAll("(?i)\\bfifty\\s*one\\b", "51");
        result = result.replaceAll("(?i)\\bfifty\\s*two\\b", "52");
        result = result.replaceAll("(?i)\\bfifty\\s*three\\b", "53");
        result = result.replaceAll("(?i)\\bfifty\\s*four\\b", "54");
        result = result.replaceAll("(?i)\\bfifty\\s*five\\b", "55");
        result = result.replaceAll("(?i)\\bfifty\\s*six\\b", "56");
        result = result.replaceAll("(?i)\\bfifty\\s*seven\\b", "57");
        result = result.replaceAll("(?i)\\bfifty\\s*eight\\b", "58");
        result = result.replaceAll("(?i)\\bfifty\\s*nine\\b", "59");
        result = result.replaceAll("(?i)\\bsixty\\s*one\\b", "61");
        result = result.replaceAll("(?i)\\bsixty\\s*two\\b", "62");
        result = result.replaceAll("(?i)\\bsixty\\s*three\\b", "63");
        result = result.replaceAll("(?i)\\bsixty\\s*four\\b", "64");
        result = result.replaceAll("(?i)\\bsixty\\s*five\\b", "65");
        result = result.replaceAll("(?i)\\bsixty\\s*six\\b", "66");
        result = result.replaceAll("(?i)\\bsixty\\s*seven\\b", "67");
        result = result.replaceAll("(?i)\\bsixty\\s*eight\\b", "68");
        result = result.replaceAll("(?i)\\bsixty\\s*nine\\b", "69");
        result = result.replaceAll("(?i)\\bseventy\\s*one\\b", "71");
        result = result.replaceAll("(?i)\\bseventy\\s*two\\b", "72");
        result = result.replaceAll("(?i)\\bseventy\\s*three\\b", "73");
        result = result.replaceAll("(?i)\\bseventy\\s*four\\b", "74");
        result = result.replaceAll("(?i)\\bseventy\\s*five\\b", "75");
        result = result.replaceAll("(?i)\\bseventy\\s*six\\b", "76");
        result = result.replaceAll("(?i)\\bseventy\\s*seven\\b", "77");
        result = result.replaceAll("(?i)\\bseventy\\s*eight\\b", "78");
        result = result.replaceAll("(?i)\\bseventy\\s*nine\\b", "79");
        result = result.replaceAll("(?i)\\beighty\\s*one\\b", "81");
        result = result.replaceAll("(?i)\\beighty\\s*two\\b", "82");
        result = result.replaceAll("(?i)\\beighty\\s*three\\b", "83");
        result = result.replaceAll("(?i)\\beighty\\s*four\\b", "84");
        result = result.replaceAll("(?i)\\beighty\\s*five\\b", "85");
        result = result.replaceAll("(?i)\\beighty\\s*six\\b", "86");
        result = result.replaceAll("(?i)\\beighty\\s*seven\\b", "87");
        result = result.replaceAll("(?i)\\beighty\\s*eight\\b", "88");
        result = result.replaceAll("(?i)\\beighty\\s*nine\\b", "89");
        result = result.replaceAll("(?i)\\bninety\\s*one\\b", "91");
        result = result.replaceAll("(?i)\\bninety\\s*two\\b", "92");
        result = result.replaceAll("(?i)\\bninety\\s*three\\b", "93");
        result = result.replaceAll("(?i)\\bninety\\s*four\\b", "94");
        result = result.replaceAll("(?i)\\bninety\\s*five\\b", "95");
        result = result.replaceAll("(?i)\\bninety\\s*six\\b", "96");
        result = result.replaceAll("(?i)\\bninety\\s*seven\\b", "97");
        result = result.replaceAll("(?i)\\bninety\\s*eight\\b", "98");
        result = result.replaceAll("(?i)\\bninety\\s*nine\\b", "99");

        // Simple numbers (after compound processing)
        result = result.replaceAll("(?i)\\bzero\\b", "0");
        result = result.replaceAll("(?i)\\bone\\b", "1");
        result = result.replaceAll("(?i)\\btwo\\b", "2");
        result = result.replaceAll("(?i)\\bthree\\b", "3");
        result = result.replaceAll("(?i)\\bfour\\b", "4");
        result = result.replaceAll("(?i)\\bfive\\b", "5");
        result = result.replaceAll("(?i)\\bsix\\b", "6");
        result = result.replaceAll("(?i)\\bseven\\b", "7");
        result = result.replaceAll("(?i)\\beight\\b", "8");
        result = result.replaceAll("(?i)\\bnine\\b", "9");
        result = result.replaceAll("(?i)\\bten\\b", "10");
        result = result.replaceAll("(?i)\\beleven\\b", "11");
        result = result.replaceAll("(?i)\\btwelve\\b", "12");
        result = result.replaceAll("(?i)\\btwenty\\b", "20");
        result = result.replaceAll("(?i)\\bthirty\\b", "30");
        result = result.replaceAll("(?i)\\bforty\\b", "40");
        result = result.replaceAll("(?i)\\bfifty\\b", "50");
        result = result.replaceAll("(?i)\\bsixty\\b", "60");
        result = result.replaceAll("(?i)\\bseventy\\b", "70");
        result = result.replaceAll("(?i)\\beighty\\b", "80");
        result = result.replaceAll("(?i)\\bninety\\b", "90");
        result = result.replaceAll("(?i)\\bhundred\\b", "100");

        // === Percentages: "25 percent" → "25%" ===
        result = result.replaceAll("(?i)(\\d+)\\s*percent", "$1%");
        result = result.replaceAll("(?i)(\\d+)\\s+%", "$1%");

        // === Currency (order matters - most specific first) ===
        // "100 million US dollars" → "$100,000,000 USD"
        result = result.replaceAll("(?i)(\\d+)\\s*million\\s+US\\s*dollars?", "\\$$1,000,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s*million\\s*dollars?", "\\$$1,000,000");
        // "100 thousand US dollars" → "$100,000 USD"
        result = result.replaceAll("(?i)(\\d+)\\s*thousand\\s+US\\s*dollars?", "\\$$1,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s*thousand\\s*dollars?", "\\$$1,000");
        // "100 US dollars" → "$100 USD"
        result = result.replaceAll("(?i)(\\d+)\\s+US\\s*dollars?", "\\$$1 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+USD", "\\$$1 USD");
        // "100 dollars" → "$100"
        result = result.replaceAll("(?i)(\\d+)\\s*dollars?", "\\$$1");
        // "50 cents" → "50¢"
        result = result.replaceAll("(?i)(\\d+)\\s*cents?", "$1¢");

        // === Common AI/Tech names ===
        result = result.replaceAll("(?i)\\bgrok\\b", "Groq");
        result = result.replaceAll("(?i)\\bgemini\\b", "Gemini");
        result = result.replaceAll("(?i)\\bchat\\s*gpt\\b", "ChatGPT");
        result = result.replaceAll("(?i)\\bopen\\s*ai\\b", "OpenAI");
        result = result.replaceAll("(?i)\\bclaude\\b", "Claude");
        result = result.replaceAll("(?i)\\banthropic\\b", "Anthropic");
        result = result.replaceAll("(?i)\\bllama\\b", "Llama");
        result = result.replaceAll("(?i)\\bmistral\\b", "Mistral");

        // === Personal dictionary (non-@ words) ===
        if (!dictStr.isEmpty()) {
            String[] words = dictStr.split(",");
            for (String word : words) {
                word = word.trim();
                if (!word.isEmpty() && !word.startsWith("@")) {
                    // Use Pattern.quote for safe regex escaping
                    String escaped = java.util.regex.Pattern.quote(word.toLowerCase());
                    result = result.replaceAll("(?i)\\b" + escaped + "\\b", word);
                }
            }
        }

        return result;
    }

    // Escape special regex characters (using Pattern.quote is safer)
    private String escapeRegex(String str) {
        return java.util.regex.Pattern.quote(str);
    }

    // Sound Wave View - Responsive to actual audio levels
    private static class SoundWaveView extends View {
        private Paint wavePaint;
        private Path wavePath;
        private float[] waveAmplitudes = new float[32];
        private float audioLevel = 0.1f;
        private Handler handler = new Handler(Looper.getMainLooper());
        private boolean isAnimating = false;
        private Runnable animationRunnable;
        private int animOffset = 0;

        public SoundWaveView(Context context) {
            super(context);
            init();
        }

        public SoundWaveView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            wavePaint.setStyle(Paint.Style.STROKE);
            wavePaint.setStrokeWidth(3f);
            wavePaint.setStrokeCap(Paint.Cap.ROUND);
            wavePaint.setColor(0xFFFFFFFF); // White wave

            wavePath = new Path();

            for (int i = 0; i < waveAmplitudes.length; i++) {
                waveAmplitudes[i] = 0.1f;
            }

            animationRunnable = () -> {
                if (isAnimating) {
                    updateWave();
                    invalidate();
                    handler.postDelayed(animationRunnable, 40); // 25fps
                }
            };
        }

        public void setAudioLevel(float level) {
            this.audioLevel = Math.max(0.05f, Math.min(1.0f, level));
        }

        public void startAnimation() {
            isAnimating = true;
            handler.post(animationRunnable);
        }

        public void stopAnimation() {
            isAnimating = false;
            handler.removeCallbacks(animationRunnable);
        }

        private void updateWave() {
            animOffset++;
            // Shift amplitudes and add new value based on audio level
            for (int i = waveAmplitudes.length - 1; i > 0; i--) {
                waveAmplitudes[i] = waveAmplitudes[i - 1];
            }
            // New amplitude based on audio level with some variation
            float variation = (float) (Math.sin(animOffset * 0.3) * 0.1);
            waveAmplitudes[0] = audioLevel + variation;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int centerY = height / 2;

            // Create gradient for the wave
            LinearGradient gradient = new LinearGradient(
                    0, 0, width, 0,
                    new int[] { 0xFFFFFFFF, 0xFFCCCCCC, 0xFFFFFFFF },
                    null, Shader.TileMode.CLAMP);
            wavePaint.setShader(gradient);

            wavePath.reset();

            float segmentWidth = (float) width / (waveAmplitudes.length - 1);
            float maxAmplitude = height * 0.4f;

            wavePath.moveTo(0, centerY);

            for (int i = 0; i < waveAmplitudes.length; i++) {
                float x = i * segmentWidth;
                float amp = waveAmplitudes[i] * maxAmplitude;

                // Create smooth sine-like wave
                float phase = (float) Math.sin((i + animOffset * 0.5) * 0.5);
                float y = centerY + phase * amp;

                if (i == 0) {
                    wavePath.moveTo(x, y);
                } else {
                    // Smooth curve
                    float prevX = (i - 1) * segmentWidth;
                    float ctrlX = (prevX + x) / 2;
                    wavePath.quadTo(ctrlX, y, x, y);
                }
            }

            canvas.drawPath(wavePath, wavePaint);

            // Draw mirrored wave for symmetry
            wavePath.reset();
            for (int i = 0; i < waveAmplitudes.length; i++) {
                float x = i * segmentWidth;
                float amp = waveAmplitudes[i] * maxAmplitude;
                float phase = (float) Math.sin((i + animOffset * 0.5) * 0.5);
                float y = centerY - phase * amp;

                if (i == 0) {
                    wavePath.moveTo(x, y);
                } else {
                    float prevX = (i - 1) * segmentWidth;
                    float ctrlX = (prevX + x) / 2;
                    wavePath.quadTo(ctrlX, y, x, y);
                }
            }

            wavePaint.setAlpha(150);
            canvas.drawPath(wavePath, wavePaint);
            wavePaint.setAlpha(255);
        }
    }
}
