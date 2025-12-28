package com.checkend;

import com.checkend.filters.SanitizeFilter;

import java.time.Instant;
import java.util.*;

/**
 * Builds Notice objects from exceptions.
 */
public final class NoticeBuilder {
    private static final int MAX_MESSAGE_LENGTH = 10000;
    private static final int MAX_BACKTRACE_LINES = 100;
    private static final String SDK_VERSION = "0.1.0";

    private final Configuration config;

    public NoticeBuilder(Configuration config) {
        this.config = config;
    }

    /**
     * Build a notice from an exception.
     */
    public Notice build(Throwable exception) {
        return build(exception, null);
    }

    /**
     * Build a notice from an exception with options.
     */
    public Notice build(Throwable exception, Map<String, Object> options) {
        if (options == null) {
            options = Collections.emptyMap();
        }

        Notice notice = new Notice();
        notice.setErrorClass(exception.getClass().getName());
        notice.setMessage(truncateMessage(exception.getMessage()));
        notice.setBacktrace(buildBacktrace(exception));
        notice.setEnvironment(config.getEnvironment());
        notice.setOccurredAt(Instant.now());
        notice.setNotifier(buildNotifier());

        // Apply options
        if (options.containsKey("fingerprint")) {
            notice.setFingerprint((String) options.get("fingerprint"));
        }

        if (options.containsKey("tags")) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) options.get("tags");
            notice.setTags(tags);
        }

        // Context: respect sendContextData toggle
        if (config.isSendContextData()) {
            Map<String, Object> mergedContext = new HashMap<>(Checkend.getContext());
            if (options.containsKey("context")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> optContext = (Map<String, Object>) options.get("context");
                mergedContext.putAll(optContext);
            }
            notice.setContext(SanitizeFilter.filter(mergedContext, config.getFilterKeys()));
        }

        // Request: respect sendRequestData toggle
        if (config.isSendRequestData()) {
            Map<String, Object> mergedRequest = new HashMap<>(Checkend.getRequest());
            if (options.containsKey("request")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> optRequest = (Map<String, Object>) options.get("request");
                mergedRequest.putAll(optRequest);
            }
            notice.setRequest(SanitizeFilter.filter(mergedRequest, config.getFilterKeys()));
        }

        // User: respect sendUserData toggle
        if (config.isSendUserData()) {
            Map<String, Object> mergedUser = new HashMap<>(Checkend.getUser());
            if (options.containsKey("user")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> optUser = (Map<String, Object>) options.get("user");
                mergedUser.putAll(optUser);
            }
            notice.setUser(SanitizeFilter.filter(mergedUser, config.getFilterKeys()));
        }

        return notice;
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return message.substring(0, MAX_MESSAGE_LENGTH);
        }
        return message;
    }

    private List<Map<String, Object>> buildBacktrace(Throwable exception) {
        List<Map<String, Object>> backtrace = new ArrayList<>();
        StackTraceElement[] stackTrace = exception.getStackTrace();

        int limit = Math.min(stackTrace.length, MAX_BACKTRACE_LINES);
        for (int i = 0; i < limit; i++) {
            StackTraceElement element = stackTrace[i];
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("file", element.getFileName() != null ? element.getFileName() : "Unknown");
            frame.put("line", element.getLineNumber());
            frame.put("method", element.getClassName() + "." + element.getMethodName());
            backtrace.add(frame);
        }

        return backtrace;
    }

    private Map<String, String> buildNotifier() {
        Map<String, String> notifier = new LinkedHashMap<>();
        notifier.put("name", "checkend-java");
        notifier.put("version", SDK_VERSION);
        notifier.put("language", "java");
        notifier.put("language_version", System.getProperty("java.version"));

        // Add app metadata if configured
        String appName = config.getAppName();
        if (appName != null && !appName.isEmpty()) {
            notifier.put("app_name", appName);
        }

        String revision = config.getRevision();
        if (revision != null && !revision.isEmpty()) {
            notifier.put("revision", revision);
        }

        return notifier;
    }
}
