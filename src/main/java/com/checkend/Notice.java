package com.checkend;

import java.time.Instant;
import java.util.*;

/**
 * Represents an error notice to be sent to Checkend.
 */
public final class Notice {
    private String errorClass;
    private String message;
    private List<Map<String, Object>> backtrace;
    private String fingerprint;
    private List<String> tags;
    private Map<String, Object> context;
    private Map<String, Object> request;
    private Map<String, Object> user;
    private String environment;
    private Instant occurredAt;
    private Map<String, String> notifier;

    public Notice() {
        this.backtrace = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.context = new HashMap<>();
        this.request = new HashMap<>();
        this.user = new HashMap<>();
        this.occurredAt = Instant.now();
        this.notifier = new HashMap<>();
    }

    // Getters and setters
    public String getErrorClass() { return errorClass; }
    public void setErrorClass(String errorClass) { this.errorClass = errorClass; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<Map<String, Object>> getBacktrace() { return backtrace; }
    public void setBacktrace(List<Map<String, Object>> backtrace) { this.backtrace = backtrace; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }

    public Map<String, Object> getRequest() { return request; }
    public void setRequest(Map<String, Object> request) { this.request = request; }

    public Map<String, Object> getUser() { return user; }
    public void setUser(Map<String, Object> user) { this.user = user; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }

    public Map<String, String> getNotifier() { return notifier; }
    public void setNotifier(Map<String, String> notifier) { this.notifier = notifier; }

    /**
     * Convert to JSON-compatible map for serialization.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("error_class", errorClass);
        map.put("message", message);
        map.put("backtrace", backtrace);
        if (fingerprint != null) {
            map.put("fingerprint", fingerprint);
        }
        if (tags != null && !tags.isEmpty()) {
            map.put("tags", tags);
        }
        if (context != null && !context.isEmpty()) {
            map.put("context", context);
        }
        if (request != null && !request.isEmpty()) {
            map.put("request", request);
        }
        if (user != null && !user.isEmpty()) {
            map.put("user", user);
        }
        map.put("environment", environment);
        map.put("occurred_at", occurredAt.toString());
        map.put("notifier", notifier);
        return map;
    }
}
