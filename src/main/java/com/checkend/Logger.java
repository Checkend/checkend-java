package com.checkend;

/**
 * Logger interface for Checkend SDK.
 * Implement this interface to provide custom logging behavior.
 */
public interface Logger {
    /**
     * Log a debug message.
     */
    void debug(String message);

    /**
     * Log an info message.
     */
    void info(String message);

    /**
     * Log a warning message.
     */
    void warn(String message);

    /**
     * Log an error message.
     */
    void error(String message);

    /**
     * Log an error message with an exception.
     */
    void error(String message, Throwable throwable);

    /**
     * Default logger that writes to System.err.
     */
    static Logger defaultLogger() {
        return new DefaultLogger();
    }

    /**
     * No-op logger that discards all messages.
     */
    static Logger nullLogger() {
        return new NullLogger();
    }

    /**
     * Default implementation using System.err.
     */
    class DefaultLogger implements Logger {
        @Override
        public void debug(String message) {
            System.err.println("[Checkend DEBUG] " + message);
        }

        @Override
        public void info(String message) {
            System.err.println("[Checkend INFO] " + message);
        }

        @Override
        public void warn(String message) {
            System.err.println("[Checkend WARN] " + message);
        }

        @Override
        public void error(String message) {
            System.err.println("[Checkend ERROR] " + message);
        }

        @Override
        public void error(String message, Throwable throwable) {
            System.err.println("[Checkend ERROR] " + message);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    /**
     * No-op logger implementation.
     */
    class NullLogger implements Logger {
        @Override
        public void debug(String message) {}

        @Override
        public void info(String message) {}

        @Override
        public void warn(String message) {}

        @Override
        public void error(String message) {}

        @Override
        public void error(String message, Throwable throwable) {}
    }
}
