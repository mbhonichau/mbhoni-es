package com.mbhoni_creative.api.controller;

import com.mbhoni_creative.adminentity.InvoiceStatus;
import com.mbhoni_creative.adminentity.QuotationStatus;
import com.mbhoni_creative.api.dto.*;
import com.mbhoni_creative.api.dto.BillingReadDtos.*;
import com.mbhoni_creative.api.error.BillingReadProblem;
import com.mbhoni_creative.api.mapper.ApiTimestamp;
import com.mbhoni_creative.api.repository.BillingReadRepository.Query;
import com.mbhoni_creative.api.service.ApiBillingReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name="ApiKey")
@PreAuthorize("hasAuthority('SCOPE_billing:read')")
public class BillingReadController {
    private final ApiBillingReadService service;
    public BillingReadController(ApiBillingReadService service) { this.service=service; }
    private static Query query(MultiValueMap<String,String> parameters,Class<? extends Enum<?>> statuses) {
        Set<String> allowed=Set.of("page","size","status","dateFrom","dateTo","updatedSince");
        if(parameters.entrySet().stream().anyMatch(e->!allowed.contains(e.getKey())||e.getValue().size()!=1)) throw invalid();
        int page=pageNumber(parameters.getFirst("page"),0,false), size=pageNumber(parameters.getFirst("size"),25,true);
        try {
            String status=parameters.getFirst("status");
            if(status!=null&&(statuses==null||Arrays.stream(statuses.getEnumConstants()).noneMatch(s->s.name().equals(status)))) throw invalid();
            LocalDate from=date(parameters.getFirst("dateFrom")),to=date(parameters.getFirst("dateTo"));
            if(from!=null&&to!=null&&from.isAfter(to)) throw invalid();
            return new Query(page,size,status,from,to,ApiTimestamp.parse(parameters.getFirst("updatedSince")));
        } catch(DateTimeException|IllegalArgumentException ex) { throw invalid(); }
    }
    private static LocalDate date(String s) { return s==null?null:LocalDate.parse(s); }
    private static int pageNumber(String value,int fallback,boolean size) {
        if(value==null) return fallback;
        try { int n=Integer.parseInt(value); if(n<(size?1:0)||size&&n>100) throw new NumberFormatException(); return n; }
        catch(NumberFormatException ex) { throw new BillingReadProblem(400,"INVALID_PAGE_PARAMS"); }
    }
    private static long id(String value) { try { long n=Long.parseLong(value); if(n<1) throw new NumberFormatException(); return n; } catch(NumberFormatException ex) { throw invalid(); } }
    private static BillingReadProblem invalid() { return new BillingReadProblem(400,"INVALID_FILTER"); }
    @GetMapping("/invoices")
    @Operation(summary="List tenant-issued customer invoices",description="TenantInvoice only; excludes Stripe subscription Invoice records and billing accounts. Stored decimal strings; no per-document currency. Filters: status, dateFrom/dateTo (issue date), updatedSince (inclusive), page and size. Includes stored lines; document total is authoritative.")
    public ApiEnvelope<List<Invoice>> invoices(@RequestParam MultiValueMap<String,String> filters) { return service.invoices(query(filters,InvoiceStatus.class)); }
    @GetMapping("/invoices/{id}")
    @Operation(summary="Read a tenant-issued invoice",description="TenantInvoice only, not a Stripe subscription invoice. Includes stored line totals, which may not sum to the stored document total because of existing rounding.")
    public ApiEnvelope<Invoice> invoice(@PathVariable String id) { return ApiEnvelope.single(service.invoice(id(id))); }
    @GetMapping("/quotations")
    @Operation(summary="List tenant-issued quotations",description="Stored decimal strings and line items. Filters: status, dateFrom/dateTo (issue date), updatedSince (inclusive), page and size. No per-document currency is available.")
    public ApiEnvelope<List<Quotation>> quotations(@RequestParam MultiValueMap<String,String> filters) { return service.quotations(query(filters,QuotationStatus.class)); }
    @GetMapping("/quotations/{id}")
    @Operation(summary="Read a tenant-issued quotation",description="Stored document total is authoritative; no conversion or writes are available in Phase 3a.")
    public ApiEnvelope<Quotation> quotation(@PathVariable String id) { return ApiEnvelope.single(service.quotation(id(id))); }
    @GetMapping("/expenses")
    @Operation(summary="List tenant expenses",description="Expenses have no status or line items. Filters: dateFrom/dateTo (expense date), updatedSince (inclusive), page and size. Notes and payment method are excluded. No currency is inferred.")
    public ApiEnvelope<List<Expense>> expenses(@RequestParam MultiValueMap<String,String> filters) { return service.expenses(query(filters,null)); }
    @GetMapping("/expenses/{id}")
    @Operation(summary="Read a tenant expense",description="Read-only; no expense creation in Phase 3a. Expenses are normal report entries, not drafts.")
    public ApiEnvelope<Expense> expense(@PathVariable String id) { return ApiEnvelope.single(service.expense(id(id))); }
}
