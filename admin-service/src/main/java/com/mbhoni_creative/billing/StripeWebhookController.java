package com.mbhoni_creative.billing;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController
public class StripeWebhookController {
    private final StripeWebhookService service;
    public StripeWebhookController(StripeWebhookService service) { this.service = service; }
    @PostMapping("/api/webhooks/stripe")
    public ResponseEntity<Void> receive(@RequestBody byte[] body,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {
        service.receive(body, signature); return ResponseEntity.ok().build();
    }
}
