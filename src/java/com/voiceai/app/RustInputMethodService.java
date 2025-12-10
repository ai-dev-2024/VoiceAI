package com.voiceai.app;

import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.AttributeSet;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;

public class RustInputMethodService extends InputMethodService {

    private static final String TAG = "VoiceAI";

    static {
        try {
            System.loadLibrary("android_transcribe_app");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }

    private TextView statusView;
    private ProgressBar progressBar;
    private SoundWaveView waveformView; // Changed to responsive waveform
    private Handler mainHandler;
    private boolean isRecording = false;
    private boolean autoStarted = false;
    private boolean modelReady = false;
    private String lastStatus = "Initializing...";

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        Log.d(TAG, "VoiceAI Service onCreate");
        try {
            initNative(this);
        } catch (Exception e) {
            Log.e(TAG, "Error in initNative", e);
        }
    }

    @Override
    public View onCreateInputView() {
        Log.d(TAG, "onCreateInputView");
        try {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            layout.setPadding(48, 40, 48, 40);
            layout.setGravity(Gravity.CENTER);

            // Dark transparent background (50% black) - matches RecognizeActivity
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x80000000); // 50% black
            bg.setCornerRadius(32f);
            layout.setBackground(bg);
            layout.setElevation(16f);

            // Make entire layout tappable to stop recording
            layout.setClickable(true);
            layout.setOnClickListener(v -> {
                if (isRecording) {
                    stopRecording();
                    stopWaveformAnimation();
                    isRecording = false;
                    if (statusView != null)
                        statusView.setText("Processing...");
                }
            });

            // Status text
            statusView = new TextView(this);
            statusView.setText("Listening...");
            statusView.setTextSize(18);
            statusView.setTextColor(0xFFFFFFFF);
            statusView.setGravity(Gravity.CENTER);
            statusView.setPadding(0, 0, 0, 16);
            layout.addView(statusView);

            // Responsive waveform view (like SwiftKey/RecognizeActivity)
            waveformView = new SoundWaveView(this);
            LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(
                    dpToPx(260), dpToPx(80));
            waveParams.gravity = Gravity.CENTER;
            waveParams.setMargins(0, 8, 0, 16);
            waveformView.setLayoutParams(waveParams);
            layout.addView(waveformView);

            // Progress Bar (during model loading only)
            progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            layout.addView(progressBar);

            // NO record button - the entire layout is tappable via
            // layout.setOnClickListener above

            // Hint text
            TextView hint = new TextView(this);
            hint.setText("Tap anywhere to stop");
            hint.setTextSize(14);
            hint.setTextColor(0xCCFFFFFF);
            hint.setGravity(Gravity.CENTER);
            hint.setPadding(0, 0, 0, 0);
            layout.addView(hint);

            updateUiState();

            return layout;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateInputView", e);
            TextView errorView = new TextView(this);
            errorView.setText("Error loading VoiceAI: " + e.getMessage());
            return errorView;
        }
    }

    @Override
    public void onWindowShown() {
        super.onWindowShown();
        Log.d(TAG, "onWindowShown - auto-starting recording if ready");

        // Auto-start recording when IME window shows (FUTO-like behavior)
        if (modelReady && !autoStarted && !isRecording) {
            mainHandler.postDelayed(() -> {
                if (!isRecording && modelReady) {
                    Log.d(TAG, "Auto-starting recording");
                    autoStarted = true;
                    isRecording = true;
                    startRecording();
                    startWaveformAnimation();
                    if (statusView != null)
                        statusView.setText("Listening...");
                }
            }, 300);
        }
    }

    @Override
    public void onWindowHidden() {
        super.onWindowHidden();
        autoStarted = false;
    }

    private void startWaveformAnimation() {
        if (waveformView != null) {
            waveformView.startAnimation();
        }
    }

    private void stopWaveformAnimation() {
        if (waveformView != null) {
            waveformView.stopAnimation();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopWaveformAnimation();
        cleanupNative();
    }

    // Native methods
    private native void initNative(RustInputMethodService service);

    private native void cleanupNative();

    private native void startRecording();

    private native void stopRecording();

    // Called from Rust
    public void onStatusUpdate(String status) {
        mainHandler.post(() -> {
            Log.d(TAG, "Status: " + status);
            lastStatus = status;
            updateUiState();

            // Handle 30s Limit
            if (status.contains("Listening") && isRecording) {
                SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
                if (prefs.getBoolean(SettingsActivity.PREF_TIME_LIMIT, true)) {
                    mainHandler.removeCallbacks(stopRunnable);
                    mainHandler.postDelayed(stopRunnable, 30000);
                    Log.d(TAG, "30s Limit Enabled: Stopping in 30s");
                }
                lastAudioTime = System.currentTimeMillis();
            }
        });
    }

    // Called from Rust - audio level for responsive waveform
    public void onAudioLevel(float level) {
        mainHandler.post(() -> {
            if (waveformView != null) {
                waveformView.setAudioLevel(level);
            }

            // Handle Auto-Silence
            if (isRecording && SettingsActivity.isAutoSilenceEnabled(this)) {
                if (level > 0.1f) { // Arbitrary threshold, adjust as needed
                    lastAudioTime = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - lastAudioTime > SILENCE_THRESHOLD_MS) {
                    Log.d(TAG, "Silence detected for 2s. Stopping.");
                    stopRecording();
                }
            }
        });
    }

    private void updateUiState() {
        if (statusView != null)
            statusView.setText(lastStatus);

        if (lastStatus.contains("Ready") || lastStatus.contains("Listening")) {
            modelReady = true;
            if (progressBar != null)
                progressBar.setVisibility(View.GONE);
            // Auto-start recording if we just became ready and window is shown
            if (!autoStarted && !isRecording) {
                mainHandler.postDelayed(() -> {
                    if (!isRecording && modelReady && !autoStarted) {
                        Log.d(TAG, "Model ready - auto-starting recording");
                        autoStarted = true;
                        isRecording = true;
                        startRecording();
                        startWaveformAnimation();
                        if (statusView != null)
                            statusView.setText("Listening...");
                    }
                }, 200);
            }
        } else if (lastStatus.contains("Initializing") || lastStatus.contains("Loading")) {
            modelReady = false;
            if (progressBar != null)
                progressBar.setVisibility(View.VISIBLE);
        }
    }

    private Runnable stopRunnable = this::stopRecording;
    private long lastAudioTime = 0;
    private static final long SILENCE_THRESHOLD_MS = 2000;

    // Called from Rust - this is where we'd add post-processing
    // Called from Rust - text is already post-processed by Qwen/Rust
    public void onTextTranscribed(String text) {
        mainHandler.post(() -> {
            Log.d(TAG, "onTextTranscribed: Committing text: '" + text + "'");
            // Text effectively comes from Qwen (or regex fallback) in Rust now.
            // No need for Java-side postProcess() here.

            android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                boolean committed = ic.commitText(text + " ", 1);
                Log.d(TAG, "commitText result: " + committed);
            } else {
                Log.e(TAG, "InputConnection is NULL! Cannot commit text.");
            }
            stopWaveformAnimation();
            isRecording = false;
            if (statusView != null)
                statusView.setText("Done!");

            // Switch back to previous keyboard after a short delay
            mainHandler.postDelayed(() -> switchToPreviousKeyboard(), 200);
        });
    }

    /**
     * Switch back to the previous input method (HeliBoard, etc.)
     */
    private void switchToPreviousKeyboard() {
        Log.d(TAG, "Switching to previous keyboard");
        autoStarted = false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                switchToPreviousInputMethod();
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                IBinder token = getWindow().getWindow().getAttributes().token;
                imm.switchToLastInputMethod(token);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error switching to previous keyboard", e);
        }
    }

    // Post-processing for custom words, dictionary, and numbers
    private String postProcess(String text) {
        String result = text;

        // Apply personal dictionary corrections first
        // Dictionary words like "@grok" will replace "at grok"
        String[] dictWords = SettingsActivity.getDictionaryWords(this);
        for (String word : dictWords) {
            if (word.isEmpty())
                continue;

            // Handle @-prefixed words (e.g., "@grok" matches "at grok")
            if (word.startsWith("@")) {
                String base = word.substring(1).toLowerCase();
                result = result.replaceAll("(?i)\\bat\\s+" + base + "\\b", word);
            }

            // Case-insensitive word replacement for exact matches
            result = result.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(word.toLowerCase()) + "\\b", word);
        }

        // Convert number words to digits
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

        // Percentage formatting: "10 percent" -> "10%"
        result = result.replaceAll("(?i)(\\d+)\\s*percent", "$1%");

        // Currency formatting: "30 dollars" -> "$30"
        result = result.replaceAll("(?i)(\\d+)\\s*dollars?", "\\$$1");
        result = result.replaceAll("(?i)(\\d+)\\s*usd", "\\$$1 USD");

        // Common transcription corrections
        result = result.replaceAll("(?i)\\bgrok\\b", "Groq");
        result = result.replaceAll("(?i)\\bgemni\\b", "Gemini");
        result = result.replaceAll("(?i)claude ai", "Claude AI");
        result = result.replaceAll("(?i)chat gpt", "ChatGPT");
        result = result.replaceAll("(?i)open ai", "OpenAI");

        return result;
    }

    // Sound Wave View - Responsive to actual audio levels (same as
    // RecognizeActivity)
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
            wavePaint.setColor(0xFFFFFFFF);

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
            for (int i = waveAmplitudes.length - 1; i > 0; i--) {
                waveAmplitudes[i] = waveAmplitudes[i - 1];
            }
            float variation = (float) (Math.sin(animOffset * 0.3) * 0.1);
            waveAmplitudes[0] = audioLevel + variation;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int width = getWidth();
            int height = getHeight();
            int centerY = height / 2;

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
                float phase = (float) Math.sin((i + animOffset * 0.5) * 0.5);
                float y = centerY + phase * amp;

                if (i == 0) {
                    wavePath.moveTo(x, y);
                } else {
                    float prevX = (i - 1) * segmentWidth;
                    float ctrlX = (prevX + x) / 2;
                    wavePath.quadTo(ctrlX, y, x, y);
                }
            }

            canvas.drawPath(wavePath, wavePaint);

            // Mirrored wave for symmetry
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
