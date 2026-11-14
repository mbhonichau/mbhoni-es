package com.mbhoni_creative.api.error;
import com.mbhoni_creative.api.controller.BillingReadController;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@Order(-210)
@RestControllerAdvice(assignableTypes=BillingReadController.class)
public class BillingReadExceptionHandler {
    @ExceptionHandler(BillingReadProblem.class)
    public ResponseEntity<ProblemDetail> handle(BillingReadProblem ex) {
        return ResponseEntity.status(ex.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiProblems.detail(ex.status(),ex.code(),ex.status()==404?"Resource not found":"Invalid filter or page parameters"));
    }
}
