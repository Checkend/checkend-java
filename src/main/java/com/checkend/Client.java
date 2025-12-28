package com.checkend;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * HTTP client for sending notices to Checkend.
 */
public final class Client {
    private static final String SDK_VERSION = "0.1.0";

    private final Configuration config;
    private final Logger logger;

    public Client(Configuration config) {
        this.config = config;
        this.logger = config.getLogger();
    }

    /**
     * Send a notice to Checkend.
     * @return Response containing status and body
     */
    public Response send(Notice notice) {
        HttpURLConnection connection = null;
        try {
            String url = config.getEndpoint() + "/ingest/v1/errors";
            connection = openConnection(url);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(config.getConnectTimeout());
            connection.setReadTimeout(config.getReadTimeout());
            connection.setDoOutput(true);

            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Checkend-Ingestion-Key", config.getApiKey());
            connection.setRequestProperty("User-Agent", "checkend-java/" + SDK_VERSION);

            // Add proxy authentication header if needed
            addProxyAuthHeader(connection);

            // Write body
            String body = toJson(notice.toMap());
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            // Read response
            int statusCode = connection.getResponseCode();
            String responseBody = readResponse(connection, statusCode);
            String retryAfter = connection.getHeaderField("Retry-After");

            logger.debug("Response: " + statusCode + " - " + responseBody);

            return new Response(statusCode, responseBody, retryAfter);

        } catch (IOException e) {
            logger.error("Error sending notice: " + e.getMessage());
            return new Response(0, e.getMessage(), null);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Open a connection, optionally through a proxy.
     */
    private HttpURLConnection openConnection(String url) throws IOException {
        URI uri = URI.create(url);
        URL urlObj = uri.toURL();

        if (config.hasProxy()) {
            Proxy proxy = new Proxy(
                Proxy.Type.HTTP,
                new InetSocketAddress(config.getProxyHost(), config.getProxyPort())
            );
            logger.debug("Using proxy: " + config.getProxyHost() + ":" + config.getProxyPort());
            return (HttpURLConnection) urlObj.openConnection(proxy);
        }

        return (HttpURLConnection) urlObj.openConnection();
    }

    /**
     * Add proxy authentication header if credentials are configured.
     */
    private void addProxyAuthHeader(HttpURLConnection connection) {
        if (config.hasProxy() && config.getProxyUsername() != null && !config.getProxyUsername().isEmpty()) {
            String auth = config.getProxyUsername() + ":" +
                (config.getProxyPassword() != null ? config.getProxyPassword() : "");
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
        }
    }

    private String readResponse(HttpURLConnection connection, int statusCode) throws IOException {
        InputStream stream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    /**
     * Simple JSON serialization without external dependencies.
     */
    @SuppressWarnings("unchecked")
    private String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                sb.append(toJson(entry.getValue()));
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Iterable) {
            Iterable<?> list = (Iterable<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(",");
                first = false;
                sb.append(toJson(item));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(obj.toString()) + "\"";
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Response from the Checkend API.
     */
    public record Response(int statusCode, String body, String retryAfter) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        public boolean isRateLimited() {
            return statusCode == 429;
        }

        /**
         * Get retry-after delay in milliseconds, or default if not specified.
         */
        public long getRetryAfterMs(long defaultMs) {
            if (retryAfter == null || retryAfter.isEmpty()) {
                return defaultMs;
            }
            try {
                // Retry-After can be seconds or HTTP-date; we only support seconds
                return Long.parseLong(retryAfter) * 1000;
            } catch (NumberFormatException e) {
                return defaultMs;
            }
        }
    }
}
