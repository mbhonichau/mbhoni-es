package com.mbhoni_creative.billing;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Map;
@Order(-100)
@RestControllerAdvice(assignableTypes = {StripeWebhookController.class, StripeLinkController.class})
public class BillingApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException ex) { return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getReason() == null ? "Request failed" : ex.getReason())); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied() { return ResponseEntity.status(403).body(Map.of("error", "Access denied")); }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> retry(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(getClass()).error("Billing request failed; transaction must be retried", ex);
        return ResponseEntity.status(503).body(Map.of("error", "Billing temporarily unavailable; retry request"));
    }
}
