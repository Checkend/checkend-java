package com.checkend;

import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckendTest {

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

    @Test
    void testConfigure() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .endpoint("https://test.example.com")
                .environment("test")
                .enabled(true));

        assertTrue(Checkend.isConfigured());
        assertEquals("test-key", Checkend.getConfiguration().getApiKey());
        assertEquals("https://test.example.com", Checkend.getConfiguration().getEndpoint());
    }

    @Test
    void testNotifyCaptures() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertTrue(Testing.hasNotices());
        assertEquals(1, Testing.noticeCount());

        Notice notice = Testing.lastNotice();
        assertEquals("java.lang.RuntimeException", notice.getErrorClass());
        assertEquals("Test error", notice.getMessage());
    }

    @Test
    void testNotifyWithOptions() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e, Map.of(
                    "fingerprint", "custom-fingerprint",
                    "tags", java.util.List.of("critical", "backend"),
                    "context", Map.of("order_id", 123)
            ));
        }

        Notice notice = Testing.lastNotice();
        assertEquals("custom-fingerprint", notice.getFingerprint());
        assertEquals(java.util.List.of("critical", "backend"), notice.getTags());
        assertEquals(123, notice.getContext().get("order_id"));
    }

    @Test
    void testContextManagement() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true));

        Checkend.setContext(Map.of("key1", "value1"));
        Checkend.setUser(Map.of("id", 42));
        Checkend.setRequest(Map.of("url", "/test"));

        assertEquals("value1", Checkend.getContext().get("key1"));
        assertEquals(42, Checkend.getUser().get("id"));
        assertEquals("/test", Checkend.getRequest().get("url"));

        Checkend.clear();

        assertTrue(Checkend.getContext().isEmpty());
        assertTrue(Checkend.getUser().isEmpty());
        assertTrue(Checkend.getRequest().isEmpty());
    }

    @Test
    void testContextIncludedInNotice() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true));

        Checkend.setContext(Map.of("feature", "checkout"));
        Checkend.setUser(Map.of("id", 123, "email", "test@example.com"));
        Checkend.setRequest(Map.of("url", "/checkout", "method", "POST"));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertEquals("checkout", notice.getContext().get("feature"));
        assertEquals(123, notice.getUser().get("id"));
        assertEquals("/checkout", notice.getRequest().get("url"));
    }

    @Test
    void testDisabledDoesNotCapture() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(false));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertFalse(Testing.hasNotices());
    }

    @Test
    void testIgnoredException() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .addIgnoredException(IllegalArgumentException.class));

        try {
            throw new IllegalArgumentException("Ignored error");
        } catch (IllegalArgumentException e) {
            Checkend.notify(e);
        }

        assertFalse(Testing.hasNotices());

        // Non-ignored exception should be captured
        try {
            throw new RuntimeException("Not ignored");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertTrue(Testing.hasNotices());
    }

    @Test
    void testBeforeNotifyCallback() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .addBeforeNotify(notice -> {
                    notice.getContext().put("added_by_callback", true);
                    return notice;
                }));

        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        Notice notice = Testing.lastNotice();
        assertEquals(true, notice.getContext().get("added_by_callback"));
    }

    @Test
    void testBeforeNotifyCanFilterNotice() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true)
                .addBeforeNotify(notice -> {
                    if (notice.getMessage().contains("skip")) {
                        return false;
                    }
                    return true;
                }));

        try {
            throw new RuntimeException("skip this error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertFalse(Testing.hasNotices());

        try {
            throw new RuntimeException("regular error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertTrue(Testing.hasNotices());
    }

    @Test
    void testReset() {
        Checkend.configure(builder -> builder
                .apiKey("test-key")
                .enabled(true));

        assertTrue(Checkend.isConfigured());

        Checkend.reset();

        assertFalse(Checkend.isConfigured());
    }
}
