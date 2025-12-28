package com.checkend.filters;

import java.util.*;

/**
 * Filters sensitive data from maps and objects.
 */
public final class SanitizeFilter {
    private static final String FILTERED = "[FILTERED]";
    private static final int MAX_DEPTH = 10;
    private static final int MAX_STRING_LENGTH = 10000;

    private SanitizeFilter() {}

    /**
     * Filter sensitive data from a map.
     */
    public static Map<String, Object> filter(Map<String, Object> data, Set<String> filterKeys) {
        if (data == null || data.isEmpty()) {
            return new HashMap<>();
        }
        return filterMap(data, filterKeys, 0, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> filterMap(Map<String, Object> data, Set<String> filterKeys,
                                                  int depth, IdentityHashMap<Object, Boolean> seen) {
        if (depth > MAX_DEPTH) {
            return Collections.singletonMap("_truncated", "max depth exceeded");
        }

        if (seen.containsKey(data)) {
            return Collections.singletonMap("_circular", "circular reference");
        }
        seen.put(data, true);

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (shouldFilter(key, filterKeys)) {
                result.put(key, FILTERED);
            } else {
                result.put(key, filterValue(value, filterKeys, depth + 1, seen));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Object filterValue(Object value, Set<String> filterKeys,
                                       int depth, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) {
            return null;
        }

        if (value instanceof String s) {
            return truncateString(s);
        }

        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Map) {
            return filterMap((Map<String, Object>) value, filterKeys, depth, seen);
        }

        if (value instanceof List<?> list) {
            if (seen.containsKey(list)) {
                return Collections.singletonList("_circular: circular reference");
            }
            seen.put(list, true);

            List<Object> result = new ArrayList<>();
            for (Object item : list) {
                result.add(filterValue(item, filterKeys, depth + 1, seen));
            }
            return result;
        }

        return truncateString(value.toString());
    }

    private static boolean shouldFilter(String key, Set<String> filterKeys) {
        if (key == null) {
            return false;
        }
        String lowerKey = key.toLowerCase();
        for (String filterKey : filterKeys) {
            if (lowerKey.contains(filterKey.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String truncateString(String s) {
        if (s.length() > MAX_STRING_LENGTH) {
            return s.substring(0, MAX_STRING_LENGTH);
        }
        return s;
    }
}
