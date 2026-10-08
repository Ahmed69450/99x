package com.byd.voiceassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class VoiceAccessibilityService extends AccessibilityService {

    private static volatile String lastScreenContext = "Screen: Home Screen / Desktop";

    public static synchronized void updateScreenContext(String packageName, String details) {
        lastScreenContext = "Active App: " + (packageName != null ? packageName : "Unknown") +
                            "\nContent: " + (details != null ? details : "");
    }

    public static synchronized String getLastScreenContext() {
        return lastScreenContext;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        CharSequence pkg = event.getPackageName();
        String packageName = pkg != null ? pkg.toString() : "Unknown";

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            try {
                StringBuilder sb = new StringBuilder();
                traverseNode(root, sb, 0);
                updateScreenContext(packageName, sb.toString().trim());
            } finally {
                try {
                    root.recycle();
                } catch (Exception ignored) {}
            }
        } else if (event.getText() != null && !event.getText().isEmpty()) {
            updateScreenContext(packageName, event.getText().toString());
        }
    }

    private void traverseNode(AccessibilityNodeInfo node, StringBuilder sb, int depth) {
        if (node == null || depth > 5 || sb.length() > 500) {
            return;
        }

        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();

        if (text != null && text.length() > 0) {
            sb.append(text).append("; ");
        } else if (desc != null && desc.length() > 0) {
            sb.append(desc).append("; ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                try {
                    traverseNode(child, sb, depth + 1);
                } finally {
                    try {
                        child.recycle();
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
