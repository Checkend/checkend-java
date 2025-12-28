package com.checkend;

import com.checkend.filters.SanitizeFilter;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SanitizeFilterTest {

    private static final Set<String> DEFAULT_KEYS = Set.of(
            "password", "secret", "api_key", "token", "authorization"
    );

    @Test
    void testFiltersPassword() {
        Map<String, Object> data = new HashMap<>();
        data.put("username", "john");
        data.put("password", "secret123");

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertEquals("john", result.get("username"));
        assertEquals("[FILTERED]", result.get("password"));
    }

    @Test
    void testCaseInsensitive() {
        Map<String, Object> data = new HashMap<>();
        data.put("PASSWORD", "secret");
        data.put("Password", "secret");
        data.put("user_password", "secret");

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertEquals("[FILTERED]", result.get("PASSWORD"));
        assertEquals("[FILTERED]", result.get("Password"));
        assertEquals("[FILTERED]", result.get("user_password"));
    }

    @Test
    void testSubstringMatching() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_api_key_id", "key123");
        data.put("auth_token_value", "token123");
        data.put("secret_data", "data");

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertEquals("[FILTERED]", result.get("user_api_key_id"));
        assertEquals("[FILTERED]", result.get("auth_token_value"));
        assertEquals("[FILTERED]", result.get("secret_data"));
    }

    @Test
    void testNestedMaps() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("password", "nested_secret");
        nested.put("name", "test");

        Map<String, Object> data = new HashMap<>();
        data.put("user", nested);

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultNested = (Map<String, Object>) result.get("user");
        assertEquals("[FILTERED]", resultNested.get("password"));
        assertEquals("test", resultNested.get("name"));
    }

    @Test
    void testLists() {
        Map<String, Object> item1 = new HashMap<>();
        item1.put("password", "pass1");
        item1.put("id", 1);

        Map<String, Object> item2 = new HashMap<>();
        item2.put("password", "pass2");
        item2.put("id", 2);

        Map<String, Object> data = new HashMap<>();
        data.put("items", List.of(item1, item2));

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultItems = (List<Map<String, Object>>) result.get("items");
        assertEquals("[FILTERED]", resultItems.get(0).get("password"));
        assertEquals(1, resultItems.get(0).get("id"));
        assertEquals("[FILTERED]", resultItems.get(1).get("password"));
        assertEquals(2, resultItems.get(1).get("id"));
    }

    @Test
    void testTruncatesLongStrings() {
        String longString = "a".repeat(15000);
        Map<String, Object> data = new HashMap<>();
        data.put("description", longString);

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        String resultString = (String) result.get("description");
        assertEquals(10000, resultString.length());
    }

    @Test
    void testHandlesNull() {
        Map<String, Object> data = new HashMap<>();
        data.put("value", null);

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertNull(result.get("value"));
    }

    @Test
    void testEmptyMap() {
        Map<String, Object> result = SanitizeFilter.filter(new HashMap<>(), DEFAULT_KEYS);
        assertTrue(result.isEmpty());
    }

    @Test
    void testNullMap() {
        Map<String, Object> result = SanitizeFilter.filter(null, DEFAULT_KEYS);
        assertTrue(result.isEmpty());
    }

    @Test
    void testCustomFilterKeys() {
        Set<String> customKeys = Set.of("custom_field");
        Map<String, Object> data = new HashMap<>();
        data.put("custom_field", "secret");
        data.put("password", "not_filtered");

        Map<String, Object> result = SanitizeFilter.filter(data, customKeys);

        assertEquals("[FILTERED]", result.get("custom_field"));
        assertEquals("not_filtered", result.get("password"));
    }

    @Test
    void testPreservesNumbers() {
        Map<String, Object> data = new HashMap<>();
        data.put("count", 42);
        data.put("price", 19.99);

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertEquals(42, result.get("count"));
        assertEquals(19.99, result.get("price"));
    }

    @Test
    void testPreservesBooleans() {
        Map<String, Object> data = new HashMap<>();
        data.put("active", true);
        data.put("deleted", false);

        Map<String, Object> result = SanitizeFilter.filter(data, DEFAULT_KEYS);

        assertEquals(true, result.get("active"));
        assertEquals(false, result.get("deleted"));
    }
}
