package com.mbhoni_creative.contentmanagementservice;

import feign.FeignException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for Content Management Service
 * Handles tenant content (about, media, products) and Feign-based metrics.
 */
@RestController
@RequestMapping("/api")
public class ContentController {

    @Value("${welcome.message:Welcome to mbhoni-ES Content Management}")
    private String welcomeMessage;

    private final TenantContentRepository repository;
    private final AdminServiceClient adminServiceClient;

    public ContentController(TenantContentRepository repository, AdminServiceClient adminServiceClient) {
        this.repository = repository;
        this.adminServiceClient = adminServiceClient;
    }

    // ========================================================================
    // PUBLIC STATUS ENDPOINT
    // ========================================================================
    @GetMapping("/content/status")
    public String getContentStatus() {
        System.out.println("[CONTENT] Status endpoint called");
        return welcomeMessage;
    }

    // ========================================================================
    // TENANT CONTENT MANAGEMENT
    // ========================================================================
    @PostMapping("/content/about/{tenantId}")
    public ResponseEntity<String> updateAboutInfo(
            @PathVariable String tenantId,
            @RequestBody String aboutInfo) {

        validateTenantId(tenantId);
        TenantContent content = repository.findById(tenantId).orElse(new TenantContent());
        content.setTenantId(tenantId);
        content.setAbout(aboutInfo);
        repository.save(content);

        System.out.println("[CONTENT] About updated for tenant: " + tenantId);
        return ResponseEntity.ok("About info updated for Tenant ID: " + tenantId);
    }

    @PostMapping("/content/media/{tenantId}")
    public ResponseEntity<String> uploadMedia(
            @PathVariable String tenantId,
            @RequestBody String mediaUrl) {

        validateTenantId(tenantId);
        TenantContent content = repository.findById(tenantId).orElse(new TenantContent());
        content.setTenantId(tenantId);
        content.setMedia(mediaUrl);
        repository.save(content);

        System.out.println("[CONTENT] Media uploaded for tenant: " + tenantId);
        return ResponseEntity.ok("Media uploaded for Tenant ID: " + tenantId);
    }

    @PostMapping("/content/products/{tenantId}")
    public ResponseEntity<String> updateProducts(
            @PathVariable String tenantId,
            @RequestBody String productsInfo) {

        validateTenantId(tenantId);
        TenantContent content = repository.findById(tenantId).orElse(new TenantContent());
        content.setTenantId(tenantId);
        content.setProducts(productsInfo);
        repository.save(content);

        System.out.println("[CONTENT] Products updated for tenant: " + tenantId);
        return ResponseEntity.ok("Products updated for Tenant ID: " + tenantId);
    }

    @GetMapping("/content/{tenantId}")
    public ResponseEntity<Map<String, String>> getContent(@PathVariable String tenantId) {
        validateTenantId(tenantId);

        TenantContent content = repository.findById(tenantId).orElse(new TenantContent());
        Map<String, String> result = new HashMap<>();
        result.put("about", nullToEmpty(content.getAbout()));
        result.put("media", nullToEmpty(content.getMedia()));
        result.put("products", nullToEmpty(content.getProducts()));

        System.out.println("[CONTENT] Retrieved content for tenant: " + tenantId);
        return ResponseEntity.ok(result);
    }

    // ========================================================================
    // FEIGN CALL TO ADMIN-SERVICE (LOAD-BALANCED METRICS)
    // ========================================================================
    @GetMapping("/loadbalanced-metrics/{tenantId}")
    public ResponseEntity<String> getLoadBalancedMetrics(@PathVariable String tenantId) {
        System.out.println("[CONTENT] Feign call initiated for tenant: " + tenantId);

        try {
            String metrics = adminServiceClient.getTenantMetrics(tenantId);
            System.out.println("[CONTENT] Feign success: " + metrics);
            return ResponseEntity.ok(metrics);

        } catch (FeignException.Unauthorized e) {
            System.err.println("[CONTENT] FEIGN 401: Invalid credentials");
            return ResponseEntity.status(401).body("Unauthorized: Invalid API key or credentials");

        } catch (FeignException.NotFound e) {
            System.err.println("[CONTENT] FEIGN 404: Metrics not found for tenant: " + tenantId);
            return ResponseEntity.status(404).body("No metrics found for tenant: " + tenantId);

        } catch (FeignException.FeignClientException e) {
            System.err.println("[CONTENT] FEIGN CLIENT ERROR: " + e.getMessage());
            return ResponseEntity.status(502).body("Service unavailable: Cannot reach admin-service");

        } catch (Exception e) {
            System.err.println("[CONTENT] UNEXPECTED FEIGN ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body("Internal error: " + e.getMessage());
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================
    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || !tenantId.matches("tenant\\d+")) {
            throw new IllegalArgumentException("Invalid tenant ID: '" + tenantId + "'. Must match 'tenant1', 'tenant2', etc.");
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}