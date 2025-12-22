package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NumberNormalizer - Converts spoken numbers to digits
 * 
 * This is a focused, clean implementation that handles:
 * - Cardinal numbers: "twenty five" → "25"
 * - Time expressions: "four twenty pm" → "4:20 PM"
 * - Years: "twenty twenty four" → "2024"
 * - Ordinals: "twenty first" → "21st" (with context awareness)
 * - Decimals: "one point five" → "1.5"
 * - Ranges: "one to five" → "1–5"
 * - Currency: "one hundred dollars" → "$100"
 * - Percentages: "twenty five percent" → "25%"
 * 
 * BUG FIX: Ordinals only converted when preceded by "the" or a number
 * to avoid "first thing" → "1st thing"
 */
public class NumberNormalizer implements TextProcessor {

    // Word to number mappings
    private static final Map<String, Integer> ONES = new HashMap<>();
    private static final Map<String, Integer> TEENS = new HashMap<>();
    private static final Map<String, Integer> TENS = new HashMap<>();
    private static final Map<String, String> ORDINAL_SUFFIX = new HashMap<>();

    static {
        // Ones
        ONES.put("zero", 0);
        ONES.put("one", 1);
        ONES.put("two", 2);
        ONES.put("three", 3);
        ONES.put("four", 4);
        ONES.put("five", 5);
        ONES.put("six", 6);
        ONES.put("seven", 7);
        ONES.put("eight", 8);
        ONES.put("nine", 9);

        // Teens
        TEENS.put("ten", 10);
        TEENS.put("eleven", 11);
        TEENS.put("twelve", 12);
        TEENS.put("thirteen", 13);
        TEENS.put("fourteen", 14);
        TEENS.put("fifteen", 15);
        TEENS.put("sixteen", 16);
        TEENS.put("seventeen", 17);
        TEENS.put("eighteen", 18);
        TEENS.put("nineteen", 19);

        // Tens
        TENS.put("twenty", 20);
        TENS.put("thirty", 30);
        TENS.put("forty", 40);
        TENS.put("fifty", 50);
        TENS.put("sixty", 60);
        TENS.put("seventy", 70);
        TENS.put("eighty", 80);
        TENS.put("ninety", 90);

        // Ordinal suffixes
        ORDINAL_SUFFIX.put("first", "1st");
        ORDINAL_SUFFIX.put("second", "2nd");
        ORDINAL_SUFFIX.put("third", "3rd");
        ORDINAL_SUFFIX.put("fourth", "4th");
        ORDINAL_SUFFIX.put("fifth", "5th");
        ORDINAL_SUFFIX.put("sixth", "6th");
        ORDINAL_SUFFIX.put("seventh", "7th");
        ORDINAL_SUFFIX.put("eighth", "8th");
        ORDINAL_SUFFIX.put("ninth", "9th");
        ORDINAL_SUFFIX.put("tenth", "10th");
        ORDINAL_SUFFIX.put("eleventh", "11th");
        ORDINAL_SUFFIX.put("twelfth", "12th");
        ORDINAL_SUFFIX.put("thirteenth", "13th");
        ORDINAL_SUFFIX.put("twentieth", "20th");
        ORDINAL_SUFFIX.put("thirtieth", "30th");
        ORDINAL_SUFFIX.put("fortieth", "40th");
        ORDINAL_SUFFIX.put("fiftieth", "50th");
        ORDINAL_SUFFIX.put("sixtieth", "60th");
        ORDINAL_SUFFIX.put("seventieth", "70th");
        ORDINAL_SUFFIX.put("eightieth", "80th");
        ORDINAL_SUFFIX.put("ninetieth", "90th");
        ORDINAL_SUFFIX.put("hundredth", "100th");
    }

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Work with lowercase for pattern matching, but track original for preservation
        String result = text;

        // Order matters - process complex patterns first
        result = normalizeYears(result);
        result = normalizeTimes(result);
        result = normalizeOrdinals(result);
        result = normalizeRanges(result);
        result = normalizeLargeNumbers(result);
        result = normalizeDecimals(result);
        result = normalizeCompoundNumbers(result);
        result = normalizeCurrency(result);
        result = normalizePercentages(result);
        result = normalizeSimpleNumbers(result);

        return result;
    }

    /**
     * Years: "twenty twenty four" → "2024"
     */
    private String normalizeYears(String text) {
        String result = text;

        // 2020s: twenty twenty → 2020, twenty twenty four → 2024
        result = result.replaceAll("(?i)\\btwenty\\s+twenty\\b(?!\\s*\\w)", "2020");
        for (Map.Entry<String, Integer> e : ONES.entrySet()) {
            if (e.getValue() >= 1 && e.getValue() <= 9) {
                result = result.replaceAll(
                        "(?i)\\btwenty\\s+twenty\\s+" + e.getKey() + "\\b",
                        String.valueOf(2020 + e.getValue()));
            }
        }

        // 2010s: twenty + teen
        for (Map.Entry<String, Integer> e : TEENS.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\btwenty\\s+" + e.getKey() + "\\b",
                    String.valueOf(2000 + e.getValue()));
        }

        // 1990s, 1980s, etc.: nineteen + tens + ones
        String[] decades = { "ninety", "eighty", "seventy", "sixty" };
        int[] decadeValues = { 1990, 1980, 1970, 1960 };

        for (int d = 0; d < decades.length; d++) {
            result = result.replaceAll(
                    "(?i)\\bnineteen\\s+" + decades[d] + "\\b(?!\\s*\\w)",
                    String.valueOf(decadeValues[d]));
            for (Map.Entry<String, Integer> e : ONES.entrySet()) {
                if (e.getValue() >= 1) {
                    result = result.replaceAll(
                            "(?i)\\bnineteen\\s+" + decades[d] + "\\s+" + e.getKey() + "\\b",
                            String.valueOf(decadeValues[d] + e.getValue()));
                }
            }
        }

        return result;
    }

    /**
     * Times: "four twenty pm" → "4:20 PM"
     */
    private String normalizeTimes(String text) {
        String result = text;

        // Pattern: [hour] [minutes] [am/pm]
        for (Map.Entry<String, Integer> hour : ONES.entrySet()) {
            if (hour.getValue() >= 1 && hour.getValue() <= 12) {
                // "four oh five pm" → "4:05 PM"
                for (Map.Entry<String, Integer> min : ONES.entrySet()) {
                    if (min.getValue() >= 1 && min.getValue() <= 9) {
                        result = result.replaceAll(
                                "(?i)\\b" + hour.getKey() + "\\s+oh\\s+" + min.getKey() + "\\s*(am|pm)\\b",
                                hour.getValue() + ":0" + min.getValue() + " " + "$1".toUpperCase());
                    }
                }

                // "four twenty pm" → "4:20 PM"
                for (Map.Entry<String, Integer> tens : TENS.entrySet()) {
                    result = result.replaceAll(
                            "(?i)\\b" + hour.getKey() + "\\s+" + tens.getKey() + "\\s*(am|pm)\\b",
                            hour.getValue() + ":" + tens.getValue() + " " + "$1".toUpperCase());

                    // "four twenty five pm" → "4:25 PM"
                    for (Map.Entry<String, Integer> ones : ONES.entrySet()) {
                        if (ones.getValue() >= 1 && ones.getValue() <= 9) {
                            result = result.replaceAll(
                                    "(?i)\\b" + hour.getKey() + "\\s+" + tens.getKey() + "\\s+" +
                                            ones.getKey() + "\\s*(am|pm)\\b",
                                    hour.getValue() + ":" + (tens.getValue() + ones.getValue()) +
                                            " " + "$1".toUpperCase());
                        }
                    }
                }

                // Simple: "four pm" → "4 PM"
                result = result.replaceAll(
                        "(?i)\\b" + hour.getKey() + "\\s*(am|pm)\\b",
                        hour.getValue() + " " + "$1".toUpperCase());
            }
        }

        // Normalize AM/PM casing
        result = result.replaceAll("(?i)\\s(am|pm)\\b", " $1".toUpperCase());

        return result;
    }

    /**
     * Ordinals: "twenty first" → "21st"
     * 
     * BUG FIX: Only convert when preceded by "the" or a number
     * "the first" → "the 1st" ✓
     * "first thing" → "first thing" ✓ (unchanged)
     */
    private String normalizeOrdinals(String text) {
        String result = text;

        // Compound ordinals with "the": "the twenty first" → "the 21st"
        for (Map.Entry<String, Integer> tens : TENS.entrySet()) {
            String[] ordUnits = { "first", "second", "third", "fourth", "fifth",
                    "sixth", "seventh", "eighth", "ninth" };
            int[] unitVals = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };

            for (int i = 0; i < ordUnits.length; i++) {
                int value = tens.getValue() + unitVals[i];
                String suffix = getOrdinalSuffix(value);

                // Only with "the" prefix
                result = result.replaceAll(
                        "(?i)\\bthe\\s+" + tens.getKey() + "\\s+" + ordUnits[i] + "\\b",
                        "the " + value + suffix);

                // In date context: "December twenty first"
                result = result.replaceAll(
                        "(?i)(january|february|march|april|may|june|july|august|" +
                                "september|october|november|december)\\s+" +
                                tens.getKey() + "\\s+" + ordUnits[i] + "\\b",
                        "$1 " + value + suffix);
            }
        }

        // Simple ordinals only with "the": "the first" → "the 1st"
        for (Map.Entry<String, String> ord : ORDINAL_SUFFIX.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\bthe\\s+" + ord.getKey() + "\\b",
                    "the " + ord.getValue());
        }

        return result;
    }

    private String getOrdinalSuffix(int n) {
        if (n >= 11 && n <= 13)
            return "th";
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    /**
     * Ranges: "one to five" → "1–5"
     */
    private String normalizeRanges(String text) {
        String result = text;

        // Word ranges: "one to five" → "1–5"
        for (Map.Entry<String, Integer> start : ONES.entrySet()) {
            for (Map.Entry<String, Integer> end : ONES.entrySet()) {
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

    /**
     * Large numbers: "twenty five hundred" → "2500"
     */
    private String normalizeLargeNumbers(String text) {
        String result = text;

        // "X hundred" patterns
        for (Map.Entry<String, Integer> tens : TENS.entrySet()) {
            for (Map.Entry<String, Integer> ones : ONES.entrySet()) {
                if (ones.getValue() >= 1) {
                    int val = (tens.getValue() + ones.getValue()) * 100;
                    result = result.replaceAll(
                            "(?i)\\b" + tens.getKey() + "\\s+" + ones.getKey() + "\\s+hundred\\b",
                            String.valueOf(val));
                }
            }
            // "twenty hundred" → "2000"
            result = result.replaceAll(
                    "(?i)\\b" + tens.getKey() + "\\s+hundred\\b",
                    String.valueOf(tens.getValue() * 100));
        }

        // "five million" → "5 million"
        for (Map.Entry<String, Integer> e : ONES.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + e.getKey() + "\\s+(million|billion|trillion)\\b",
                    e.getValue() + " $1");
        }

        return result;
    }

    /**
     * Decimals: "one point five" → "1.5"
     */
    private String normalizeDecimals(String text) {
        String result = text;

        for (Map.Entry<String, Integer> whole : ONES.entrySet()) {
            for (Map.Entry<String, Integer> decimal : ONES.entrySet()) {
                result = result.replaceAll(
                        "(?i)\\b" + whole.getKey() + "\\s+point\\s+" + decimal.getKey() + "\\b",
                        whole.getValue() + "." + decimal.getValue());
            }
        }

        return result;
    }

    /**
     * Compound numbers: "twenty five" → "25"
     */
    private String normalizeCompoundNumbers(String text) {
        String result = text;

        for (Map.Entry<String, Integer> tens : TENS.entrySet()) {
            for (Map.Entry<String, Integer> ones : ONES.entrySet()) {
                if (ones.getValue() >= 1) {
                    result = result.replaceAll(
                            "(?i)\\b" + tens.getKey() + "\\s*" + ones.getKey() + "\\b",
                            String.valueOf(tens.getValue() + ones.getValue()));
                }
            }
        }

        return result;
    }

    /**
     * Currency: "one hundred dollars" → "$100"
     * 
     * BUG FIX: Check for existing $ before adding
     */
    private String normalizeCurrency(String text) {
        String result = text;

        // Million/thousand scales first
        result = result.replaceAll("(?i)(\\d+)\\s+million\\s+US\\s*dollars?", "\\$$1,000,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+million\\s*dollars?", "\\$$1,000,000");
        result = result.replaceAll("(?i)(\\d+)\\s+thousand\\s+US\\s*dollars?", "\\$$1,000 USD");
        result = result.replaceAll("(?i)(\\d+)\\s+thousand\\s*dollars?", "\\$$1,000");

        // Simple amounts - only if not already prefixed with $
        result = result.replaceAll("(?i)(?<!\\$)(\\d+)\\s+US\\s*dollars?", "\\$$1 USD");
        result = result.replaceAll("(?i)(?<!\\$)(\\d+)\\s*dollars?", "\\$$1");
        result = result.replaceAll("(?i)(\\d+)\\s*cents?", "$1¢");

        return result;
    }

    /**
     * Percentages: "twenty five percent" → "25%"
     */
    private String normalizePercentages(String text) {
        String result = text;

        // Decimal percentages: "1.5 percent" → "1.5%"
        result = result.replaceAll("(?i)(\\d+)\\s+point\\s+(\\d+)\\s*percent", "$1.$2%");
        result = result.replaceAll("(?i)(\\d+\\.\\d+)\\s*percent", "$1%");

        // Simple: "25 percent" → "25%"
        result = result.replaceAll("(?i)(\\d+)\\s*percent", "$1%");

        // Cleanup spacing
        result = result.replaceAll("(\\d+)\\s+%", "$1%");

        return result;
    }

    /**
     * Simple numbers: replace remaining word numbers with digits
     */
    private String normalizeSimpleNumbers(String text) {
        String result = text;

        // Teens first (before ones to avoid "nineteen" → "nine-teen")
        for (Map.Entry<String, Integer> e : TEENS.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + e.getKey() + "\\b",
                    String.valueOf(e.getValue()));
        }

        // Tens
        for (Map.Entry<String, Integer> e : TENS.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + e.getKey() + "\\b",
                    String.valueOf(e.getValue()));
        }

        // Ones last
        for (Map.Entry<String, Integer> e : ONES.entrySet()) {
            result = result.replaceAll(
                    "(?i)\\b" + e.getKey() + "\\b",
                    String.valueOf(e.getValue()));
        }

        // Hundred
        result = result.replaceAll("(?i)\\bhundred\\b", "100");

        return result;
    }

    @Override
    public boolean shouldSkip(ProcessingContext context) {
        return !context.isNumberNormalization();
    }
}
