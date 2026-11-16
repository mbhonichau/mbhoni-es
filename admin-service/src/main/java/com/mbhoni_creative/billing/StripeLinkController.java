package com.mbhoni_creative.billing;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
@RestController
public class StripeLinkController {
    public record LinkRequest(String subscriptionId, String customerId, String reason) {}
    private final StripeLinkService service;
    public StripeLinkController(StripeLinkService service) { this.service = service; }
    @PostMapping("/api/platform/tenants/{tenantId}/stripe-link")
    public ResponseEntity<Void> link(@PathVariable Long tenantId, @RequestBody LinkRequest request) {
        service.link(tenantId, request.subscriptionId(), request.customerId(), request.reason());
        return ResponseEntity.noContent().build();
    }
}
