package com.mbhoni_creative.api.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiPaths {
    private ApiPaths() {}
    /** One rule for chain selection and component-filter exclusions. */
    public static final org.springframework.security.web.util.matcher.RequestMatcher MATCHER = ApiPaths::matchesPath;
    public static boolean matches(HttpServletRequest request) {
        return MATCHER.matches(request);
    }
    private static boolean matchesPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/v1") || path.startsWith("/api/v1/")
                || path.equals("/api/docs") || path.startsWith("/api/docs/") || path.equals("/api/openapi.json");
    }
}
