package com.checkend.integrations.spring;

import com.checkend.Checkend;
import com.checkend.Configuration;

/**
 * Spring Boot auto-configuration for Checkend.
 *
 * To use with Spring Boot, create a configuration class:
 *
 * <pre>
 * {@code
 * @Configuration
 * public class CheckendConfig {
 *
 *     @Bean
 *     public CheckendAutoConfiguration checkendAutoConfiguration(
 *             @Value("${checkend.api-key}") String apiKey,
 *             @Value("${checkend.endpoint:https://app.checkend.com}") String endpoint,
 *             @Value("${checkend.environment:#{null}}") String environment,
 *             @Value("${checkend.enabled:true}") boolean enabled,
 *             @Value("${checkend.debug:false}") boolean debug) {
 *         return new CheckendAutoConfiguration(apiKey, endpoint, environment, enabled, debug);
 *     }
 *
 *     @Bean
 *     public CheckendExceptionHandler checkendExceptionHandler() {
 *         return new CheckendExceptionHandler();
 *     }
 * }
 * }
 * </pre>
 *
 * And add properties to application.properties:
 * <pre>
 * checkend.api-key=your-api-key
 * checkend.endpoint=https://app.checkend.com
 * checkend.environment=production
 * checkend.enabled=true
 * checkend.debug=false
 * </pre>
 */
public class CheckendAutoConfiguration {

    public CheckendAutoConfiguration(String apiKey, String endpoint, String environment,
                                      boolean enabled, boolean debug) {
        Configuration.Builder builder = new Configuration.Builder()
                .apiKey(apiKey)
                .endpoint(endpoint)
                .enabled(enabled)
                .debug(debug);

        if (environment != null && !environment.isEmpty()) {
            builder.environment(environment);
        }

        Checkend.configure(builder.build());
    }
}
