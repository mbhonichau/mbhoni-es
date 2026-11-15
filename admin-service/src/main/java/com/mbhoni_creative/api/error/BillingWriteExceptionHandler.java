package com.mbhoni_creative.api.error;

import com.mbhoni_creative.api.controller.BillingWriteController;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Order(-220)
@RestControllerAdvice(assignableTypes=BillingWriteController.class)
public class BillingWriteExceptionHandler {
    @ExceptionHandler(BillingWriteProblem.class)
    public ResponseEntity<ProblemDetail> handle(BillingWriteProblem ex) {
        return ResponseEntity.status(ex.status()).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ApiProblems.detail(ex.status(),ex.code(),ex.code()));
    }
}
