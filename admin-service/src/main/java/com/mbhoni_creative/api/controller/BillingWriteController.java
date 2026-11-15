package com.mbhoni_creative.api.controller;

import com.mbhoni_creative.api.service.ApiBillingWriteService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAuthority('SCOPE_billing:write')")
public class BillingWriteController {
    private final ApiBillingWriteService service;
    public BillingWriteController(ApiBillingWriteService service) { this.service=service; }
    @PostMapping(value="/invoices",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> invoice(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody String body) { return response(service.create(true,key,body)); }
    @PostMapping(value="/quotations",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> quotation(@RequestHeader(value="Idempotency-Key",required=false) String key,@RequestBody String body) { return response(service.create(false,key,body)); }
    private ResponseEntity<String> response(ApiBillingWriteService.Result r) {
        return ResponseEntity.status(r.status()).contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.LOCATION,r.location()).body(r.body());
    }
}
