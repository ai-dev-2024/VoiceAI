package com.voiceai.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Accessibility Service for injecting dictated text at cursor position
 * Used when external keyboards (SwiftKey) don't handle Intent results
 */
public class VoiceTextInjectionService extends AccessibilityService {

    private static final String TAG = "VoiceAI";
    private static VoiceTextInjectionService instance = null;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "VoiceTextInjectionService connected");

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed - we just need the service connection for text injection
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "VoiceTextInjectionService interrupted");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "VoiceTextInjectionService destroyed");
    }

    /**
     * Check if accessibility service is enabled
     */
    public static boolean isServiceEnabled(Context context) {
        try {
            int enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            if (enabled != 1)
                return false;

            String serviceName = context.getPackageName() + "/" +
                    VoiceTextInjectionService.class.getCanonicalName();
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (enabledServices == null)
                return false;

            TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
            splitter.setString(enabledServices);
            while (splitter.hasNext()) {
                String service = splitter.next();
                if (service.equalsIgnoreCase(serviceName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service", e);
        }
        return false;
    }

    /**
     * Inject text at current cursor position
     * Returns true if injection successful, false if fell back to clipboard
     */
    public static boolean injectText(Context context, String text) {
        Log.d(TAG, "injectText called with text length: " + (text != null ? text.length() : 0));

        if (text == null || text.isEmpty()) {
            Log.w(TAG, "Empty text, nothing to inject");
            return false;
        }

        // ============ PRIORITY 1: IME InputConnection (UNIVERSAL - works in ALL apps)
        // ============
        // This is the same method keyboards use, works even in Word, Chrome, etc.
        if (RustInputMethodService.isAvailable()) {
            Log.d(TAG, "IME service available, using commitText for universal injection");
            if (RustInputMethodService.injectText(text)) {
                Log.d(TAG, "Text injected via IME InputConnection successfully!");
                return true;
            }
        }

        // ============ PRIORITY 2: Accessibility Service (fallback) ============
        // Check if accessibility service is available
        if (instance == null) {
            Log.w(TAG, "VoiceTextInjectionService instance is null - service not connected");
            copyToClipboard(context, text);
            return false;
        }

        if (!isServiceEnabled(context)) {
            Log.w(TAG, "Accessibility service is not enabled in settings");
            copyToClipboard(context, text);
            return false;
        }

        // Try immediate injection first
        if (instance.injectViaAccessibility(text)) {
            Log.d(TAG, "Text injected via accessibility immediately");
            return true;
        }

        // Schedule delayed retries for system-wide injection
        // This handles cases where focus hasn't returned to original window yet
        Log.d(TAG, "Immediate injection failed, scheduling delayed retries");
        scheduleDelayedInjection(context, text);
        return true; // Return true since we've scheduled the injection
    }

    /**
     * Schedule delayed injection attempts with increasing delays
     * This is critical for system-wide injection where focus restoration takes time
     */
    private static void scheduleDelayedInjection(Context context, String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        final int[] delays = { 150, 300, 500 }; // Increasing delays for focus restoration

        for (int i = 0; i < delays.length; i++) {
            final int attempt = i + 1;
            handler.postDelayed(() -> {
                if (instance != null) {
                    Log.d(TAG, "Delayed injection attempt " + attempt);
                    if (instance.injectViaAccessibility(text)) {
                        Log.d(TAG, "Text injected via accessibility on delayed attempt " + attempt);
                    } else if (attempt == delays.length) {
                        // Last attempt failed, copy to clipboard
                        Log.d(TAG, "All delayed attempts failed, falling back to clipboard");
                        copyToClipboard(context, text);
                    }
                }
            }, delays[i]);
        }
    }

    private static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("VoiceAI", text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Text copied to clipboard");

            // Show user-friendly toast notification
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> {
                Toast.makeText(context, "✓ Copied! Tap text field to paste", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error copying to clipboard", e);
        }
    }

    private boolean injectViaAccessibility(String text) {
        try {
            // Try active window first
            if (tryInjectInWindow(getRootInActiveWindow(), text)) {
                return true;
            }

            // Fallback: try ALL windows for system-wide injection
            // This is critical for multi-window scenarios and app switching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                List<AccessibilityWindowInfo> windows = getWindows();
                Log.d(TAG, "Traversing " + windows.size() + " windows for editable node");

                for (AccessibilityWindowInfo window : windows) {
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root != null) {
                        if (tryInjectInWindow(root, text)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error injecting via accessibility", e);
        }
        return false;
    }

    private boolean tryInjectInWindow(AccessibilityNodeInfo root, String text) {
        if (root == null) {
            return false;
        }

        try {
            AccessibilityNodeInfo focused = findFocusedEditableNode(root);
            if (focused != null) {
                boolean result = insertTextAtCursor(focused, text);
                if (!result) {
                    // Fallback: try paste action
                    result = tryPasteText(focused, text);
                }
                focused.recycle();
                root.recycle();
                return result;
            }

            // If no focused editable found, try first editable node
            AccessibilityNodeInfo anyEditable = findAnyEditableNode(root);
            if (anyEditable != null) {
                Log.d(TAG, "No focused editable, trying any editable node");
                boolean result = insertTextAtCursor(anyEditable, text);
                if (!result) {
                    result = tryPasteText(anyEditable, text);
                }
                anyEditable.recycle();
                root.recycle();
                return result;
            }

            root.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error in tryInjectInWindow", e);
        }
        return false;
    }

    private boolean tryPasteText(AccessibilityNodeInfo node, String text) {
        try {
            // Copy to clipboard first
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("VoiceAI", text);
            clipboard.setPrimaryClip(clip);

            // Try paste action
            boolean pasted = node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            Log.d(TAG, "Paste action result: " + pasted);
            return pasted;
        } catch (Exception e) {
            Log.e(TAG, "Error in paste fallback", e);
            return false;
        }
    }

    private AccessibilityNodeInfo findFocusedEditableNode(AccessibilityNodeInfo root) {
        // Try input focus first
        AccessibilityNodeInfo focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focused != null && focused.isEditable()) {
            return focused;
        }
        if (focused != null)
            focused.recycle();

        // Search for focused editable node
        return searchForEditableNode(root);
    }

    private AccessibilityNodeInfo searchForEditableNode(AccessibilityNodeInfo node) {
        if (node.isEditable() && node.isFocused()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = searchForEditableNode(child);
                child.recycle();
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private AccessibilityNodeInfo findAnyEditableNode(AccessibilityNodeInfo node) {
        // Find any editable node, even if not focused
        if (node.isEditable()) {
            return AccessibilityNodeInfo.obtain(node);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findAnyEditableNode(child);
                child.recycle();
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    private boolean insertTextAtCursor(AccessibilityNodeInfo node, String text) {
        try {
            // First ensure the node has focus via ACTION_CLICK
            if (!node.isFocused()) {
                Log.d(TAG, "Node not focused, attempting to focus via ACTION_CLICK");
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                // Give a tiny bit of time for focus to take effect
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }

            // Also try ACTION_FOCUS to ensure input focus
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

            CharSequence currentText = node.getText();
            String current = currentText != null ? currentText.toString() : "";

            int selStart = node.getTextSelectionStart();
            int selEnd = node.getTextSelectionEnd();

            if (selStart < 0)
                selStart = current.length();
            if (selEnd < 0)
                selEnd = selStart;
            if (selStart > current.length())
                selStart = current.length();
            if (selEnd > current.length())
                selEnd = current.length();

            // Smart spacing after punctuation
            String textToInsert = text;
            if (selStart > 0 && selStart <= current.length()) {
                char before = current.charAt(selStart - 1);
                if (before == '.' || before == '!' || before == '?' || before == ',') {
                    textToInsert = " " + text;
                }
            }

            // Build new text
            StringBuilder newText = new StringBuilder(current);
            if (selEnd > selStart) {
                newText.replace(selStart, selEnd, textToInsert);
            } else {
                newText.insert(selStart, textToInsert);
            }

            // Try METHOD 1: ACTION_SET_TEXT (standard approach)
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText.toString());
            boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            Log.d(TAG, "ACTION_SET_TEXT result: " + success);

            if (success) {
                // Move cursor to end of inserted text
                int newPos = selStart + textToInsert.length();
                Bundle selArgs = new Bundle();
                selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newPos);
                selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newPos);
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs);
                return true;
            }

            // METHOD 2: For apps like Word that don't support ACTION_SET_TEXT,
            // try setting just the new text (without cursor position logic)
            Log.d(TAG, "Trying fallback: SET_TEXT with just the text to insert");
            Bundle directArgs = new Bundle();
            directArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, textToInsert);
            success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, directArgs);
            Log.d(TAG, "Direct SET_TEXT result: " + success);

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting text", e);
            return false;
        }
    }
}
