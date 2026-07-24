package com.mbhoni_creative.adminexception;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.mbhoni_creative.admindto.ApiErrorResponse;
import com.mbhoni_creative.admincontroller.IntegrationApiController;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(assignableTypes = IntegrationApiController.class)
public class IntegrationApiExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiErrorResponse accessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return new ApiErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "The API key does not have permission to access this resource",
                request.getRequestURI()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse runtime(RuntimeException ex, HttpServletRequest request) {
        return new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
    }
}
