package com.checkend;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {

    @Test
    void testRequiresApiKey() {
        assertThrows(IllegalStateException.class, () ->
                new Configuration.Builder().build()
        );
    }

    @Test
    @SuppressWarnings("deprecation")
    void testDefaultValues() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .build();

        assertEquals("test-key", config.getApiKey());
        assertEquals("https://app.checkend.com", config.getEndpoint());
        assertEquals(5000, config.getConnectTimeout());
        assertEquals(15000, config.getReadTimeout());
        assertEquals(15000, config.getTimeout()); // Deprecated, returns readTimeout
        assertEquals(1000, config.getMaxQueueSize());
        assertTrue(config.isAsyncSend());
        assertFalse(config.isDebug());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testCustomValues() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .endpoint("https://custom.example.com")
                .environment("staging")
                .enabled(false)
                .asyncSend(false)
                .maxQueueSize(500)
                .connectTimeout(10000)
                .readTimeout(30000)
                .debug(true)
                .build();

        assertEquals("https://custom.example.com", config.getEndpoint());
        assertEquals("staging", config.getEnvironment());
        assertFalse(config.isEnabled());
        assertFalse(config.isAsyncSend());
        assertEquals(500, config.getMaxQueueSize());
        assertEquals(10000, config.getConnectTimeout());
        assertEquals(30000, config.getReadTimeout());
        assertEquals(30000, config.getTimeout()); // Deprecated
        assertTrue(config.isDebug());
    }

    @Test
    void testDefaultFilterKeys() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .build();

        assertTrue(config.getFilterKeys().contains("password"));
        assertTrue(config.getFilterKeys().contains("secret"));
        assertTrue(config.getFilterKeys().contains("api_key"));
        assertTrue(config.getFilterKeys().contains("token"));
        assertTrue(config.getFilterKeys().contains("credit_card"));
    }

    @Test
    void testAddFilterKey() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .addFilterKey("custom_secret")
                .build();

        assertTrue(config.getFilterKeys().contains("custom_secret"));
        assertTrue(config.getFilterKeys().contains("password")); // Still has defaults
    }

    @Test
    void testIgnoredExceptions() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .addIgnoredException(RuntimeException.class)
                .addIgnoredException("CustomException")
                .addIgnoredException(Pattern.compile(".*NotFound.*"))
                .build();

        assertEquals(3, config.getIgnoredExceptions().size());
    }

    @Test
    void testBeforeNotify() {
        Configuration config = new Configuration.Builder()
                .apiKey("test-key")
                .addBeforeNotify(notice -> {
                    notice.getContext().put("test", true);
                    return notice;
                })
                .build();

        assertEquals(1, config.getBeforeNotify().size());
    }
}
