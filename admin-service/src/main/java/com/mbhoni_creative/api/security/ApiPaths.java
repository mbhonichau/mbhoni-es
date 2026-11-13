package com.mbhoni_creative.api.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ApiPaths {
    private ApiPaths() {}
    public static boolean matches(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/v1") || path.startsWith("/api/v1/")
                || path.equals("/api/docs") || path.startsWith("/api/docs/") || path.equals("/api/openapi.json");
    }
}
