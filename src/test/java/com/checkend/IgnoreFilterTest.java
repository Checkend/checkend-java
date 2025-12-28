package com.checkend;

import com.checkend.filters.IgnoreFilter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class IgnoreFilterTest {

    @Test
    void testIgnoresByClass() {
        RuntimeException exception = new RuntimeException("test");

        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of(RuntimeException.class)));
        assertFalse(IgnoreFilter.shouldIgnore(exception, List.of(IllegalArgumentException.class)));
    }

    @Test
    void testIgnoresByStringName() {
        RuntimeException exception = new RuntimeException("test");

        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of("RuntimeException")));
        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of("java.lang.RuntimeException")));
        assertFalse(IgnoreFilter.shouldIgnore(exception, List.of("IllegalArgumentException")));
    }

    @Test
    void testIgnoresByRegex() {
        RuntimeException exception = new RuntimeException("test");

        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of(Pattern.compile(".*Runtime.*"))));
        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of(Pattern.compile(".*Exception"))));
        assertFalse(IgnoreFilter.shouldIgnore(exception, List.of(Pattern.compile("Custom.*"))));
    }

    @Test
    void testIgnoresWithMultiplePatterns() {
        assertTrue(IgnoreFilter.shouldIgnore(
                new RuntimeException("test"),
                List.of(RuntimeException.class, IllegalArgumentException.class)));

        assertTrue(IgnoreFilter.shouldIgnore(
                new IllegalArgumentException("test"),
                List.of(RuntimeException.class, IllegalArgumentException.class)));

        // NullPointerException is a subclass of RuntimeException, so it matches
        assertTrue(IgnoreFilter.shouldIgnore(
                new NullPointerException("test"),
                List.of(RuntimeException.class, IllegalArgumentException.class)));

        // Error is NOT a subclass of RuntimeException, so it should not match
        assertFalse(IgnoreFilter.shouldIgnore(
                new Error("test"),
                List.of(RuntimeException.class, IllegalArgumentException.class)));
    }

    @Test
    void testEmptyPatternsList() {
        assertFalse(IgnoreFilter.shouldIgnore(new RuntimeException("test"), List.of()));
    }

    @Test
    void testNullPatternsList() {
        assertFalse(IgnoreFilter.shouldIgnore(new RuntimeException("test"), null));
    }

    @Test
    void testIgnoresSubclasses() {
        // IllegalArgumentException is a subclass of RuntimeException
        IllegalArgumentException exception = new IllegalArgumentException("test");

        // Should match by class inheritance
        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of(RuntimeException.class)));
    }

    @Test
    void testMixedPatternTypes() {
        RuntimeException exception = new RuntimeException("test");

        assertTrue(IgnoreFilter.shouldIgnore(exception, List.of(
                IllegalArgumentException.class,
                "RuntimeException",
                Pattern.compile("NotMatching.*")
        )));
    }
}
