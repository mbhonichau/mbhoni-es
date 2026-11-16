package com.mbhoni_creative.onboarding;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/organization/{unitId}")
public class OrganizationBoundaryController {
    private final OrganizationBoundaryService service;
    public OrganizationBoundaryController(OrganizationBoundaryService service) { this.service = service; }
    public record Boundary(boolean isolated, boolean restrictParentVisibility, String reason) {}
    public record Membership(Long userId, boolean administrator, boolean remove, String reason) {}
    @PutMapping("/boundary") public void boundary(@PathVariable Long unitId, @RequestBody Boundary b) {
        service.boundary(unitId, b.isolated(), b.restrictParentVisibility(), b.reason());
    }
    @PutMapping("/membership") public void membership(@PathVariable Long unitId, @RequestBody Membership m) {
        service.membership(unitId, m.userId(), m.administrator(), m.remove(), m.reason());
    }
}
