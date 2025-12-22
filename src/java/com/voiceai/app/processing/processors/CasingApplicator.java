package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CasingApplicator - Applies proper capitalization
 * 
 * Handles:
 * - Sentence capitalization (after . ! ?)
 * - Pronoun "I" capitalization
 * - Proper nouns (brands, tech names)
 * - Days of week and months
 * - Personal dictionary case preservation
 */
public class CasingApplicator implements TextProcessor {

    // Tech/brand proper nouns
    private static final String[][] PROPER_NOUNS = {
            { "openai", "OpenAI" }, { "chatgpt", "ChatGPT" }, { "gpt", "GPT" },
            { "youtube", "YouTube" }, { "tiktok", "TikTok" }, { "whatsapp", "WhatsApp" },
            { "linkedin", "LinkedIn" }, { "iphone", "iPhone" }, { "ipad", "iPad" },
            { "macbook", "MacBook" }, { "imac", "iMac" }, { "airpods", "AirPods" },
            { "microsoft", "Microsoft" }, { "google", "Google" }, { "amazon", "Amazon" },
            { "facebook", "Facebook" }, { "meta", "Meta" }, { "nvidia", "NVIDIA" },
            { "tesla", "Tesla" }, { "twitter", "Twitter" }, { "instagram", "Instagram" },
            { "spotify", "Spotify" }, { "netflix", "Netflix" }, { "uber", "Uber" },
            { "anthropic", "Anthropic" }, { "claude", "Claude" }, { "gemini", "Gemini" },
            { "groq", "Groq" }, { "llama", "Llama" }, { "mistral", "Mistral" },
            { "deepmind", "DeepMind" }, { "huggingface", "HuggingFace" },
            { "usa", "USA" }, { "uk", "UK" }, { "ai", "AI" }, { "api", "API" },
            { "sql", "SQL" }, { "html", "HTML" }, { "css", "CSS" }, { "json", "JSON" },
            { "http", "HTTP" }, { "https", "HTTPS" }, { "wifi", "WiFi" },
            { "ios", "iOS" }, { "macos", "macOS" }, { "linux", "Linux" },
            { "android", "Android" }, { "windows", "Windows" }
    };

    // Days of week
    private static final String[] DAYS = {
            "monday", "tuesday", "wednesday", "thursday",
            "friday", "saturday", "sunday"
    };

    // Months
    private static final String[] MONTHS = {
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Apply sentence capitalization
        result = applySentenceCase(result);

        // Capitalize "I" pronoun
        result = capitalizeI(result);

        // Apply proper noun casing
        result = applyProperNouns(result);

        // Capitalize days and months
        result = capitalizeDaysMonths(result);

        // Apply personal dictionary casing (should be last to override)
        result = applyPersonalDictionary(result, context.getPersonalDictionary());

        return result;
    }

    private String applySentenceCase(String text) {
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (capitalizeNext && Character.isLetter(c)) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }

            // Capitalize after sentence-ending punctuation
            if (c == '.' || c == '!' || c == '?') {
                capitalizeNext = true;
            }
        }

        return result.toString();
    }

    private String capitalizeI(String text) {
        String result = text;

        // "i" as standalone word
        result = result.replaceAll("(?<=\\s)i(?=\\s|'|$)", "I");
        result = result.replaceAll("^i(?=\\s|')", "I");
        result = result.replaceAll("(?<=[.!?]\\s)i(?=\\s|')", "I");

        // "i'm", "i've", "i'll", "i'd"
        result = result.replaceAll("(?i)\\bi'm\\b", "I'm");
        result = result.replaceAll("(?i)\\bi've\\b", "I've");
        result = result.replaceAll("(?i)\\bi'll\\b", "I'll");
        result = result.replaceAll("(?i)\\bi'd\\b", "I'd");

        return result;
    }

    private String applyProperNouns(String text) {
        String result = text;

        for (String[] noun : PROPER_NOUNS) {
            result = result.replaceAll(
                    "(?i)\\b" + Pattern.quote(noun[0]) + "\\b",
                    Matcher.quoteReplacement(noun[1]));
        }

        return result;
    }

    private String capitalizeDaysMonths(String text) {
        String result = text;

        for (String day : DAYS) {
            String cap = day.substring(0, 1).toUpperCase() + day.substring(1);
            result = result.replaceAll("(?i)\\b" + day + "\\b", cap);
        }

        for (String month : MONTHS) {
            String cap = month.substring(0, 1).toUpperCase() + month.substring(1);
            result = result.replaceAll("(?i)\\b" + month + "\\b", cap);
        }

        return result;
    }

    private String applyPersonalDictionary(String text, Map<String, String> dictionary) {
        if (dictionary == null || dictionary.isEmpty()) {
            return text;
        }

        String result = text;

        for (Map.Entry<String, String> entry : dictionary.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + Pattern.quote(entry.getKey()) + "\\b",
                    Matcher.quoteReplacement(entry.getValue()));
        }

        return result;
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        return !context.isCasingEnabled();
    }
}
