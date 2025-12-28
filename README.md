# Checkend Java SDK

Java SDK for [Checkend](https://checkend.com) error monitoring. Async by default with Servlet, Spring Boot, Quartz, and Spring Batch integration.

## Features

- **Async by default** - Non-blocking error sending via background worker
- **Servlet Filter** - Easy integration with any Java web app
- **Spring Boot** - Auto-configuration support
- **Job Integrations** - Quartz and Spring Batch support
- **Automatic context** - Request, user, and custom context tracking
- **Sensitive data filtering** - Automatic scrubbing of passwords, tokens, etc.
- **Rate limiting** - Automatic backoff on 429 responses
- **Proxy support** - HTTP proxy with authentication
- **Custom logging** - Pluggable logger interface
- **Testing utilities** - Capture errors in tests without sending

## Requirements

- Java 17+
- No external dependencies (uses java.net.http)

## Installation

### Maven

Add the JitPack repository and dependency:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Checkend</groupId>
        <artifactId>checkend-java</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

### Gradle

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.Checkend:checkend-java:v1.0.0'
}
```

## Quick Start

```java
import com.checkend.Checkend;

// Configure the SDK
Checkend.configure(builder -> builder
    .apiKey("your-api-key")
);

// Report an error
try {
    doSomething();
} catch (Exception e) {
    Checkend.notify(e);
}
```

## Configuration

```java
Checkend.configure(builder -> builder
    // Required
    .apiKey("your-api-key")

    // Connection settings
    .endpoint("https://app.checkend.com")  // Custom endpoint
    .connectTimeout(5000)                  // Connection timeout (default: 5000ms)
    .readTimeout(15000)                    // Read timeout (default: 15000ms)

    // Proxy settings
    .proxy("proxy.example.com", 8080)                    // Basic proxy
    .proxy("proxy.example.com", 8080, "user", "pass")   // With auth

    // Behavior
    .environment("production")            // Auto-detected from env vars
    .enabled(true)                        // Enable/disable SDK
    .asyncSend(true)                      // Async sending (default: true)
    .maxQueueSize(1000)                   // Max queue size
    .shutdownTimeout(5000)                // Graceful shutdown timeout

    // App metadata
    .appName("my-service")                // Application name
    .revision("abc123")                   // Git SHA or version

    // Data controls
    .sendRequestData(true)                // Include request data
    .sendUserData(true)                   // Include user data
    .sendContextData(true)                // Include custom context

    // Filtering
    .addFilterKey("custom_secret")        // Additional keys to filter
    .addIgnoredException(MyException.class)

    // Logging
    .logger(myCustomLogger)               // Custom logger
    .debug(false)                         // Debug logging
);
```

### Environment Variables

```bash
CHECKEND_API_KEY=your-api-key
CHECKEND_ENDPOINT=https://your-server.com
CHECKEND_ENVIRONMENT=production
CHECKEND_DEBUG=true
CHECKEND_PROXY=http://user:pass@proxy.example.com:8080
CHECKEND_APP_NAME=my-service
CHECKEND_REVISION=abc123
```

## Manual Error Reporting

```java
// Basic error reporting
try {
    riskyOperation();
} catch (Exception e) {
    Checkend.notify(e);
}

// With additional context
try {
    processOrder(orderId);
} catch (Exception e) {
    Checkend.notify(e, Map.of(
        "context", Map.of("order_id", orderId),
        "user", Map.of("id", userId, "email", userEmail),
        "tags", List.of("orders", "critical"),
        "fingerprint", "order-processing-error"
    ));
}

// Synchronous sending (blocks until sent)
Client.Response response = Checkend.notifySync(e);
if (response.isSuccess()) {
    System.out.println("Notice sent successfully");
}
```

## Context & User Tracking

```java
// Set context for all errors in this thread
Checkend.setContext(Map.of(
    "order_id", 12345,
    "feature_flag", "new-checkout"
));

// Set user information
Checkend.setUser(Map.of(
    "id", user.getId(),
    "email", user.getEmail(),
    "name", user.getName()
));

// Set request information
Checkend.setRequest(Map.of(
    "url", request.getRequestURI(),
    "method", request.getMethod()
));

// Clear all context (call at end of request)
Checkend.clear();
```

## Servlet Integration

### Using the Servlet Filter

```java
// In your web.xml
<filter>
    <filter-name>checkendFilter</filter-name>
    <filter-class>com.checkend.integrations.servlet.CheckendFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>checkendFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

Or register programmatically:

```java
@WebListener
public class CheckendInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Checkend.configure(builder -> builder
            .apiKey(System.getenv("CHECKEND_API_KEY"))
        );

        FilterRegistration.Dynamic filter = sce.getServletContext()
            .addFilter("checkendFilter", CheckendFilter.class);
        filter.addMappingForUrlPatterns(null, true, "/*");
    }
}
```

## Spring Boot Integration

### Configuration

```java
@Configuration
public class CheckendConfig {

    @Bean
    public CheckendAutoConfiguration checkendAutoConfiguration(
            @Value("${checkend.api-key}") String apiKey,
            @Value("${checkend.endpoint:https://app.checkend.com}") String endpoint,
            @Value("${checkend.environment:#{null}}") String environment,
            @Value("${checkend.enabled:true}") boolean enabled,
            @Value("${checkend.debug:false}") boolean debug) {
        return new CheckendAutoConfiguration(apiKey, endpoint, environment, enabled, debug);
    }

    @Bean
    public CheckendExceptionHandler checkendExceptionHandler() {
        return new CheckendExceptionHandler();
    }
}
```

### Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    private final CheckendExceptionHandler checkendHandler;

    public GlobalExceptionHandler(CheckendExceptionHandler checkendHandler) {
        this.checkendHandler = checkendHandler;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        checkendHandler.handle(ex);
        return ResponseEntity.status(500).body(new ErrorResponse("Internal Server Error"));
    }
}
```

### application.properties

```properties
checkend.api-key=your-api-key
checkend.endpoint=https://app.checkend.com
checkend.environment=production
checkend.enabled=true
checkend.debug=false
```

## Quartz Integration

Automatically capture job context and report job failures:

```java
import com.checkend.integrations.quartz.CheckendJobListener;

// Register the listener with your scheduler
Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
scheduler.getListenerManager().addJobListener(new CheckendJobListener());

// Or for a specific group
scheduler.getListenerManager().addJobListener(
    new CheckendJobListener(),
    GroupMatcher.groupEquals("myGroup")
);
```

The listener captures:
- Job name, group, and class
- Trigger name and group
- Fire time and refire count
- Job data map (sanitized)

## Spring Batch Integration

### Job-Level Monitoring

```java
import com.checkend.integrations.springbatch.CheckendJobExecutionListener;

@Bean
public Job myJob(JobRepository jobRepository) {
    return new JobBuilder("myJob", jobRepository)
        .listener(new CheckendJobExecutionListener())
        .start(myStep())
        .build();
}
```

### Step-Level Monitoring

```java
import com.checkend.integrations.springbatch.CheckendStepExecutionListener;

@Bean
public Step myStep(JobRepository jobRepository, PlatformTransactionManager tm) {
    return new StepBuilder("myStep", jobRepository)
        .<Input, Output>chunk(100, tm)
        .reader(reader())
        .processor(processor())
        .writer(writer())
        .listener(new CheckendStepExecutionListener())
        .build();
}
```

Captured context includes:
- Job/step name and execution IDs
- Status and exit codes
- Read/write/skip counts
- Job parameters

## Custom Logger

Implement the `Logger` interface to customize logging:

```java
import com.checkend.Logger;

Logger myLogger = new Logger() {
    @Override
    public void debug(String message) {
        log.debug("[Checkend] " + message);
    }

    @Override
    public void info(String message) {
        log.info("[Checkend] " + message);
    }

    @Override
    public void warn(String message) {
        log.warn("[Checkend] " + message);
    }

    @Override
    public void error(String message) {
        log.error("[Checkend] " + message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error("[Checkend] " + message, throwable);
    }
};

Checkend.configure(builder -> builder
    .apiKey("your-api-key")
    .logger(myLogger)
);
```

Built-in loggers:
- `Logger.defaultLogger()` - Writes to System.err
- `Logger.nullLogger()` - Discards all messages

## Testing

Use the `Testing` class to capture errors without sending them:

```java
import com.checkend.Checkend;
import com.checkend.Testing;
import org.junit.jupiter.api.*;

class MyTest {

    @BeforeEach
    void setUp() {
        Testing.setup();
        Checkend.configure(builder -> builder
            .apiKey("test-key")
            .enabled(true)
        );
    }

    @AfterEach
    void tearDown() {
        Testing.teardown();
        Checkend.reset();
    }

    @Test
    void testErrorReporting() {
        try {
            throw new RuntimeException("Test error");
        } catch (RuntimeException e) {
            Checkend.notify(e);
        }

        assertTrue(Testing.hasNotices());
        assertEquals(1, Testing.noticeCount());

        Notice notice = Testing.lastNotice();
        assertEquals("java.lang.RuntimeException", notice.getErrorClass());
    }
}
```

## Filtering Sensitive Data

By default, these keys are filtered: `password`, `secret`, `token`, `api_key`, `authorization`, `credit_card`, `cvv`, `ssn`, etc.

Add custom keys:

```java
Checkend.configure(builder -> builder
    .apiKey("your-api-key")
    .addFilterKey("custom_secret")
    .addFilterKey("internal_token")
);
```

Filtered values appear as `[FILTERED]` in the dashboard.

## Ignoring Exceptions

```java
Checkend.configure(builder -> builder
    .apiKey("your-api-key")
    .addIgnoredException(ResourceNotFoundException.class)
    .addIgnoredException("javax.servlet.ServletException")
    .addIgnoredException(Pattern.compile(".*NotFound.*"))
);
```

## Before Notify Callbacks

```java
Checkend.configure(builder -> builder
    .apiKey("your-api-key")
    .addBeforeNotify(notice -> {
        // Add extra context
        notice.getContext().put("server", InetAddress.getLocalHost().getHostName());
        return notice;
    })
    .addBeforeNotify(notice -> {
        // Skip certain errors
        if (notice.getMessage().contains("ignore-me")) {
            return false;
        }
        return true;
    })
);
```

## Graceful Shutdown

The SDK automatically flushes pending notices. For manual control:

```java
// Wait for pending notices to send
Checkend.flush();

// Stop the worker
Checkend.stop();
```

## Development

```bash
# Install git hooks (secrets scanner)
./scripts/install-hooks.sh

# Compile
mvn compile

# Run tests
mvn test

# Run Checkstyle
mvn checkstyle:check

# Package
mvn package
```

## License

MIT License - see [LICENSE](LICENSE) for details.
