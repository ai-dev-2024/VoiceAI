package com.voiceai.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.view.View;

/**
 * SettingsActivity - Modern tile-based settings UI for VoiceAI (iPhone-like)
 */
public class SettingsActivity extends Activity {

        public static final String APP_VERSION = "1.2.0";

        public static final String PREFS_NAME = "VoiceAIPrefs";
        public static final String PREF_TIME_LIMIT = "transcription_time_limit";
        public static final String PREF_AUTO_SILENCE = "auto_stop_on_silence";
        public static final String PREF_PERSONAL_DICT = "personal_dictionary";

        private SharedPreferences prefs;
        private EditText dictInput;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                createUI();
        }

        private void createUI() {
                ScrollView scroll = new ScrollView(this);
                // Clean white background like shadcn/GitHub
                scroll.setBackgroundColor(0xFFFAFAFA); // Very light gray

                LinearLayout root = new LinearLayout(this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(24, 48, 24, 40);

                // Header with back button
                LinearLayout header = new LinearLayout(this);
                header.setOrientation(LinearLayout.VERTICAL);
                header.setPadding(0, 0, 0, 24);

                TextView backBtn = new TextView(this);
                backBtn.setText("←");
                backBtn.setTextSize(24);
                backBtn.setTextColor(0xFF24292F);
                backBtn.setPadding(0, 0, 0, 16);
                backBtn.setOnClickListener(v -> finish());
                header.addView(backBtn);

                // Title row with version
                LinearLayout titleRow = new LinearLayout(this);
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView title = new TextView(this);
                title.setText("Settings");
                title.setTextSize(28);
                title.setTextColor(0xFF24292F);
                title.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
                titleRow.addView(title);

                TextView versionBadge = new TextView(this);
                versionBadge.setText("v" + APP_VERSION);
                versionBadge.setTextSize(12);
                versionBadge.setTextColor(0xFF656D76);
                versionBadge.setPadding(16, 6, 16, 6);
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setColor(0xFFEFF1F3);
                badgeBg.setCornerRadius(16f);
                badgeBg.setStroke(1, 0xFFD1D9E0);
                versionBadge.setBackground(badgeBg);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                badgeParams.setMargins(12, 0, 0, 0);
                versionBadge.setLayoutParams(badgeParams);
                titleRow.addView(versionBadge);

                header.addView(titleRow);
                root.addView(header);

                // RECORDING Section
                root.addView(createSectionTitle("RECORDING"));

                LinearLayout recordingCard = createCard();
                recordingCard.addView(createToggleTile(
                                "30-Second Limit",
                                "Auto-stop after 30 seconds",
                                PREF_TIME_LIMIT,
                                true));
                recordingCard.addView(createDivider());
                recordingCard.addView(createToggleTile(
                                "Auto-stop on Silence",
                                "Stop when silence detected (~2s)",
                                PREF_AUTO_SILENCE,
                                true));
                root.addView(recordingCard);

                // AI Settings Section
                root.addView(createSectionTitle("AI POST-PROCESSING"));

                LinearLayout aiCard = createCard();

                TextView aiHint = new TextView(this);
                aiHint.setText("Enter Groq API key for Wispr Flow-style AI formatting");
                aiHint.setTextSize(13);
                aiHint.setTextColor(0xFF656D76);
                aiHint.setPadding(16, 16, 16, 8);
                aiCard.addView(aiHint);

                // One-click Get API Key button
                TextView getKeyBtn = new TextView(this);
                getKeyBtn.setText("→ Get Free API Key");
                getKeyBtn.setTextSize(14);
                getKeyBtn.setTextColor(0xFF2563EB); // Blue link color
                getKeyBtn.setPadding(16, 8, 16, 16);
                getKeyBtn.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(android.net.Uri.parse("https://console.groq.com/keys"));
                        startActivity(intent);
                });
                aiCard.addView(getKeyBtn);

                EditText apiKeyInput = new EditText(this);
                apiKeyInput.setHint("gsk_xxxxx... (leave empty for offline)");
                apiKeyInput.setTextColor(0xFF24292F); // Dark text
                apiKeyInput.setHintTextColor(0xFFADB5BD); // Gray hint
                apiKeyInput.setTextSize(14);
                apiKeyInput.setPadding(16, 16, 16, 16);
                apiKeyInput.setSingleLine(true);
                apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                apiKeyInput.setBackground(null);
                apiKeyInput.setText(prefs.getString("groq_api_key", ""));

                apiKeyInput.addTextChangedListener(new android.text.TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        public void afterTextChanged(android.text.Editable s) {
                                prefs.edit().putString("groq_api_key", s.toString()).apply();
                        }
                });
                aiCard.addView(apiKeyInput);
                root.addView(aiCard);

                // Offline LLM Section
                root.addView(createSectionTitle("OFFLINE PROCESSING"));

                LinearLayout offlineCard = createCard();

                // Toggle for offline processing
                offlineCard.addView(createToggleTile(
                                "Use Offline LLM",
                                "Qwen3 0.6B model for offline post-processing",
                                "offline_llm_enabled",
                                false));
                offlineCard.addView(createDivider());

                // Model info and download
                TextView modelInfo = new TextView(this);
                modelInfo.setText("Download ~400MB model for offline AI formatting. Works without internet.");
                modelInfo.setTextSize(13);
                modelInfo.setTextColor(0xFF656D76);
                modelInfo.setPadding(16, 16, 16, 8);
                offlineCard.addView(modelInfo);

                // Download button
                TextView downloadBtn = new TextView(this);
                downloadBtn.setText("↓ Download Offline Model (~400MB)");
                downloadBtn.setTextSize(14);
                downloadBtn.setTextColor(0xFF2563EB);
                downloadBtn.setPadding(16, 12, 16, 16);
                downloadBtn.setOnClickListener(v -> {
                        android.widget.Toast.makeText(this, "Model download coming soon!",
                                        android.widget.Toast.LENGTH_SHORT).show();
                });
                offlineCard.addView(downloadBtn);

                root.addView(offlineCard);

                // Personal Dictionary Section
                root.addView(createSectionTitle("Personal Dictionary"));

                LinearLayout dictCard = createCard();

                TextView dictHint = new TextView(this);
                dictHint.setText(
                                "Add words separated by commas. These will be preserved exactly as typed during transcription.");
                dictHint.setTextSize(13);
                dictHint.setTextColor(0xFF656D76); // Muted gray
                dictHint.setPadding(16, 16, 16, 8);
                dictCard.addView(dictHint);

                dictInput = new EditText(this);
                dictInput.setHint("Groq, ChatGPT, OpenAI...");
                dictInput.setTextColor(0xFF24292F); // Dark text
                dictInput.setHintTextColor(0xFFADB5BD); // Gray hint
                dictInput.setTextSize(15);
                dictInput.setPadding(16, 16, 16, 16);
                dictInput.setMinLines(3);
                dictInput.setMaxLines(6);
                dictInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                dictInput.setBackground(null);
                dictInput.setText(prefs.getString(PREF_PERSONAL_DICT,
                                "Groq, Gemini, ChatGPT, OpenAI, Claude, Anthropic, Llama, Mistral, Qwen"));

                // Auto-save on text change
                dictInput.addTextChangedListener(new android.text.TextWatcher() {
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                        }

                        public void onTextChanged(CharSequence s, int start, int before, int count) {
                        }

                        public void afterTextChanged(android.text.Editable s) {
                                prefs.edit().putString(PREF_PERSONAL_DICT, s.toString()).apply();
                        }
                });
                dictCard.addView(dictInput);
                root.addView(dictCard);

                // About Section
                root.addView(createSectionTitle("About"));

                LinearLayout aboutCard = createCard();
                aboutCard.addView(createInfoTile("Version", APP_VERSION));
                aboutCard.addView(createDivider());
                aboutCard.addView(createInfoTile("Engine", "Parakeet TDT by NVIDIA"));
                aboutCard.addView(createDivider());
                aboutCard.addView(createInfoTile("Runtime", "ONNX Runtime by Microsoft"));
                aboutCard.addView(createDivider());

                // Add status indicator
                LinearLayout statusTile = new LinearLayout(this);
                statusTile.setOrientation(LinearLayout.HORIZONTAL);
                statusTile.setGravity(Gravity.CENTER_VERTICAL);
                statusTile.setPadding(16, 14, 16, 14);

                TextView statusLabel = new TextView(this);
                statusLabel.setText("Status");
                statusLabel.setTextSize(15);
                statusLabel.setTextColor(0xFF24292F); // Dark text
                LinearLayout.LayoutParams statusLabelParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                statusLabel.setLayoutParams(statusLabelParams);
                statusTile.addView(statusLabel);

                TextView statusValue = new TextView(this);
                statusValue.setText("✓ Ready");
                statusValue.setTextSize(15);
                statusValue.setTextColor(0xFF22C55E); // Green for ready
                statusTile.addView(statusValue);

                aboutCard.addView(statusTile);
                root.addView(aboutCard);

                scroll.addView(root);
                setContentView(scroll);
        }

        private TextView createSectionTitle(String text) {
                TextView tv = new TextView(this);
                tv.setText(text.toUpperCase());
                tv.setTextSize(12);
                tv.setTextColor(0xFF656D76); // Muted gray like shadcn
                tv.setPadding(4, 20, 4, 8);
                tv.setLetterSpacing(0.05f);
                tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                return tv;
        }

        private LinearLayout createCard() {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFFFFFFFF); // Pure white card
                bg.setCornerRadius(12f);
                bg.setStroke(1, 0xFFE5E7EB); // Light gray border
                card.setBackground(bg);
                card.setElevation(1f); // Subtle shadow

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 4, 0, 8);
                card.setLayoutParams(params);

                return card;
        }

        // Simple toggle card - just title and toggle, no subtitle (matches mockup)
        private LinearLayout createSimpleToggleCard(String title, String prefKey, boolean defaultValue) {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setGravity(Gravity.CENTER_VERTICAL);
                card.setPadding(20, 18, 20, 18);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0xFFFFFFFF);
                bg.setCornerRadius(12f);
                bg.setStroke(1, 0xFFE5E7EB);
                card.setBackground(bg);

                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 4, 0, 8);
                card.setLayoutParams(cardParams);

                // Title
                TextView titleTv = new TextView(this);
                titleTv.setText(title);
                titleTv.setTextSize(16);
                titleTv.setTextColor(0xFF24292F);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                titleTv.setLayoutParams(titleParams);
                card.addView(titleTv);

                // Toggle switch
                final boolean[] isOn = { prefs.getBoolean(prefKey, defaultValue) };

                FrameLayout toggleContainer = new FrameLayout(this);
                toggleContainer.setLayoutParams(new LinearLayout.LayoutParams(56, 32));

                GradientDrawable trackBg = new GradientDrawable();
                trackBg.setCornerRadius(16f);
                trackBg.setColor(isOn[0] ? 0xFF22C55E : 0xFFE5E5EA);
                toggleContainer.setBackground(trackBg);

                android.view.View thumb = new android.view.View(this);
                GradientDrawable thumbBg = new GradientDrawable();
                thumbBg.setShape(GradientDrawable.OVAL);
                thumbBg.setColor(0xFFFFFFFF);
                thumb.setBackground(thumbBg);
                thumb.setElevation(2f);

                FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(26, 26);
                thumbParams.gravity = Gravity.CENTER_VERTICAL;
                thumbParams.setMargins(isOn[0] ? 27 : 3, 3, 3, 3);
                thumb.setLayoutParams(thumbParams);
                toggleContainer.addView(thumb);

                toggleContainer.setOnClickListener(v -> {
                        isOn[0] = !isOn[0];
                        prefs.edit().putBoolean(prefKey, isOn[0]).apply();
                        ((GradientDrawable) toggleContainer.getBackground())
                                        .setColor(isOn[0] ? 0xFF22C55E : 0xFFE5E5EA);
                        FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(26, 26);
                        newParams.gravity = Gravity.CENTER_VERTICAL;
                        newParams.setMargins(isOn[0] ? 27 : 3, 3, 3, 3);
                        thumb.setLayoutParams(newParams);
                });
                card.addView(toggleContainer);

                return card;
        }

        private LinearLayout createToggleTile(String title, String subtitle, String prefKey, boolean defaultValue) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.HORIZONTAL);
                tile.setGravity(Gravity.CENTER_VERTICAL);
                tile.setPadding(24, 20, 24, 20);

                LinearLayout textPart = new LinearLayout(this);
                textPart.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textPart.setLayoutParams(textParams);

                TextView titleTv = new TextView(this);
                titleTv.setText(title);
                titleTv.setTextSize(16);
                titleTv.setTextColor(0xFF24292F); // Dark text for light theme
                titleTv.setTypeface(null, android.graphics.Typeface.NORMAL);
                textPart.addView(titleTv);

                TextView subtitleTv = new TextView(this);
                subtitleTv.setText(subtitle);
                subtitleTv.setTextSize(13);
                subtitleTv.setTextColor(0xFF656D76); // Muted gray
                textPart.addView(subtitleTv);

                tile.addView(textPart);

                // Custom iOS-style toggle switch
                final boolean[] isOn = { prefs.getBoolean(prefKey, defaultValue) };

                FrameLayout toggleContainer = new FrameLayout(this);
                toggleContainer.setLayoutParams(new LinearLayout.LayoutParams(100, 50));

                GradientDrawable trackBg = new GradientDrawable();
                trackBg.setCornerRadius(25f);
                trackBg.setColor(isOn[0] ? 0xFF22C55E : 0xFFE5E7EB); // Green when on, light gray when off
                toggleContainer.setBackground(trackBg);

                // Thumb (white circle)
                View thumb = new View(this);
                GradientDrawable thumbBg = new GradientDrawable();
                thumbBg.setShape(GradientDrawable.OVAL);
                thumbBg.setColor(Color.WHITE);
                thumb.setBackground(thumbBg);
                thumb.setElevation(4f);
                FrameLayout.LayoutParams thumbParams = new FrameLayout.LayoutParams(42, 42);
                thumbParams.gravity = Gravity.CENTER_VERTICAL;
                thumbParams.setMargins(isOn[0] ? 54 : 4, 4, 4, 4);
                thumb.setLayoutParams(thumbParams);
                toggleContainer.addView(thumb);

                // Click handler
                toggleContainer.setClickable(true);
                toggleContainer.setFocusable(true);
                toggleContainer.setOnClickListener(v -> {
                        isOn[0] = !isOn[0];
                        prefs.edit().putBoolean(prefKey, isOn[0]).apply();

                        // Update visual
                        ((GradientDrawable) toggleContainer.getBackground())
                                        .setColor(isOn[0] ? 0xFF22C55E : 0xFFE5E7EB);
                        FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(42, 42);
                        newParams.gravity = Gravity.CENTER_VERTICAL;
                        newParams.setMargins(isOn[0] ? 54 : 4, 4, 4, 4);
                        thumb.setLayoutParams(newParams);
                });

                tile.addView(toggleContainer);

                return tile;
        }

        private LinearLayout createActionTile(String title, String subtitle, Runnable action) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.HORIZONTAL);
                tile.setGravity(Gravity.CENTER_VERTICAL);
                tile.setPadding(16, 14, 16, 14);
                tile.setClickable(true);
                tile.setFocusable(true);
                tile.setOnClickListener(v -> action.run());

                LinearLayout textPart = new LinearLayout(this);
                textPart.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textPart.setLayoutParams(textParams);

                TextView titleTv = new TextView(this);
                titleTv.setText(title);
                titleTv.setTextSize(15);
                titleTv.setTextColor(0xFF24292F); // Dark text
                textPart.addView(titleTv);

                TextView subtitleTv = new TextView(this);
                subtitleTv.setText(subtitle);
                subtitleTv.setTextSize(13);
                subtitleTv.setTextColor(0xFF656D76); // Muted gray
                textPart.addView(subtitleTv);

                tile.addView(textPart);

                TextView arrow = new TextView(this);
                arrow.setText("›");
                arrow.setTextSize(18);
                arrow.setTextColor(0xFFD1D9E0); // Light gray arrow
                tile.addView(arrow);

                return tile;
        }

        private LinearLayout createInfoTile(String label, String value) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.HORIZONTAL);
                tile.setGravity(Gravity.CENTER_VERTICAL);
                tile.setPadding(16, 14, 16, 14);

                TextView labelTv = new TextView(this);
                labelTv.setText(label);
                labelTv.setTextSize(15);
                labelTv.setTextColor(0xFF24292F); // Dark text
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                labelTv.setLayoutParams(labelParams);
                tile.addView(labelTv);

                TextView valueTv = new TextView(this);
                valueTv.setText(value);
                valueTv.setTextSize(15);
                valueTv.setTextColor(0xFF656D76); // Muted gray
                tile.addView(valueTv);

                return tile;
        }

        private android.view.View createDivider() {
                android.view.View divider = new android.view.View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(16, 0, 0, 0);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(0xFFE5E7EB); // Light gray divider
                return divider;
        }

        /**
         * Get personal dictionary words from SharedPreferences.
         * Can be called from any context (services, activities).
         */
        public static String[] getDictionaryWords(Context context) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String dict = prefs.getString(PREF_PERSONAL_DICT, "");
                if (dict == null || dict.trim().isEmpty()) {
                        return new String[0];
                }
                String[] words = dict.split(",");
                String[] trimmed = new String[words.length];
                for (int i = 0; i < words.length; i++) {
                        trimmed[i] = words[i].trim();
                }
                return trimmed;
        }

        /**
         * Check if auto-stop on silence is enabled.
         */
        public static boolean isAutoSilenceEnabled(Context context) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getBoolean(PREF_AUTO_SILENCE, true);
        }
}
