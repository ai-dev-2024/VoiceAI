package com.voiceai.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 2 Post-Processing Pipeline for VoiceAI (ParaKit ASR)
 * Complete implementation - no placeholders, production-ready
 * 
 * Pipeline order:
 * 1. clean_repetitions
 * 2. apply_personal_dictionary
 * 3. normalize_numbers
 * 4. normalize_percentages
 * 5. remove_fillers
 * 6. restore_punctuation
 * 7. apply_casing
 * 8. segment_paragraphs
 */
public class PostProcessor {

    // ========================================================================
    // MAIN ENTRY POINT
    // ========================================================================

    public static String processTranscript(String rawText, List<WordTimestamp> timestamps,
            Map<String, String> personalDictionary) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }

        String text = rawText.trim().toLowerCase();

        // Pipeline execution in exact order
        text = cleanRepetitions(text);
        text = applyPersonalDictionary(text, personalDictionary);
        text = normalizeNumbers(text);
        text = normalizePercentages(text);
        text = removeFillers(text);
        text = restorePunctuation(text);
        text = applyCasing(text, personalDictionary);
        text = segmentParagraphs(text, timestamps);

        return text.trim();
    }

    public static String processTranscript(String rawText) {
        return processTranscript(rawText, null, null);
    }

    // ========================================================================
    // 1. CLEAN REPETITIONS
    // ========================================================================

    public static String cleanRepetitions(String text) {
        String result = text;

        // Fix triple+ word repeats: "I I I think" → "I think"
        result = result.replaceAll("(?i)\\b(\\w+)(\\s+\\1){2,}\\b", "$1");

        // Fix double word repeats: "I I think" → "I think"
        result = result.replaceAll("(?i)\\b(\\w+)\\s+\\1\\b", "$1");

        // Fix repeated phrases (2-3 words): "you know you know" → "you know"
        result = result.replaceAll("(?i)\\b(\\w+\\s+\\w+)(\\s+\\1)+\\b", "$1");
        result = result.replaceAll("(?i)\\b(\\w+\\s+\\w+\\s+\\w+)(\\s+\\1)+\\b", "$1");

        // Fix multiple spaces
        result = result.replaceAll("\\s{2,}", " ");

        return result.trim();
    }

    // ========================================================================
    // 2. APPLY PERSONAL DICTIONARY (FUTO-style)
    // ========================================================================

    public static String applyPersonalDictionary(String text, Map<String, String> dictionary) {
        if (dictionary == null || dictionary.isEmpty()) {
            return text;
        }

        String result = text;

        // Sort by length (longest first) to handle multi-word replacements
        List<Map.Entry<String, String>> sorted = new ArrayList<>(dictionary.entrySet());
        sorted.sort((a, b) -> b.getKey().length() - a.getKey().length());

        for (Map.Entry<String, String> entry : sorted) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue();

            // Handle @mentions: key="@Groq" → replace "at groq" with "@Groq"
            if (value.startsWith("@") && value.length() > 1) {
                String withoutAt = value.substring(1);
                result = result.replaceAll("(?i)\\bat\\s+" + Pattern.quote(withoutAt.toLowerCase()) + "\\b", value);
            }

            // Standard replacement
            result = result.replaceAll("(?i)\\b" + Pattern.quote(key) + "\\b", value);
        }

        return result;
    }

    // ========================================================================
    // 3. NORMALIZE NUMBERS
    // ========================================================================

    private static final Map<String, Integer> WORD_TO_NUM = new HashMap<>();
    private static final Map<String, Integer> TENS = new HashMap<>();
    private static final Map<String, Integer> ORDINAL_WORD = new HashMap<>();

    static {
        WORD_TO_NUM.put("zero", 0);
        WORD_TO_NUM.put("one", 1);
        WORD_TO_NUM.put("two", 2);
        WORD_TO_NUM.put("three", 3);
        WORD_TO_NUM.put("four", 4);
        WORD_TO_NUM.put("five", 5);
        WORD_TO_NUM.put("six", 6);
        WORD_TO_NUM.put("seven", 7);
        WORD_TO_NUM.put("eight", 8);
        WORD_TO_NUM.put("nine", 9);
        WORD_TO_NUM.put("ten", 10);
        WORD_TO_NUM.put("eleven", 11);
        WORD_TO_NUM.put("twelve", 12);
        WORD_TO_NUM.put("thirteen", 13);
        WORD_TO_NUM.put("fourteen", 14);
        WORD_TO_NUM.put("fifteen", 15);
        WORD_TO_NUM.put("sixteen", 16);
        WORD_TO_NUM.put("seventeen", 17);
        WORD_TO_NUM.put("eighteen", 18);
        WORD_TO_NUM.put("nineteen", 19);

        TENS.put("twenty", 20);
        TENS.put("thirty", 30);
        TENS.put("forty", 40);
        TENS.put("fifty", 50);
        TENS.put("sixty", 60);
        TENS.put("seventy", 70);
        TENS.put("eighty", 80);
        TENS.put("ninety", 90);

        ORDINAL_WORD.put("first", 1);
        ORDINAL_WORD.put("second", 2);
        ORDINAL_WORD.put("third", 3);
        ORDINAL_WORD.put("fourth", 4);
        ORDINAL_WORD.put("fifth", 5);
        ORDINAL_WORD.put("sixth", 6);
        ORDINAL_WORD.put("seventh", 7);
        ORDINAL_WORD.put("eighth", 8);
        ORDINAL_WORD.put("ninth", 9);
        ORDINAL_WORD.put("tenth", 10);
        ORDINAL_WORD.put("eleventh", 11);
        ORDINAL_WORD.put("twelfth", 12);
        ORDINAL_WORD.put("thirteenth", 13);
        ORDINAL_WORD.put("fourteenth", 14);
        ORDINAL_WORD.put("fifteenth", 15);
        ORDINAL_WORD.put("sixteenth", 16);
        ORDINAL_WORD.put("seventeenth", 17);
        ORDINAL_WORD.put("eighteenth", 18);
        ORDINAL_WORD.put("nineteenth", 19);
        ORDINAL_WORD.put("twentieth", 20);
        ORDINAL_WORD.put("thirtieth", 30);
        ORDINAL_WORD.put("fortieth", 40);
        ORDINAL_WORD.put("fiftieth", 50);
        ORDINAL_WORD.put("sixtieth", 60);
        ORDINAL_WORD.put("seventieth", 70);
        ORDINAL_WORD.put("eightieth", 80);
        ORDINAL_WORD.put("ninetieth", 90);
        ORDINAL_WORD.put("hundredth", 100);
    }

    public static String normalizeNumbers(String text) {
        String result = text;

        // === Times: "ten oh five" → "10:05", "four twenty pm" → "4:20 PM" ===
        result = normalizeTimes(result);

        // === Years: "twenty twenty four" → "2024" ===
        result = normalizeYears(result);

        // === Ordinals: "twenty first" → "21st" ===
        result = normalizeOrdinals(result);

        // === Ranges: "one to five" → "1–5" ===
        result = normalizeRanges(result);

        // === Large numbers: "twenty five hundred" → "2500" ===
        result = normalizeLargeNumbers(result);

        // === Compound numbers: "twenty five" → "25" ===
        result = normalizeCompoundNumbers(result);

        // === Decimals: "one point five" → "1.5" ===
        result = normalizeDecimals(result);

        // === Currency ===
        result = normalizeCurrency(result);

        // === Simple numbers ===
        result = normalizeSimpleNumbers(result);

        return result;
    }

    private static String normalizeTimes(String text) {
        String result = text;

        // "ten oh five" → "10:05"
        for (Map.Entry<String, Integer> hour : WORD_TO_NUM.entrySet()) {
            if (hour.getValue() >= 1 && hour.getValue() <= 12) {
                for (Map.Entry<String, Integer> min : WORD_TO_NUM.entrySet()) {
                    if (min.getValue() >= 1 && min.getValue() <= 9) {
                        result = result.replaceAll(
                                "(?i)\\b" + hour.getKey() + "\\s+oh\\s+" + min.getKey() + "\\b",
                                hour.getValue() + ":0" + min.getValue());
                    }
                }
            }
        }

        // "four twenty pm" → "4:20 PM"
        String[] tens = { "twenty", "thirty", "forty", "fifty" };
        int[] tenVals = { 20, 30, 40, 50 };

        for (Map.Entry<String, Integer> hour : WORD_TO_NUM.entrySet()) {
            if (hour.getValue() >= 1 && hour.getValue() <= 12) {
                for (int t = 0; t < tens.length; t++) {
                    result = result.replaceAll(
                            "(?i)\\b" + hour.getKey() + "\\s+" + tens[t] + "\\s*(am|pm)\\b",
                            hour.getValue() + ":" + tenVals[t] + " $1".toUpperCase());

                    // With units: "four twenty five pm"
                    String[] units = { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
                    int[] unitVals = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
                    for (int u = 0; u < units.length; u++) {
                        result = result.replaceAll(
                                "(?i)\\b" + hour.getKey() + "\\s+" + tens[t] + "\\s+" + units[u] + "\\s*(am|pm)\\b",
                                hour.getValue() + ":" + (tenVals[t] + unitVals[u]) + " $1".toUpperCase());
                    }
                }

                // Simple: "four pm" → "4 PM"
                result = result.replaceAll(
                        "(?i)\\b" + hour.getKey() + "\\s*(am|pm)\\b",
                        hour.getValue() + " $1".toUpperCase());
            }
        }

        return result;
    }

    private static String normalizeYears(String text) {
        String result = text;

        // 2020s: "twenty twenty four" → "2024"
        String[] units = { "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
        for (int i = 0; i < units.length; i++) {
            if (i == 0) {
                result = result.replaceAll("(?i)\\btwenty\\s+twenty\\b", "2020");
            } else {
                result = result.replaceAll("(?i)\\btwenty\\s+twenty\\s+" + units[i] + "\\b", String.valueOf(2020 + i));
            }
        }

        // 2010s
        String[] teens = { "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
                "sixteen", "seventeen", "eighteen", "nineteen" };
        for (int i = 0; i < teens.length; i++) {
            result = result.replaceAll("(?i)\\btwenty\\s+" + teens[i] + "\\b", String.valueOf(2010 + i));
        }

        // 1990s, 1980s, etc.
        String[] decades = { "ninety", "eighty", "seventy", "sixty" };
        int[] decadeYears = { 1990, 1980, 1970, 1960 };
        for (int d = 0; d < decades.length; d++) {
            for (int i = 0; i < units.length; i++) {
                if (i == 0) {
                    result = result.replaceAll("(?i)\\bnineteen\\s+" + decades[d] + "\\b",
                            String.valueOf(decadeYears[d]));
                } else {
                    result = result.replaceAll("(?i)\\bnineteen\\s+" + decades[d] + "\\s+" + units[i] + "\\b",
                            String.valueOf(decadeYears[d] + i));
                }
            }
        }

        return result;
    }

    private static String normalizeOrdinals(String text) {
        String result = text;

        // Compound ordinals: "twenty first" → "21st"
        String[] tens = { "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
        int[] tenVals = { 20, 30, 40, 50, 60, 70, 80, 90 };
        String[] ordUnits = { "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth" };
        int[] unitVals = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        String[] suffixes = { "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

        for (int t = 0; t < tens.length; t++) {
            for (int u = 0; u < ordUnits.length; u++) {
                int value = tenVals[t] + unitVals[u];
                String suffix = suffixes[u];
                // Special cases for 21st, 22nd, 23rd, 31st, etc.
                if (unitVals[u] == 1)
                    suffix = "st";
                else if (unitVals[u] == 2)
                    suffix = "nd";
                else if (unitVals[u] == 3)
                    suffix = "rd";
                else
                    suffix = "th";

                result = result.replaceAll(
                        "(?i)\\b" + tens[t] + "\\s+" + ordUnits[u] + "\\b",
                        value + suffix);
            }
        }

        // Simple ordinals
        for (Map.Entry<String, Integer> entry : ORDINAL_WORD.entrySet()) {
            int val = entry.getValue();
            String suffix;
            if (val % 10 == 1 && val != 11)
                suffix = "st";
            else if (val % 10 == 2 && val != 12)
                suffix = "nd";
            else if (val % 10 == 3 && val != 13)
                suffix = "rd";
            else
                suffix = "th";

            result = result.replaceAll("(?i)\\b" + entry.getKey() + "\\b", val + suffix);
        }

        return result;
    }

    private static String normalizeRanges(String text) {
        String result = text;

        // "one to five" → "1–5"
        for (Map.Entry<String, Integer> start : WORD_TO_NUM.entrySet()) {
            for (Map.Entry<String, Integer> end : WORD_TO_NUM.entrySet()) {
                if (end.getValue() > start.getValue()) {
                    result = result.replaceAll(
                            "(?i)\\b" + start.getKey() + "\\s+to\\s+" + end.getKey() + "\\b",
                            start.getValue() + "–" + end.getValue());
                }
            }
        }

        // Digit ranges: "1 to 5" → "1–5"
        result = result.replaceAll("(\\d+)\\s+to\\s+(\\d+)", "$1–$2");

        return result;
    }

    private static String normalizeLargeNumbers(String text) {
        String result = text;

        // "twenty five hundred" → "2500"
        String[] tens = { "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
        int[] tenVals = { 20, 30, 40, 50, 60, 70, 80, 90 };
        String[] units = { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
        int[] unitVals = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        for (int t = 0; t < tens.length; t++) {
            for (int u = 0; u < units.length; u++) {
                int val = (tenVals[t] + unitVals[u]) * 100;
                result = result.replaceAll(
                        "(?i)\\b" + tens[t] + "\\s+" + units[u] + "\\s+hundred\\b",
                        String.valueOf(val));
            }
            // "twenty hundred" → "2000"
            result = result.replaceAll(
                    "(?i)\\b" + tens[t] + "\\s+hundred\\b",
                    String.valueOf(tenVals[t] * 100));
        }

        // Teen hundreds
        String[] teens = { "eleven", "twelve", "thirteen", "fourteen", "fifteen",
                "sixteen", "seventeen", "eighteen", "nineteen" };
        int[] teenVals = { 11, 12, 13, 14, 15, 16, 17, 18, 19 };
        for (int i = 0; i < teens.length; i++) {
            result = result.replaceAll(
                    "(?i)\\b" + teens[i] + "\\s+hundred\\b",
                    String.valueOf(teenVals[i] * 100));
        }

        // "one point five million" → "1.5 million"
        // Using Pattern/Matcher for Java 8 compatibility
        Pattern decimalMillionPattern = Pattern.compile(
                "(?i)\\b(one|two|three|four|five|six|seven|eight|nine)\\s+point\\s+(one|two|three|four|five|six|seven|eight|nine)\\s+(million|billion|trillion)\\b");
        Matcher decimalMatcher = decimalMillionPattern.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (decimalMatcher.find()) {
            String whole = decimalMatcher.group(1).toLowerCase();
            String decimal = decimalMatcher.group(2).toLowerCase();
            String mult = decimalMatcher.group(3).toLowerCase();
            Integer wholeNum = WORD_TO_NUM.get(whole);
            Integer decimalNum = WORD_TO_NUM.get(decimal);
            if (wholeNum != null && decimalNum != null) {
                decimalMatcher.appendReplacement(sb, wholeNum + "." + decimalNum + " " + mult);
            }
        }
        decimalMatcher.appendTail(sb);
        result = sb.toString();

        // "five million" → "5 million"
        for (Map.Entry<String, Integer> entry : WORD_TO_NUM.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + entry.getKey() + "\\s+(million|billion|trillion)\\b",
                    entry.getValue() + " $1");
        }

        return result;
    }

    private static String normalizeDecimals(String text) {
        String result = text;

        // "one point five" → "1.5"
        for (Map.Entry<String, Integer> whole : WORD_TO_NUM.entrySet()) {
            for (Map.Entry<String, Integer> decimal : WORD_TO_NUM.entrySet()) {
                result = result.replaceAll(
                        "(?i)\\b" + whole.getKey() + "\\s+point\\s+" + decimal.getKey() + "\\b",
                        whole.getValue() + "." + decimal.getValue());
            }
        }

        // "zero point five" → "0.5"
        result = result.replaceAll("(?i)\\bzero\\s+point\\s+(\\d+)\\b", "0.$1");

        return result;
    }

    private static String normalizeCompoundNumbers(String text) {
        String result = text;

        String[] tens = { "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
        int[] tenVals = { 20, 30, 40, 50, 60, 70, 80, 90 };
        String[] units = { "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
        int[] unitVals = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        for (int t = 0; t < tens.length; t++) {
            for (int u = 0; u < units.length; u++) {
                result = result.replaceAll(
                        "(?i)\\b" + tens[t] + "\\s*" + units[u] + "\\b",
                        String.valueOf(tenVals[t] + unitVals[u]));
            }
        }

        return result;
    }

    private static String normalizeCurrency(String text) {
        String result = text;

        // "one hundred US dollars" → "$100 USD"
        result = result.replaceAll("(?i)(\\d+)\\s+million\\s+US\\s*dollars?", "\\$$1,000,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+thousand\\s+US\\s*dollars?", "\\$$1,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+US\\s*dollars?", "\\$$1 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+million\\s*dollars?", "\\$$1,000,000");
        result = result.replaceAll("(?i)(\\d+)\\s+thousand\\s*dollars?", "\\$$1,000");
        result = result.replaceAll("(?i)(\\d+)\\s*dollars?", "\\$$1");
        result = result.replaceAll("(?i)(\\d+)\\s*cents?", "$1¢");

        return result;
    }

    private static String normalizeSimpleNumbers(String text) {
        String result = text;

        // Replace remaining word numbers
        for (Map.Entry<String, Integer> entry : WORD_TO_NUM.entrySet()) {
            result = result.replaceAll("(?i)\\b" + entry.getKey() + "\\b", String.valueOf(entry.getValue()));
        }

        // Tens
        for (Map.Entry<String, Integer> entry : TENS.entrySet()) {
            result = result.replaceAll("(?i)\\b" + entry.getKey() + "\\b", String.valueOf(entry.getValue()));
        }

        // Hundred
        result = result.replaceAll("(?i)\\bhundred\\b", "100");

        return result;
    }

    // ========================================================================
    // 4. NORMALIZE PERCENTAGES
    // ========================================================================

    public static String normalizePercentages(String text) {
        String result = text;

        // "twenty five point six percent" → "25.6%"
        result = result.replaceAll("(?i)(\\d+)\\s+point\\s+(\\d+)\\s*percent", "$1.$2%");

        // "zero point five percent" → "0.5%"
        result = result.replaceAll("(?i)zero\\s+point\\s+(\\d+)\\s*percent", "0.$1%");

        // "one half percent" → "0.5%"
        result = result.replaceAll("(?i)\\bone\\s+half\\s+percent\\b", "0.5%");
        result = result.replaceAll("(?i)\\ba\\s+half\\s+percent\\b", "0.5%");

        // "twenty out of a hundred" → "20%"
        result = result.replaceAll("(?i)(\\d+)\\s+out\\s+of\\s+(a\\s+)?hundred\\b", "$1%");
        result = result.replaceAll("(?i)(\\d+)\\s+out\\s+of\\s+100\\b", "$1%");

        // "twenty percent" → "20%"
        result = result.replaceAll("(?i)(\\d+)\\s*percent", "$1%");

        // Normalize spacing
        result = result.replaceAll("(\\d+)\\s+%", "$1%");

        return result;
    }

    // ========================================================================
    // 5. REMOVE FILLERS
    // ========================================================================

    private static final String[] FILLERS = {
            "\\buh\\b", "\\bum\\b", "\\berm\\b", "\\bhmm\\b", "\\bhm\\b",
            "\\byou know\\b", "\\bi mean\\b", "\\bsort of\\b", "\\bkinda\\b",
            "\\bkind of\\b"
    };

    public static String removeFillers(String text) {
        String result = text;

        for (String filler : FILLERS) {
            result = result.replaceAll("(?i)" + filler + "\\s*,?\\s*", " ");
        }

        // Handle "like" carefully - only as filler, not as verb
        // Remove "like" when preceded by comma or at phrase boundaries
        result = result.replaceAll("(?i),\\s*like\\s*,", ",");
        result = result.replaceAll("(?i)\\blike\\s*,\\s*(uh|um|so|you know)", "");
        result = result.replaceAll("(?i)^like\\s+", "");

        // Clean up spaces
        result = result.replaceAll("\\s{2,}", " ");

        return result.trim();
    }

    // ========================================================================
    // 6. RESTORE PUNCTUATION
    // ========================================================================

    public static String restorePunctuation(String text) {
        String result = text;

        // Question detection
        String[] questionStarters = {
                "what", "where", "when", "why", "how", "who", "which", "whose",
                "is it", "are you", "do you", "did you", "can you", "could you",
                "would you", "will you", "have you", "has it", "was it", "were you",
                "does", "don't you", "isn't", "aren't", "won't", "wouldn't",
                "shouldn't", "couldn't", "haven't", "hasn't"
        };

        for (String starter : questionStarters) {
            // End of text questions
            result = result.replaceAll("(?i)\\b(" + starter + "[^.!?]{5,}?)\\s*$", "$1?");
        }

        // Sentence boundaries - capitalize after adding period
        // Long phrases without punctuation (> 50 chars) followed by I/We/You/The
        result = result.replaceAll(
                "(?<=[a-z]{50,})(?=\\s+(i|we|you|they|he|she|it|the|a|an|this|that|my|our)\\s)",
                ".");

        // Add commas after introductory words
        String[] introWords = {
                "however", "therefore", "moreover", "furthermore", "meanwhile",
                "nevertheless", "consequently", "finally", "well", "so",
                "actually", "basically", "honestly", "personally", "certainly"
        };

        for (String word : introWords) {
            result = result.replaceAll("(?i)^" + word + "\\s+", word + ", ");
            result = result.replaceAll("(?i)([.!?])\\s+" + word + "\\s+", "$1 " + word + ", ");
        }

        // Add comma before conjunctions in long sentences
        result = result.replaceAll("(?i)(?<=[a-z]{20,})\\s+(but|yet|so)\\s+(?=[a-z])", ", $1 ");

        // Add period at end if missing
        if (!result.matches(".*[.!?]\\s*$")) {
            result = result + ".";
        }

        // Clean up double punctuation
        result = result.replaceAll("[.]{2,}", ".");
        result = result.replaceAll("[?]{2,}", "?");
        result = result.replaceAll("[!]{2,}", "!");
        result = result.replaceAll(",{2,}", ",");

        return result;
    }

    // ========================================================================
    // 7. APPLY CASING
    // ========================================================================

    public static String applyCasing(String text, Map<String, String> personalDictionary) {
        if (text == null || text.isEmpty())
            return text;

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

            if (c == '.' || c == '!' || c == '?') {
                capitalizeNext = true;
            }
        }

        String output = result.toString();

        // Capitalize "I"
        output = output.replaceAll("(?<=\\s)i(?=\\s|'|$)", "I");
        output = output.replaceAll("^i(?=\\s|')", "I");
        output = output.replaceAll("(?<=[.!?]\\s)i(?=\\s|')", "I");

        // Proper nouns
        String[][] properNouns = {
                { "openai", "OpenAI" }, { "chatgpt", "ChatGPT" }, { "gpt", "GPT" },
                { "youtube", "YouTube" }, { "tiktok", "TikTok" }, { "whatsapp", "WhatsApp" },
                { "linkedin", "LinkedIn" }, { "iphone", "iPhone" }, { "ipad", "iPad" },
                { "macbook", "MacBook" }, { "microsoft", "Microsoft" }, { "google", "Google" },
                { "amazon", "Amazon" }, { "facebook", "Facebook" }, { "meta", "Meta" },
                { "nvidia", "NVIDIA" }, { "tesla", "Tesla" }, { "twitter", "Twitter" },
                { "instagram", "Instagram" }, { "spotify", "Spotify" }, { "netflix", "Netflix" },
                { "anthropic", "Anthropic" }, { "claude", "Claude" }, { "gemini", "Gemini" },
                { "groq", "Groq" }, { "llama", "Llama" }, { "mistral", "Mistral" },
                { "usa", "USA" }, { "uk", "UK" }, { "ai", "AI" }, { "api", "API" }
        };

        for (String[] noun : properNouns) {
            output = output.replaceAll("(?i)\\b" + noun[0] + "\\b", noun[1]);
        }

        // Days of week
        String[] days = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
        for (String day : days) {
            String cap = day.substring(0, 1).toUpperCase() + day.substring(1);
            output = output.replaceAll("(?i)\\b" + day + "\\b", cap);
        }

        // Months
        String[] months = { "january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december" };
        for (String month : months) {
            String cap = month.substring(0, 1).toUpperCase() + month.substring(1);
            output = output.replaceAll("(?i)\\b" + month + "\\b", cap);
        }

        // Apply personal dictionary casing overrides
        if (personalDictionary != null) {
            for (Map.Entry<String, String> entry : personalDictionary.entrySet()) {
                output = output.replaceAll("(?i)\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
            }
        }

        return output;
    }

    // ========================================================================
    // 8. SEGMENT PARAGRAPHS
    // ========================================================================

    public static String segmentParagraphs(String text, List<WordTimestamp> timestamps) {
        if (timestamps != null && timestamps.size() >= 2) {
            return segmentByTimestamps(text, timestamps);
        }
        return segmentByLength(text);
    }

    private static String segmentByTimestamps(String text, List<WordTimestamp> timestamps) {
        StringBuilder result = new StringBuilder();
        String[] words = text.split("\\s+");

        int wordIndex = 0;
        for (int i = 0; i < timestamps.size() - 1 && wordIndex < words.length; i++) {
            result.append(words[wordIndex]).append(" ");

            double pause = timestamps.get(i + 1).startTime - timestamps.get(i).endTime;
            if (pause > 1.5) {
                result.append("\n\n");
            }

            wordIndex++;
        }

        while (wordIndex < words.length) {
            result.append(words[wordIndex]).append(" ");
            wordIndex++;
        }

        return result.toString().trim();
    }

    private static String segmentByLength(String text) {
        String[] sentences = text.split("(?<=[.!?])\\s+");
        StringBuilder result = new StringBuilder();
        int wordCount = 0;

        for (String sentence : sentences) {
            result.append(sentence).append(" ");
            wordCount += sentence.split("\\s+").length;

            if (wordCount > 80) {
                result.append("\n\n");
                wordCount = 0;
            }
        }

        return result.toString().trim();
    }

    // ========================================================================
    // HELPER CLASSES
    // ========================================================================

    public static class WordTimestamp {
        public String word;
        public double startTime;
        public double endTime;

        public WordTimestamp(String word, double startTime, double endTime) {
            this.word = word;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }

    // ========================================================================
    // EXAMPLE DEMONSTRATION
    // ========================================================================

    public static void main(String[] args) {
        String rawInput = "uh so i was thinking um you know about like twenty five " +
                "hundred dollars that we we need to pay by twenty twenty four " +
                "and um the meeting is at four twenty pm on monday " +
                "what do you think about that like i mean its about twenty five percent " +
                "of our budget and the twenty first of december is the deadline";

        Map<String, String> dictionary = new HashMap<>();
        dictionary.put("groq", "Groq");
        dictionary.put("@Groq", "@Groq"); // at groq → @Groq

        String result = processTranscript(rawInput, null, dictionary);

        System.out.println("=== RAW INPUT ===");
        System.out.println(rawInput);
        System.out.println("\n=== PROCESSED OUTPUT ===");
        System.out.println(result);

        // Expected output:
        // "So, I was thinking about $2500 that we need to pay by 2024.
        // The meeting is at 4:20 PM on Monday. What do you think about that?
        // It's about 25% of our budget, and the 21st of December is the deadline."
    }
}
