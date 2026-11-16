package com.mbhoni_creative.api.error;

import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@Order(-200)
@RestControllerAdvice(basePackages = "com.mbhoni_creative.api.controller")
public class ApiExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> forbidden() { return response(403, "FORBIDDEN", "Access denied"); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class,
            java.time.format.DateTimeParseException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ProblemDetail> badRequest() { return response(400, "INVALID_REQUEST", "Request parameters or JSON are invalid"); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> invalid() { return response(422, "VALIDATION_FAILED", "Request fields failed validation"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> conflict() { return response(409, "CONFLICT", "The request conflicts with existing data"); }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> status(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        String code = switch (status) { case 400 -> "INVALID_REQUEST"; case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND"; case 409 -> "CONFLICT"; case 422 -> "VALIDATION_FAILED"; default -> "INTERNAL_ERROR"; };
        return response(status, code, status == 404 ? "Resource not found" : "Request could not be completed");
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> unexpected() { return response(500, "INTERNAL_ERROR", "Request could not be completed"); }
    private ResponseEntity<ProblemDetail> response(int status, String code, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(ApiProblems.detail(status, code, detail));
    }
}
