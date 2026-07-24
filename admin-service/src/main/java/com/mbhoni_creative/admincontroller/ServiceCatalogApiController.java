package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.ServiceCatalog;
import com.mbhoni_creative.adminentity.ServiceSla;
import com.mbhoni_creative.adminservice.ServiceCatalogService;

@RestController
@RequestMapping("/api/services")
public class ServiceCatalogApiController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogApiController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SERVICE_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<ServiceCatalog>> getServices() {
        return ResponseEntity.ok(serviceCatalogService.getAllServices());
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAuthority('SERVICE_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<ServiceCatalog> getServiceByCode(@PathVariable String code) {
        return ResponseEntity.ok(serviceCatalogService.getServiceByCode(code));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SERVICE_EDIT')")
    public ResponseEntity<ServiceCatalog> createService(@RequestBody ServiceCatalog serviceCatalog) {
        return ResponseEntity.ok(serviceCatalogService.saveService(serviceCatalog));
    }

    @PostMapping("/{id}/slas")
    @PreAuthorize("hasAuthority('SERVICE_EDIT')")
    public ResponseEntity<ServiceSla> addSla(@PathVariable Long id, @RequestBody ServiceSla sla) {
        return ResponseEntity.ok(serviceCatalogService.addSla(id, sla));
    }

    @GetMapping("/{id}/slas")
    @PreAuthorize("hasAuthority('SERVICE_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<ServiceSla>> getSlas(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.getSlasByServiceId(id));
    }
}
