package com.checkend.filters;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Filters exceptions that should be ignored.
 */
public final class IgnoreFilter {
    private IgnoreFilter() {}

    /**
     * Check if an exception should be ignored.
     */
    public static boolean shouldIgnore(Throwable exception, List<Object> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }

        String exceptionClassName = exception.getClass().getName();
        String simpleClassName = exception.getClass().getSimpleName();

        for (Object pattern : patterns) {
            if (matches(exception, exceptionClassName, simpleClassName, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(Throwable exception, String className,
                                    String simpleName, Object pattern) {
        // Match by class
        if (pattern instanceof Class<?> cls) {
            return cls.isInstance(exception);
        }

        // Match by string name
        if (pattern instanceof String name) {
            return className.equals(name) ||
                   simpleName.equals(name) ||
                   className.endsWith("." + name);
        }

        // Match by regex
        if (pattern instanceof Pattern regex) {
            return regex.matcher(className).matches() ||
                   regex.matcher(simpleName).matches();
        }

        return false;
    }
}
