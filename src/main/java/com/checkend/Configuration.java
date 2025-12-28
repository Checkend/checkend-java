package com.checkend;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Configuration for the Checkend SDK.
 * Use the Builder to create instances.
 */
public final class Configuration {
    private static final String DEFAULT_ENDPOINT = "https://app.checkend.com";
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 15000;
    private static final int DEFAULT_MAX_QUEUE_SIZE = 1000;
    private static final int DEFAULT_SHUTDOWN_TIMEOUT = 5000;
    private static final Set<String> DEFAULT_FILTER_KEYS = Set.of(
        "password", "password_confirmation", "secret", "secret_key",
        "api_key", "apikey", "access_token", "auth_token", "authorization",
        "token", "credit_card", "card_number", "cvv", "cvc", "ssn", "social_security"
    );

    // Core settings
    private final String apiKey;
    private final String endpoint;
    private final String environment;
    private final boolean enabled;
    private final boolean asyncSend;
    private final int maxQueueSize;
    private final boolean debug;

    // Timeout settings
    private final int connectTimeout;
    private final int readTimeout;
    private final int shutdownTimeout;

    // Proxy settings
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;

    // Logger
    private final Logger logger;

    // App metadata
    private final String appName;
    private final String revision;

    // Data send toggles
    private final boolean sendRequestData;
    private final boolean sendUserData;
    private final boolean sendContextData;

    // Filters and callbacks
    private final Set<String> filterKeys;
    private final List<Object> ignoredExceptions;
    private final List<Function<Notice, Object>> beforeNotify;

    private Configuration(Builder builder) {
        // Core settings
        this.apiKey = builder.apiKey;
        this.endpoint = builder.endpoint;
        this.environment = builder.environment;
        this.enabled = builder.enabled;
        this.asyncSend = builder.asyncSend;
        this.maxQueueSize = builder.maxQueueSize;
        this.debug = builder.debug;

        // Timeout settings
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.shutdownTimeout = builder.shutdownTimeout;

        // Proxy settings
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;
        this.proxyUsername = builder.proxyUsername;
        this.proxyPassword = builder.proxyPassword;

        // Logger
        this.logger = builder.logger;

        // App metadata
        this.appName = builder.appName;
        this.revision = builder.revision;

        // Data send toggles
        this.sendRequestData = builder.sendRequestData;
        this.sendUserData = builder.sendUserData;
        this.sendContextData = builder.sendContextData;

        // Filters and callbacks
        this.filterKeys = Collections.unmodifiableSet(builder.filterKeys);
        this.ignoredExceptions = Collections.unmodifiableList(builder.ignoredExceptions);
        this.beforeNotify = Collections.unmodifiableList(builder.beforeNotify);
    }

    // Core getters
    public String getApiKey() { return apiKey; }
    public String getEndpoint() { return endpoint; }
    public String getEnvironment() { return environment; }
    public boolean isEnabled() { return enabled; }
    public boolean isAsyncSend() { return asyncSend; }
    public int getMaxQueueSize() { return maxQueueSize; }
    public boolean isDebug() { return debug; }

    // Timeout getters
    public int getConnectTimeout() { return connectTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getShutdownTimeout() { return shutdownTimeout; }

    /**
     * @deprecated Use {@link #getConnectTimeout()} and {@link #getReadTimeout()} instead.
     */
    @Deprecated
    public int getTimeout() { return readTimeout; }

    // Proxy getters
    public String getProxyHost() { return proxyHost; }
    public int getProxyPort() { return proxyPort; }
    public String getProxyUsername() { return proxyUsername; }
    public String getProxyPassword() { return proxyPassword; }
    public boolean hasProxy() { return proxyHost != null && !proxyHost.isEmpty(); }

    // Logger getter
    public Logger getLogger() { return logger; }

    // App metadata getters
    public String getAppName() { return appName; }
    public String getRevision() { return revision; }

    // Data send toggle getters
    public boolean isSendRequestData() { return sendRequestData; }
    public boolean isSendUserData() { return sendUserData; }
    public boolean isSendContextData() { return sendContextData; }

    // Filter and callback getters
    public Set<String> getFilterKeys() { return filterKeys; }
    public List<Object> getIgnoredExceptions() { return ignoredExceptions; }
    public List<Function<Notice, Object>> getBeforeNotify() { return beforeNotify; }

    /**
     * Builder for Configuration.
     */
    public static class Builder {
        // Core settings
        private String apiKey;
        private String endpoint = DEFAULT_ENDPOINT;
        private String environment;
        private boolean enabled = true;
        private boolean asyncSend = true;
        private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
        private boolean debug = false;

        // Timeout settings
        private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private int readTimeout = DEFAULT_READ_TIMEOUT;
        private int shutdownTimeout = DEFAULT_SHUTDOWN_TIMEOUT;

        // Proxy settings
        private String proxyHost;
        private int proxyPort;
        private String proxyUsername;
        private String proxyPassword;

        // Logger
        private Logger logger;

        // App metadata
        private String appName;
        private String revision;

        // Data send toggles
        private boolean sendRequestData = true;
        private boolean sendUserData = true;
        private boolean sendContextData = true;

        // Filters and callbacks
        private Set<String> filterKeys = new HashSet<>(DEFAULT_FILTER_KEYS);
        private List<Object> ignoredExceptions = new ArrayList<>();
        private List<Function<Notice, Object>> beforeNotify = new ArrayList<>();

        public Builder() {
            // Read from environment variables
            String envApiKey = System.getenv("CHECKEND_API_KEY");
            if (envApiKey != null && !envApiKey.isEmpty()) {
                this.apiKey = envApiKey;
            }

            String envEndpoint = System.getenv("CHECKEND_ENDPOINT");
            if (envEndpoint != null && !envEndpoint.isEmpty()) {
                this.endpoint = envEndpoint;
            }

            String envEnvironment = System.getenv("CHECKEND_ENVIRONMENT");
            if (envEnvironment != null && !envEnvironment.isEmpty()) {
                this.environment = envEnvironment;
            } else {
                this.environment = detectEnvironment();
            }

            String envDebug = System.getenv("CHECKEND_DEBUG");
            if ("true".equalsIgnoreCase(envDebug) || "1".equals(envDebug)) {
                this.debug = true;
            }

            // Parse proxy from environment (format: http://user:pass@host:port or host:port)
            String envProxy = System.getenv("CHECKEND_PROXY");
            if (envProxy != null && !envProxy.isEmpty()) {
                parseProxy(envProxy);
            }

            // App metadata from environment
            String envAppName = System.getenv("CHECKEND_APP_NAME");
            if (envAppName != null && !envAppName.isEmpty()) {
                this.appName = envAppName;
            }

            String envRevision = System.getenv("CHECKEND_REVISION");
            if (envRevision != null && !envRevision.isEmpty()) {
                this.revision = envRevision;
            }

            // Auto-enable for production/staging
            String env = this.environment;
            if (env != null) {
                this.enabled = env.equalsIgnoreCase("production") || env.equalsIgnoreCase("staging");
            }
        }

        private String detectEnvironment() {
            // Check common environment variables
            String[] envVars = {"ENVIRONMENT", "ENV", "RAILS_ENV", "NODE_ENV", "APP_ENV"};
            for (String var : envVars) {
                String value = System.getenv(var);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
            return "development";
        }

        private void parseProxy(String proxyString) {
            try {
                String s = proxyString;
                // Remove protocol if present
                if (s.startsWith("http://")) {
                    s = s.substring(7);
                } else if (s.startsWith("https://")) {
                    s = s.substring(8);
                }

                // Check for auth
                int atIndex = s.lastIndexOf('@');
                if (atIndex > 0) {
                    String auth = s.substring(0, atIndex);
                    s = s.substring(atIndex + 1);
                    int colonIndex = auth.indexOf(':');
                    if (colonIndex > 0) {
                        this.proxyUsername = auth.substring(0, colonIndex);
                        this.proxyPassword = auth.substring(colonIndex + 1);
                    } else {
                        this.proxyUsername = auth;
                    }
                }

                // Parse host:port
                int colonIndex = s.lastIndexOf(':');
                if (colonIndex > 0) {
                    this.proxyHost = s.substring(0, colonIndex);
                    this.proxyPort = Integer.parseInt(s.substring(colonIndex + 1));
                } else {
                    this.proxyHost = s;
                    this.proxyPort = 8080; // Default proxy port
                }
            } catch (Exception e) {
                // Invalid proxy format, ignore
            }
        }

        // Core setters
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder asyncSend(boolean asyncSend) {
            this.asyncSend = asyncSend;
            return this;
        }

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        // Timeout setters
        public Builder connectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder shutdownTimeout(int shutdownTimeout) {
            this.shutdownTimeout = shutdownTimeout;
            return this;
        }

        /**
         * @deprecated Use {@link #connectTimeout(int)} and {@link #readTimeout(int)} instead.
         */
        @Deprecated
        public Builder timeout(int timeout) {
            this.connectTimeout = timeout;
            this.readTimeout = timeout;
            return this;
        }

        // Proxy setters
        public Builder proxyHost(String proxyHost) {
            this.proxyHost = proxyHost;
            return this;
        }

        public Builder proxyPort(int proxyPort) {
            this.proxyPort = proxyPort;
            return this;
        }

        public Builder proxyUsername(String proxyUsername) {
            this.proxyUsername = proxyUsername;
            return this;
        }

        public Builder proxyPassword(String proxyPassword) {
            this.proxyPassword = proxyPassword;
            return this;
        }

        public Builder proxy(String host, int port) {
            this.proxyHost = host;
            this.proxyPort = port;
            return this;
        }

        public Builder proxy(String host, int port, String username, String password) {
            this.proxyHost = host;
            this.proxyPort = port;
            this.proxyUsername = username;
            this.proxyPassword = password;
            return this;
        }

        // Logger setter
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        // App metadata setters
        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder revision(String revision) {
            this.revision = revision;
            return this;
        }

        // Data send toggle setters
        public Builder sendRequestData(boolean sendRequestData) {
            this.sendRequestData = sendRequestData;
            return this;
        }

        public Builder sendUserData(boolean sendUserData) {
            this.sendUserData = sendUserData;
            return this;
        }

        public Builder sendContextData(boolean sendContextData) {
            this.sendContextData = sendContextData;
            return this;
        }

        // Filter and callback setters
        public Builder addFilterKey(String key) {
            this.filterKeys.add(key);
            return this;
        }

        public Builder addFilterKeys(Collection<String> keys) {
            this.filterKeys.addAll(keys);
            return this;
        }

        public Builder addIgnoredException(Class<? extends Throwable> exceptionClass) {
            this.ignoredExceptions.add(exceptionClass);
            return this;
        }

        public Builder addIgnoredException(String exceptionName) {
            this.ignoredExceptions.add(exceptionName);
            return this;
        }

        public Builder addIgnoredException(Pattern pattern) {
            this.ignoredExceptions.add(pattern);
            return this;
        }

        public Builder addBeforeNotify(Function<Notice, Object> callback) {
            this.beforeNotify.add(callback);
            return this;
        }

        public Configuration build() {
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("API key is required");
            }

            // Default logger if not set
            if (logger == null) {
                logger = debug ? Logger.defaultLogger() : Logger.nullLogger();
            }

            return new Configuration(this);
        }
    }
}
