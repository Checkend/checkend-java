package com.checkend;

import com.checkend.filters.IgnoreFilter;

import java.util.*;
import java.util.function.Function;

/**
 * Main entry point for the Checkend Java SDK.
 */
public final class Checkend {
    private static volatile Configuration config;
    private static volatile Client client;
    private static volatile Worker worker;
    private static volatile NoticeBuilder noticeBuilder;

    // Thread-local context storage
    private static final ThreadLocal<Map<String, Object>> context =
        ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, Object>> user =
        ThreadLocal.withInitial(HashMap::new);
    private static final ThreadLocal<Map<String, Object>> request =
        ThreadLocal.withInitial(HashMap::new);

    private Checkend() {}

    /**
     * Configure the SDK with a Configuration object.
     */
    public static synchronized void configure(Configuration configuration) {
        config = configuration;
        client = new Client(config);
        worker = new Worker(config, client);
        noticeBuilder = new NoticeBuilder(config);

        config.getLogger().info("Configured with endpoint: " + config.getEndpoint());
    }

    /**
     * Configure the SDK with a builder.
     */
    public static void configure(java.util.function.Consumer<Configuration.Builder> builderConsumer) {
        Configuration.Builder builder = new Configuration.Builder();
        builderConsumer.accept(builder);
        configure(builder.build());
    }

    /**
     * Report an exception asynchronously.
     */
    public static void notify(Throwable exception) {
        notify(exception, null);
    }

    /**
     * Report an exception asynchronously with options.
     */
    public static void notify(Throwable exception, Map<String, Object> options) {
        if (!isConfigured() || !config.isEnabled()) {
            return;
        }

        if (IgnoreFilter.shouldIgnore(exception, config.getIgnoredExceptions())) {
            config.getLogger().debug("Ignoring exception: " + exception.getClass().getName());
            return;
        }

        Notice notice = noticeBuilder.build(exception, options);

        // Run before_notify callbacks
        for (Function<Notice, Object> callback : config.getBeforeNotify()) {
            Object result = callback.apply(notice);
            if (result instanceof Boolean && !((Boolean) result)) {
                config.getLogger().debug("Notice filtered by before_notify callback");
                return;
            }
            if (result instanceof Notice) {
                notice = (Notice) result;
            }
        }

        // Testing mode
        if (Testing.isTestingMode()) {
            Testing.capture(notice);
            return;
        }

        // Queue for async sending
        if (config.isAsyncSend()) {
            worker.enqueue(notice);
        } else {
            client.send(notice);
        }
    }

    /**
     * Report an exception synchronously and return the response.
     */
    public static Client.Response notifySync(Throwable exception) {
        return notifySync(exception, null);
    }

    /**
     * Report an exception synchronously with options and return the response.
     */
    public static Client.Response notifySync(Throwable exception, Map<String, Object> options) {
        if (!isConfigured() || !config.isEnabled()) {
            return new Client.Response(0, "SDK not configured or disabled", null);
        }

        if (IgnoreFilter.shouldIgnore(exception, config.getIgnoredExceptions())) {
            return new Client.Response(0, "Exception ignored", null);
        }

        Notice notice = noticeBuilder.build(exception, options);

        // Run before_notify callbacks
        for (Function<Notice, Object> callback : config.getBeforeNotify()) {
            Object result = callback.apply(notice);
            if (result instanceof Boolean && !((Boolean) result)) {
                return new Client.Response(0, "Filtered by before_notify", null);
            }
            if (result instanceof Notice) {
                notice = (Notice) result;
            }
        }

        // Testing mode
        if (Testing.isTestingMode()) {
            Testing.capture(notice);
            return new Client.Response(200, "Captured in testing mode", null);
        }

        return client.send(notice);
    }

    // Context management

    /**
     * Set custom context for the current thread.
     */
    public static void setContext(Map<String, Object> ctx) {
        context.get().putAll(ctx);
    }

    /**
     * Get the current context.
     */
    public static Map<String, Object> getContext() {
        return new HashMap<>(context.get());
    }

    /**
     * Set user information for the current thread.
     */
    public static void setUser(Map<String, Object> usr) {
        user.get().putAll(usr);
    }

    /**
     * Get the current user information.
     */
    public static Map<String, Object> getUser() {
        return new HashMap<>(user.get());
    }

    /**
     * Set request information for the current thread.
     */
    public static void setRequest(Map<String, Object> req) {
        request.get().putAll(req);
    }

    /**
     * Get the current request information.
     */
    public static Map<String, Object> getRequest() {
        return new HashMap<>(request.get());
    }

    /**
     * Clear all context for the current thread.
     */
    public static void clear() {
        context.remove();
        user.remove();
        request.remove();
    }

    /**
     * Wait for all pending notices to be sent.
     */
    public static void flush() {
        if (worker != null) {
            worker.flush();
        }
    }

    /**
     * Stop the worker.
     */
    public static void stop() {
        if (worker != null) {
            worker.stop();
        }
    }

    /**
     * Reset all state (useful for testing).
     */
    public static synchronized void reset() {
        if (worker != null) {
            worker.stop();
        }
        config = null;
        client = null;
        worker = null;
        noticeBuilder = null;
        clear();
    }

    /**
     * Check if the SDK is configured.
     */
    public static boolean isConfigured() {
        return config != null;
    }

    /**
     * Get the current configuration (for testing/debugging).
     */
    public static Configuration getConfiguration() {
        return config;
    }

    /**
     * Get the logger from the current configuration.
     * Returns a null logger if not configured.
     */
    public static Logger getLogger() {
        return config != null ? config.getLogger() : Logger.nullLogger();
    }
}
