package com.checkend.integrations.servlet;

import com.checkend.Checkend;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet filter for automatic error reporting.
 * Add to web.xml or register programmatically.
 */
public class CheckendFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Configuration should be done before this filter is used
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest httpRequest) {
                setRequestContext(httpRequest);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            Checkend.notify(e);
            throw e;
        } finally {
            Checkend.clear();
        }
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }

    private void setRequestContext(HttpServletRequest request) {
        Map<String, Object> requestInfo = new HashMap<>();
        requestInfo.put("url", getFullUrl(request));
        requestInfo.put("method", request.getMethod());
        requestInfo.put("remote_addr", request.getRemoteAddr());
        requestInfo.put("user_agent", request.getHeader("User-Agent"));

        // Collect headers (excluding sensitive ones)
        Map<String, String> headers = new HashMap<>();
        var headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String lowerName = name.toLowerCase();
                if (!lowerName.contains("authorization") &&
                    !lowerName.contains("cookie") &&
                    !lowerName.contains("token")) {
                    headers.put(name, request.getHeader(name));
                }
            }
        }
        requestInfo.put("headers", headers);

        // Collect parameters (will be filtered by SDK)
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            Map<String, Object> paramMap = new HashMap<>();
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String[] values = entry.getValue();
                if (values.length == 1) {
                    paramMap.put(entry.getKey(), values[0]);
                } else {
                    paramMap.put(entry.getKey(), values);
                }
            }
            requestInfo.put("params", paramMap);
        }

        Checkend.setRequest(requestInfo);
    }

    private String getFullUrl(HttpServletRequest request) {
        StringBuilder url = new StringBuilder();
        url.append(request.getScheme()).append("://");
        url.append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            url.append(":").append(port);
        }
        url.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null) {
            url.append("?").append(query);
        }
        return url.toString();
    }
}
