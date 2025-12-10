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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * SettingsActivity - Modern tile-based settings UI for VoiceAI (iPhone-like)
 */
public class SettingsActivity extends Activity {

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
                scroll.setBackgroundColor(0xFFF5F5F7); // Light gray like iOS

                LinearLayout root = new LinearLayout(this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setPadding(32, 48, 32, 48);

                // Header with back button and app icon
                LinearLayout header = new LinearLayout(this);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                header.setPadding(0, 0, 0, 24);

                TextView backBtn = new TextView(this);
                backBtn.setText("←");
                backBtn.setTextSize(24);
                backBtn.setTextColor(0xFF007AFF); // iOS blue
                backBtn.setPadding(0, 0, 16, 0);
                backBtn.setOnClickListener(v -> finish());
                header.addView(backBtn);

                // Add app icon
                TextView icon = new TextView(this);
                icon.setText("🎤");
                icon.setTextSize(28);
                icon.setPadding(0, 0, 12, 0);
                header.addView(icon);

                TextView title = new TextView(this);
                title.setText("VoiceAI Settings");
                title.setTextSize(28);
                title.setTextColor(0xFF1C1C1E);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                header.addView(title);

                // Add version badge
                TextView versionBadge = new TextView(this);
                versionBadge.setText("v1.0.2");
                versionBadge.setTextSize(14);
                versionBadge.setTextColor(Color.WHITE);
                versionBadge.setBackgroundColor(0xFF007AFF);
                versionBadge.setPadding(8, 4, 8, 4);
                GradientDrawable badgeBg = new GradientDrawable();
                badgeBg.setCornerRadius(12f);
                versionBadge.setBackground(badgeBg);
                LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                badgeParams.setMargins(12, 0, 0, 0);
                versionBadge.setLayoutParams(badgeParams);
                header.addView(versionBadge);

                root.addView(header);

                // Recording Settings Section
                root.addView(createSectionTitle("Recording"));

                LinearLayout recordingCard = createCard();
                recordingCard.addView(createToggleTile(
                                "⏱️ 30-Second Limit",
                                "Auto-stop after 30 seconds",
                                PREF_TIME_LIMIT,
                                true));
                recordingCard.addView(createDivider());
                recordingCard.addView(createToggleTile(
                                "🔇 Auto-stop on Silence",
                                "Stop when silence detected (~2s)",
                                PREF_AUTO_SILENCE,
                                true));
                root.addView(recordingCard);

                // Keyboard Setup Section
                root.addView(createSectionTitle("Keyboard Setup"));

                LinearLayout keyboardCard = createCard();
                keyboardCard.addView(createActionTile(
                                "⌨️ Enable VoiceAI Keyboard",
                                "Open keyboard settings",
                                () -> {
                                        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                                        startActivity(intent);
                                }));
                keyboardCard.addView(createDivider());
                keyboardCard.addView(createActionTile(
                                "🎤 Set as Voice Input",
                                "Select VoiceAI for dictation",
                                () -> {
                                        Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
                                        startActivity(intent);
                                }));
                root.addView(keyboardCard);

                // Personal Dictionary Section
                root.addView(createSectionTitle("Personal Dictionary"));

                LinearLayout dictCard = createCard();

                TextView dictHint = new TextView(this);
                dictHint.setText(
                                "Add words separated by commas. These will be preserved exactly as typed during transcription.");
                dictHint.setTextSize(13);
                dictHint.setTextColor(0xFF8E8E93);
                dictHint.setPadding(16, 16, 16, 8);
                dictCard.addView(dictHint);

                dictInput = new EditText(this);
                dictInput.setHint("Groq, ChatGPT, OpenAI...");
                dictInput.setTextColor(0xFF1C1C1E);
                dictInput.setHintTextColor(0xFFC7C7CC);
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
                aboutCard.addView(createInfoTile("Version", "1.0.0"));
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
                statusLabel.setTextSize(16);
                statusLabel.setTextColor(0xFF1C1C1E);
                LinearLayout.LayoutParams statusLabelParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                statusLabel.setLayoutParams(statusLabelParams);
                statusTile.addView(statusLabel);

                TextView statusValue = new TextView(this);
                statusValue.setText("✅ Ready");
                statusValue.setTextSize(16);
                statusValue.setTextColor(0xFF34C759); // Green for ready status
                statusTile.addView(statusValue);

                aboutCard.addView(statusTile);
                root.addView(aboutCard);

                scroll.addView(root);
                setContentView(scroll);
        }

        private TextView createSectionTitle(String text) {
                TextView tv = new TextView(this);
                tv.setText(text.toUpperCase());
                tv.setTextSize(13);
                tv.setTextColor(0xFF8E8E93);
                tv.setPadding(16, 24, 16, 8);
                tv.setLetterSpacing(0.05f);
                return tv;
        }

        private LinearLayout createCard() {
                LinearLayout card = new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.WHITE);
                bg.setCornerRadius(16f);
                card.setBackground(bg);
                card.setElevation(2f);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 4, 0, 8);
                card.setLayoutParams(params);

                return card;
        }

        private LinearLayout createToggleTile(String title, String subtitle, String prefKey, boolean defaultValue) {
                LinearLayout tile = new LinearLayout(this);
                tile.setOrientation(LinearLayout.HORIZONTAL);
                tile.setGravity(Gravity.CENTER_VERTICAL);
                tile.setPadding(16, 14, 16, 14);

                LinearLayout textPart = new LinearLayout(this);
                textPart.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                textPart.setLayoutParams(textParams);

                TextView titleTv = new TextView(this);
                titleTv.setText(title);
                titleTv.setTextSize(16);
                titleTv.setTextColor(0xFF1C1C1E);
                textPart.addView(titleTv);

                TextView subtitleTv = new TextView(this);
                subtitleTv.setText(subtitle);
                subtitleTv.setTextSize(13);
                subtitleTv.setTextColor(0xFF8E8E93);
                textPart.addView(subtitleTv);

                tile.addView(textPart);

                Switch toggle = new Switch(this);
                toggle.setChecked(prefs.getBoolean(prefKey, defaultValue));
                toggle.setOnCheckedChangeListener((btn, checked) -> prefs.edit().putBoolean(prefKey, checked).apply());
                tile.addView(toggle);

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
                titleTv.setTextSize(16);
                titleTv.setTextColor(0xFF1C1C1E);
                textPart.addView(titleTv);

                TextView subtitleTv = new TextView(this);
                subtitleTv.setText(subtitle);
                subtitleTv.setTextSize(13);
                subtitleTv.setTextColor(0xFF8E8E93);
                textPart.addView(subtitleTv);

                tile.addView(textPart);

                TextView arrow = new TextView(this);
                arrow.setText("›");
                arrow.setTextSize(20);
                arrow.setTextColor(0xFFC7C7CC);
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
                labelTv.setTextSize(16);
                labelTv.setTextColor(0xFF1C1C1E);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                labelTv.setLayoutParams(labelParams);
                tile.addView(labelTv);

                TextView valueTv = new TextView(this);
                valueTv.setText(value);
                valueTv.setTextSize(16);
                valueTv.setTextColor(0xFF8E8E93);
                tile.addView(valueTv);

                return tile;
        }

        private android.view.View createDivider() {
                android.view.View divider = new android.view.View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMargins(16, 0, 0, 0);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(0xFFE5E5EA);
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
