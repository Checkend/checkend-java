package com.checkend.integrations.spring;

import com.checkend.Checkend;

/**
 * Spring exception handler for automatic error reporting.
 *
 * Use with @ControllerAdvice:
 *
 * <pre>
 * {@code
 * @ControllerAdvice
 * public class GlobalExceptionHandler {
 *
 *     private final CheckendExceptionHandler checkendHandler;
 *
 *     public GlobalExceptionHandler(CheckendExceptionHandler checkendHandler) {
 *         this.checkendHandler = checkendHandler;
 *     }
 *
 *     @ExceptionHandler(Exception.class)
 *     public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
 *         checkendHandler.handle(ex, request);
 *         return ResponseEntity.status(500).body(new ErrorResponse("Internal Server Error"));
 *     }
 * }
 * }
 * </pre>
 */
public class CheckendExceptionHandler {

    /**
     * Handle an exception and report to Checkend.
     */
    public void handle(Exception exception) {
        Checkend.notify(exception);
    }

    /**
     * Handle an exception with request context.
     * Note: You need to set up request context before calling this.
     */
    public void handle(Exception exception, Object request) {
        // Request context should already be set via filter or interceptor
        Checkend.notify(exception);
    }
}
