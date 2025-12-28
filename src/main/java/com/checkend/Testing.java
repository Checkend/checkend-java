package com.checkend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Testing utilities for capturing notices without sending them.
 */
public final class Testing {
    private static final List<Notice> notices = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean testingMode = false;

    private Testing() {}

    /**
     * Enable testing mode. Notices will be captured instead of sent.
     */
    public static void setup() {
        testingMode = true;
        notices.clear();
    }

    /**
     * Disable testing mode.
     */
    public static void teardown() {
        testingMode = false;
        notices.clear();
    }

    /**
     * Check if testing mode is enabled.
     */
    public static boolean isTestingMode() {
        return testingMode;
    }

    /**
     * Capture a notice (called by Checkend when in testing mode).
     */
    static void capture(Notice notice) {
        if (testingMode) {
            notices.add(notice);
        }
    }

    /**
     * Get all captured notices.
     */
    public static List<Notice> notices() {
        return new ArrayList<>(notices);
    }

    /**
     * Get the last captured notice.
     */
    public static Notice lastNotice() {
        if (notices.isEmpty()) {
            return null;
        }
        return notices.get(notices.size() - 1);
    }

    /**
     * Get the first captured notice.
     */
    public static Notice firstNotice() {
        if (notices.isEmpty()) {
            return null;
        }
        return notices.get(0);
    }

    /**
     * Get the number of captured notices.
     */
    public static int noticeCount() {
        return notices.size();
    }

    /**
     * Check if any notices have been captured.
     */
    public static boolean hasNotices() {
        return !notices.isEmpty();
    }

    /**
     * Clear all captured notices.
     */
    public static void clearNotices() {
        notices.clear();
    }
}
