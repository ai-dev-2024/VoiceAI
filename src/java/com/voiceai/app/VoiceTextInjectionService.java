package com.voiceai.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

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

        // Check if service is available
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

        // Try accessibility injection with retries
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            Log.d(TAG, "Injection attempt " + attempt + "/" + maxRetries);
            
            if (instance.injectViaAccessibility(text)) {
                Log.d(TAG, "Text injected via accessibility on attempt " + attempt);
                return true;
            }
            
            // Brief delay before retry
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Fallback to clipboard after all retries failed
        Log.d(TAG, "All injection attempts failed, falling back to clipboard");
        copyToClipboard(context, text);
        return false;
    }

    private static void copyToClipboard(Context context, String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("VoiceAI", text);
            clipboard.setPrimaryClip(clip);
            Log.d(TAG, "Text copied to clipboard");
        } catch (Exception e) {
            Log.e(TAG, "Error copying to clipboard", e);
        }
    }

    private boolean injectViaAccessibility(String text) {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                Log.d(TAG, "No root window");
                return false;
            }

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
            Log.e(TAG, "Error injecting via accessibility", e);
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

            // Set the new text
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText.toString());
            boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);

            if (success) {
                // Move cursor to end of inserted text
                int newPos = selStart + textToInsert.length();
                Bundle selArgs = new Bundle();
                selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newPos);
                selArgs.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newPos);
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error inserting text", e);
            return false;
        }
    }
}
