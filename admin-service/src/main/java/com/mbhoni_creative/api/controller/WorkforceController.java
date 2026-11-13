package com.mbhoni_creative.api.controller;

import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.api.dto.*;
import com.mbhoni_creative.api.dto.WorkforceDtos.*;
import com.mbhoni_creative.api.error.WorkforceProblem;
import com.mbhoni_creative.api.mapper.ApiTimestamp;
import com.mbhoni_creative.api.service.WorkforceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name="ApiKey")
public class WorkforceController {
    private final WorkforceService service;
    public WorkforceController(WorkforceService service) { this.service=service; }
    private static int number(String value,boolean size) {
        try { int n=Integer.parseInt(value); if(n<(size?1:0) || size && n>100) throw new NumberFormatException(); return n; }
        catch(NumberFormatException ex) { throw new WorkforceProblem(400,"INVALID_PAGE_PARAMS"); }
    }
    private static Long id(String value) {
        if(value==null) return null;
        try { long n=Long.parseLong(value); if(n<1) throw new NumberFormatException(); return n; }
        catch(NumberFormatException ex) { throw new WorkforceProblem(400,"INVALID_FILTER"); }
    }
    private static LocalDateTime since(String value) {
        try { return ApiTimestamp.parse(value); }
        catch(java.time.DateTimeException | IllegalArgumentException ex) { throw new WorkforceProblem(400,"INVALID_FILTER"); }
    }
    @GetMapping("/employees") @PreAuthorize("hasAuthority('SCOPE_employees:read')")
    @Operation(summary="List visible tenant employees",description="Excludes isolated organization subtrees and sensitive fields. updatedSince is inclusive and includes custom-value recordedAt.")
    public ApiEnvelope<List<EmployeeSummary>> employees(@RequestParam(defaultValue="0") String page,@RequestParam(defaultValue="25") String size,
            @RequestParam(required=false) String orgUnitId,@RequestParam(required=false) String status,@RequestParam(required=false) String updatedSince) {
        if(status!=null) try { EmployeeStatus.valueOf(status); } catch(IllegalArgumentException ex) { throw new WorkforceProblem(400,"INVALID_FILTER"); }
        return service.employees(number(page,false),number(size,true),id(orgUnitId),status,since(updatedSince));
    }
    @GetMapping("/employees/{id}") @PreAuthorize("hasAuthority('SCOPE_employees:read')")
    @Operation(summary="Read a visible tenant employee",description="Missing, foreign-tenant and isolated employees all return EMPLOYEE_NOT_FOUND.")
    public ApiEnvelope<EmployeeDetail> employee(@PathVariable String id) { return ApiEnvelope.single(service.employee(id(id))); }
    @GetMapping("/employee-fields") @PreAuthorize("hasAuthority('SCOPE_employees:read')")
    @Operation(summary="List safe effective employee field definitions")
    public ApiEnvelope<List<Field>> fields(@RequestParam(defaultValue="0") String page,@RequestParam(defaultValue="25") String size) {
        return service.fields(number(page,false),number(size,true));
    }
    @GetMapping("/org-units") @PreAuthorize("hasAuthority('SCOPE_orgunits:read')")
    @Operation(summary="List visible organization units",description="Flat hierarchy; excludes isolated subtrees, cycles and invalid parent references. No status field exists in the underlying model.")
    public ApiEnvelope<List<OrgUnit>> units(@RequestParam(defaultValue="0") String page,@RequestParam(defaultValue="25") String size,
            @RequestParam(required=false) String updatedSince,@RequestParam(required=false) String tree) {
        if(tree!=null) throw new WorkforceProblem(400,"INVALID_FILTER");
        return service.units(number(page,false),number(size,true),since(updatedSince));
    }
    @GetMapping("/reference/{type}") @PreAuthorize("hasAuthority('SCOPE_reference:read')")
    @Operation(summary="List allowlisted reference values",description="Types: countries, currencies, employment-types, employee-statuses. Tenant overrides global defaults. Metadata is excluded.")
    public ApiEnvelope<List<Reference>> references(@PathVariable String type,@RequestParam(defaultValue="0") String page,@RequestParam(defaultValue="25") String size) {
        return service.references(type,number(page,false),number(size,true));
    }
}
