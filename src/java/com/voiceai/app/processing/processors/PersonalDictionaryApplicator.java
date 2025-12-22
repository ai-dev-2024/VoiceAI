package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PersonalDictionaryApplicator - Applies user's custom word replacements
 * 
 * FUTO-style personal dictionary:
 * - Preserves exact casing for brand names: "groq" → "Groq"
 * - Handles @mentions: "at Groq" → "@Groq"
 * - Multi-word replacements supported
 */
public class PersonalDictionaryApplicator implements TextProcessor {

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Map<String, String> dictionary = context.getPersonalDictionary();
        if (dictionary == null || dictionary.isEmpty()) {
            return text;
        }

        String result = text;

        // Sort by key length (longest first) for proper multi-word handling
        java.util.List<Map.Entry<String, String>> sorted = new java.util.ArrayList<>(dictionary.entrySet());
        sorted.sort((a, b) -> b.getKey().length() - a.getKey().length());

        for (Map.Entry<String, String> entry : sorted) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            // Handle @mentions: "at groq" → "@Groq"
            if (value.startsWith("@") && value.length() > 1) {
                String withoutAt = value.substring(1);
                result = result.replaceAll(
                        "(?i)\\bat\\s+" + Pattern.quote(withoutAt.toLowerCase()) + "\\b",
                        value);
            }

            // Standard word replacement with case preservation
            result = result.replaceAll(
                    "(?i)\\b" + Pattern.quote(key) + "\\b",
                    Matcher.quoteReplacement(value));
        }

        return result;
    }
}
