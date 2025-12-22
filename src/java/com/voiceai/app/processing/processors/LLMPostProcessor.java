package com.voiceai.app.processing.processors;

import android.util.Log;
import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * LLMPostProcessor - Wispr Flow-style AI text formatting
 * 
 * Uses Groq API (free tier) for:
 * - Filler word removal ("um", "uh", "like")
 * - Grammar and punctuation fixes
 * - Natural text formatting
 * - Context-aware capitalization
 * 
 * Falls back to rule-based processing if API unavailable.
 */
public class LLMPostProcessor implements TextProcessor {

    private static final String TAG = "VoiceAI.LLM";

    // Groq API - Free tier available
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.1-8b-instant"; // Fast, good quality

    // API key from settings (can be null for offline mode)
    private String apiKey = null;

    // Timeout for API calls
    private static final int TIMEOUT_MS = 3000;

    public LLMPostProcessor() {
        // Default constructor - API key set via setApiKey()
    }

    public LLMPostProcessor(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Skip LLM if no API key or offline mode
        if (!hasApiKey()) {
            Log.d(TAG, "No API key, using rule-based fallback");
            return ruleBasedFallback(text);
        }

        try {
            String result = callGroqAPI(text);
            if (result != null && !result.isEmpty()) {
                Log.d(TAG, "LLM processing successful");
                return result;
            }
        } catch (Exception e) {
            Log.w(TAG, "LLM API error, using fallback: " + e.getMessage());
        }

        return ruleBasedFallback(text);
    }

    private String callGroqAPI(String text) throws Exception {
        // System prompt for Wispr Flow-style formatting
        String systemPrompt = "You are a text formatting assistant. Your job is to clean up voice dictation text. " +
                "Rules:\n" +
                "1. Remove filler words (um, uh, like, you know, basically, actually)\n" +
                "2. Fix grammar and punctuation\n" +
                "3. Convert spoken numbers to digits (twenty five → 25, one hundred → 100)\n" +
                "4. Keep the original meaning and tone\n" +
                "5. ONLY output the cleaned text, nothing else\n" +
                "6. If input is already clean, output it unchanged";

        // Build JSON request
        JSONObject message1 = new JSONObject();
        message1.put("role", "system");
        message1.put("content", systemPrompt);

        JSONObject message2 = new JSONObject();
        message2.put("role", "user");
        message2.put("content", text);

        JSONArray messages = new JSONArray();
        messages.put(message1);
        messages.put(message2);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", GROQ_MODEL);
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3); // Low temperature for consistent output
        requestBody.put("max_tokens", 500);

        // Make API call on background thread with timeout
        AtomicReference<String> result = new AtomicReference<>(null);
        AtomicReference<Exception> error = new AtomicReference<>(null);
        CountDownLatch latch = new CountDownLatch(1);

        new Thread(() -> {
            try {
                URL url = new URL(GROQ_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes("UTF-8"));
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                        result.set(message.getString("content").trim());
                    }
                } else {
                    Log.w(TAG, "API returned code: " + responseCode);
                }
                conn.disconnect();
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        }).start();

        // Wait for result with timeout
        if (!latch.await(TIMEOUT_MS + 500, TimeUnit.MILLISECONDS)) {
            throw new Exception("API timeout");
        }

        if (error.get() != null) {
            throw error.get();
        }

        return result.get();
    }

    /**
     * Rule-based fallback when LLM is unavailable
     */
    private String ruleBasedFallback(String text) {
        String result = text;

        // Remove common filler words
        result = result.replaceAll("(?i)\\b(um+|uh+|er+|ah+)\\b\\s*", "");
        result = result.replaceAll("(?i)\\b(you know|i mean|like|basically|actually|literally)\\b\\s*,?\\s*", "");

        // Clean up double spaces
        result = result.replaceAll("\\s+", " ").trim();

        return result;
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        // LLM processing is always attempted if enabled
        return false;
    }
}
