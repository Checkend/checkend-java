package com.checkend;

import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for new SDK features added in 1.0.0.
 */
class NewFeaturesTest {

    @BeforeEach
    void setUp() {
        Checkend.reset();
        Testing.setup();
    }

    @AfterEach
    void tearDown() {
        Testing.teardown();
        Checkend.reset();
    }

    // ========== Timeout Configuration Tests ==========

    @Test
    void testSeparateTimeouts() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .connectTimeout(3000)
                .readTimeout(10000)
                .build();

        assertEquals(3000, config.getConnectTimeout());
        assertEquals(10000, config.getReadTimeout());
    }

    @Test
    void testDefaultTimeouts() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .build();

        assertEquals(5000, config.getConnectTimeout());
        assertEquals(15000, config.getReadTimeout());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDeprecatedTimeoutSetsBoth() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .timeout(20000)
                .build();

        assertEquals(20000, config.getConnectTimeout());
        assertEquals(20000, config.getReadTimeout());
        assertEquals(20000, config.getTimeout()); // Deprecated getter
    }

    @Test
    void testShutdownTimeout() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .shutdownTimeout(10000)
                .build();

        assertEquals(10000, config.getShutdownTimeout());
    }

    // ========== Proxy Configuration Tests ==========

    @Test
    void testProxyConfiguration() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .proxyHost("proxy.example.com")
                .proxyPort(8080)
                .build();

        assertTrue(config.hasProxy());
        assertEquals("proxy.example.com", config.getProxyHost());
        assertEquals(8080, config.getProxyPort());
    }

    @Test
    void testProxyWithAuth() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .proxy("proxy.example.com", 8080, "user", "pass")
                .build();

        assertTrue(config.hasProxy());
        assertEquals("proxy.example.com", config.getProxyHost());
        assertEquals(8080, config.getProxyPort());
        assertEquals("user", config.getProxyUsername());
        assertEquals("pass", config.getProxyPassword());
    }

    @Test
    void testNoProxy() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .build();

        assertFalse(config.hasProxy());
    }

    // ========== Logger Tests ==========

    @Test
    void testCustomLogger() {
        List<String> logs = new ArrayList<>();
        Logger customLogger = new Logger() {
            @Override
            public void debug(String message) { logs.add("DEBUG: " + message); }
            @Override
            public void info(String message) { logs.add("INFO: " + message); }
            @Override
            public void warn(String message) { logs.add("WARN: " + message); }
            @Override
            public void error(String message) { logs.add("ERROR: " + message); }
            @Override
            public void error(String message, Throwable t) { logs.add("ERROR: " + message); }
        };

        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .logger(customLogger));

        // Configuration logs an info message
        assertTrue(logs.stream().anyMatch(l -> l.contains("INFO")));
    }

    @Test
    void testDefaultLoggerWhenDebug() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .debug(true)
                .build();

        assertNotNull(config.getLogger());
        assertTrue(config.getLogger() instanceof Logger.DefaultLogger);
    }

    @Test
    void testNullLoggerWhenNotDebug() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .debug(false)
                .build();

        assertNotNull(config.getLogger());
        assertTrue(config.getLogger() instanceof Logger.NullLogger);
    }

    @Test
    void testStaticLoggerFactories() {
        Logger defaultLogger = Logger.defaultLogger();
        Logger nullLogger = Logger.nullLogger();

        assertNotNull(defaultLogger);
        assertNotNull(nullLogger);
        assertTrue(defaultLogger instanceof Logger.DefaultLogger);
        assertTrue(nullLogger instanceof Logger.NullLogger);
    }

    // ========== App Metadata Tests ==========

    @Test
    void testAppMetadata() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .appName("my-app")
                .revision("abc123")
                .build();

        assertEquals("my-app", config.getAppName());
        assertEquals("abc123", config.getRevision());
    }

    @Test
    void testAppMetadataInNotice() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .appName("my-app")
                .revision("v1.2.3"));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertEquals("my-app", notice.getNotifier().get("app_name"));
        assertEquals("v1.2.3", notice.getNotifier().get("revision"));
    }

    // ========== Data Send Toggles Tests ==========

    @Test
    void testDataSendTogglesDefaults() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .build();

        assertTrue(config.isSendRequestData());
        assertTrue(config.isSendUserData());
        assertTrue(config.isSendContextData());
    }

    @Test
    void testDisableSendRequestData() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .sendRequestData(false));

        Checkend.setRequest(Map.of("url", "/test"));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertTrue(notice.getRequest().isEmpty());
    }

    @Test
    void testDisableSendUserData() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .sendUserData(false));

        Checkend.setUser(Map.of("id", 123));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertTrue(notice.getUser().isEmpty());
    }

    @Test
    void testDisableSendContextData() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .sendContextData(false));

        Checkend.setContext(Map.of("key", "value"));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertTrue(notice.getContext().isEmpty());
    }

    // ========== Response Tests ==========

    @Test
    void testResponseRateLimitDetection() {
        Client.Response rateLimited = new Client.Response(429, "Too many requests", "60");
        Client.Response success = new Client.Response(200, "OK", null);
        Client.Response error = new Client.Response(500, "Server error", null);

        assertTrue(rateLimited.isRateLimited());
        assertFalse(success.isRateLimited());
        assertFalse(error.isRateLimited());
    }

    @Test
    void testResponseRetryAfterParsing() {
        Client.Response withRetryAfter = new Client.Response(429, "Too many requests", "30");
        Client.Response withoutRetryAfter = new Client.Response(429, "Too many requests", null);
        Client.Response invalidRetryAfter = new Client.Response(429, "Too many requests", "invalid");

        assertEquals(30000, withRetryAfter.getRetryAfterMs(60000));
        assertEquals(60000, withoutRetryAfter.getRetryAfterMs(60000));
        assertEquals(60000, invalidRetryAfter.getRetryAfterMs(60000));
    }

    // ========== Checkend.getLogger() Tests ==========

    @Test
    void testGetLoggerBeforeConfigure() {
        Logger logger = Checkend.getLogger();
        assertNotNull(logger);
        assertTrue(logger instanceof Logger.NullLogger);
    }

    @Test
    void testGetLoggerAfterConfigure() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .debug(true));

        Logger logger = Checkend.getLogger();
        assertNotNull(logger);
        assertTrue(logger instanceof Logger.DefaultLogger);
    }
}
