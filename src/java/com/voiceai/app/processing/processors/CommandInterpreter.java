package com.voiceai.app.processing.processors;

import com.voiceai.app.processing.ProcessingContext;
import com.voiceai.app.processing.TextProcessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CommandInterpreter - Voice command detection and execution
 * 
 * Wispr Flow-style command mode that interprets voice commands like:
 * - "delete that" / "undo" / "clear all"
 * - "make formal" / "add bullets" / "capitalize"
 * - "new paragraph" / "new line" / "period"
 * 
 * Commands are detected BEFORE other processing and can modify the text
 * or return special markers for the UI to handle.
 * 
 * NOTE: This is a rule-based implementation. For true intelligence,
 * integrate with on-device LLM (Gemma 2B via MediaPipe).
 */
public class CommandInterpreter implements TextProcessor {

    // Command result markers (UI interprets these)
    public static final String CMD_DELETE_LAST = "[[CMD:DELETE_LAST]]";
    public static final String CMD_UNDO = "[[CMD:UNDO]]";
    public static final String CMD_CLEAR = "[[CMD:CLEAR]]";
    public static final String CMD_NEW_PARAGRAPH = "\n\n";
    public static final String CMD_NEW_LINE = "\n";

    // Edit commands - modify or delete text
    private static final Pattern DELETE_THAT = Pattern.compile(
            "(?i)^\\s*(delete|remove|erase)\\s+(that|this|it|last\\s+word|last\\s+sentence)\\s*$");
    private static final Pattern UNDO = Pattern.compile(
            "(?i)^\\s*(undo|undo\\s+that|go\\s+back)\\s*$");
    private static final Pattern CLEAR_ALL = Pattern.compile(
            "(?i)^\\s*(clear|clear\\s+all|start\\s+over|delete\\s+all)\\s*$");

    // Navigation/structure commands
    private static final Pattern NEW_PARAGRAPH = Pattern.compile(
            "(?i)^\\s*(new\\s+paragraph|next\\s+paragraph|paragraph)\\s*$");
    private static final Pattern NEW_LINE = Pattern.compile(
            "(?i)^\\s*(new\\s+line|next\\s+line|line\\s+break)\\s*$");

    // Punctuation commands
    private static final Pattern ADD_PERIOD = Pattern.compile(
            "(?i)^\\s*(period|full\\s+stop|dot)\\s*$");
    private static final Pattern ADD_COMMA = Pattern.compile(
            "(?i)^\\s*(comma)\\s*$");
    private static final Pattern ADD_QUESTION = Pattern.compile(
            "(?i)^\\s*(question\\s+mark|question)\\s*$");
    private static final Pattern ADD_EXCLAMATION = Pattern.compile(
            "(?i)^\\s*(exclamation|exclamation\\s+(point|mark))\\s*$");

    // Format commands - these modify the previous text
    // For now, we'll handle simple ones; complex ones need LLM
    private static final Pattern CAPITALIZE_THAT = Pattern.compile(
            "(?i)^\\s*(capitalize|caps|uppercase)\\s+(that|this|it)\\s*$");
    private static final Pattern LOWERCASE_THAT = Pattern.compile(
            "(?i)^\\s*(lowercase|lower\\s+case)\\s+(that|this|it)\\s*$");

    // Future LLM commands (detected but flagged for LLM processing)
    private static final Pattern MAKE_FORMAL = Pattern.compile(
            "(?i)^\\s*(make|convert)\\s+(this|that|it)\\s+(formal|professional|more\\s+formal)\\s*$");
    private static final Pattern ADD_BULLETS = Pattern.compile(
            "(?i)^\\s*(add|convert\\s+to|make)\\s+bullets?\\s*$");
    private static final Pattern SHORTEN = Pattern.compile(
            "(?i)^\\s*(shorten|make\\s+shorter|summarize)\\s+(that|this|it)?\\s*$");

    @Override
    public String process(String text, ProcessingContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String trimmed = text.trim();

        // Check for pure command (entire input is a command)
        CommandResult pureCommand = detectPureCommand(trimmed);
        if (pureCommand != null) {
            return pureCommand.result;
        }

        // Check for inline commands (command embedded in dictation)
        return processInlineCommands(text);
    }

    /**
     * Detect if the entire input is a voice command
     */
    private CommandResult detectPureCommand(String text) {
        // Edit commands
        if (DELETE_THAT.matcher(text).matches()) {
            return new CommandResult(CMD_DELETE_LAST, CommandType.EDIT);
        }
        if (UNDO.matcher(text).matches()) {
            return new CommandResult(CMD_UNDO, CommandType.EDIT);
        }
        if (CLEAR_ALL.matcher(text).matches()) {
            return new CommandResult(CMD_CLEAR, CommandType.EDIT);
        }

        // Navigation commands
        if (NEW_PARAGRAPH.matcher(text).matches()) {
            return new CommandResult(CMD_NEW_PARAGRAPH, CommandType.INSERT);
        }
        if (NEW_LINE.matcher(text).matches()) {
            return new CommandResult(CMD_NEW_LINE, CommandType.INSERT);
        }

        // Punctuation commands
        if (ADD_PERIOD.matcher(text).matches()) {
            return new CommandResult(".", CommandType.INSERT);
        }
        if (ADD_COMMA.matcher(text).matches()) {
            return new CommandResult(",", CommandType.INSERT);
        }
        if (ADD_QUESTION.matcher(text).matches()) {
            return new CommandResult("?", CommandType.INSERT);
        }
        if (ADD_EXCLAMATION.matcher(text).matches()) {
            return new CommandResult("!", CommandType.INSERT);
        }

        // Format commands (need previous context - return markers)
        if (CAPITALIZE_THAT.matcher(text).matches()) {
            return new CommandResult("[[CMD:CAPITALIZE_LAST]]", CommandType.FORMAT);
        }
        if (LOWERCASE_THAT.matcher(text).matches()) {
            return new CommandResult("[[CMD:LOWERCASE_LAST]]", CommandType.FORMAT);
        }

        // LLM commands (flag for future LLM processing)
        if (MAKE_FORMAL.matcher(text).matches()) {
            return new CommandResult("[[CMD:LLM:FORMALIZE]]", CommandType.LLM);
        }
        if (ADD_BULLETS.matcher(text).matches()) {
            return new CommandResult("[[CMD:LLM:BULLETS]]", CommandType.LLM);
        }
        if (SHORTEN.matcher(text).matches()) {
            return new CommandResult("[[CMD:LLM:SHORTEN]]", CommandType.LLM);
        }

        return null;
    }

    /**
     * Process inline commands embedded in dictation
     * e.g., "Hello comma how are you question mark" → "Hello, how are you?"
     */
    private String processInlineCommands(String text) {
        String result = text;

        // Inline punctuation
        result = result.replaceAll("(?i)\\s*\\bperiod\\b\\s*", ". ");
        result = result.replaceAll("(?i)\\s*\\bfull\\s+stop\\b\\s*", ". ");
        result = result.replaceAll("(?i)\\s*\\bcomma\\b\\s*", ", ");
        result = result.replaceAll("(?i)\\s*\\bquestion\\s+mark\\b\\s*", "? ");
        result = result.replaceAll("(?i)\\s*\\bexclamation\\s+(point|mark)\\b\\s*", "! ");
        result = result.replaceAll("(?i)\\s*\\bexclamation\\b\\s*", "! ");

        // Inline navigation
        result = result.replaceAll("(?i)\\s*\\bnew\\s+paragraph\\b\\s*", "\n\n");
        result = result.replaceAll("(?i)\\s*\\bnew\\s+line\\b\\s*", "\n");

        // Cleanup double spaces
        result = result.replaceAll("\\s{2,}", " ");
        result = result.replaceAll("\\s+\\.", ".");
        result = result.replaceAll("\\s+,", ",");
        result = result.replaceAll("\\s+\\?", "?");
        result = result.replaceAll("\\s+!", "!");

        return result.trim();
    }

    // Command types for UI handling
    public enum CommandType {
        EDIT, // Modifies existing text (delete, undo)
        INSERT, // Inserts new content (punctuation, newline)
        FORMAT, // Changes format of previous text
        LLM // Requires LLM for processing
    }

    private static class CommandResult {
        final String result;
        final CommandType type;

        CommandResult(String result, CommandType type) {
            this.result = result;
            this.type = type;
        }
    }
}
