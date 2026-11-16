package com.mbhoni_creative.onboarding;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/{tenantId}/ownership")
public class OwnershipController {
    private final OwnershipService service;
    public OwnershipController(OwnershipService service) { this.service = service; }
    public record Transfer(Long userId, String reason) {}
    @PostMapping public void transfer(@PathVariable Long tenantId, @RequestBody Transfer transfer) {
        service.transfer(tenantId, transfer.userId(), transfer.reason());
    }
}
