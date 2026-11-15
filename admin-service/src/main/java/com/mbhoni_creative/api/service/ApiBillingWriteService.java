package com.mbhoni_creative.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.api.config.BillingWriteSettings;
import com.mbhoni_creative.api.dto.*;
import com.mbhoni_creative.api.error.BillingWriteProblem;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiBillingWriteService {
    private final ApiReadService identity;
    private final BillingWriteSettings config;
    private final BillingDraftParser parser;
    private final JdbcTemplate jdbc;
    private final TenantRepository tenants;
    private final TenantSettingsRepository settings;
    private final TenantInvoiceRepository invoices;
    private final TenantQuotationRepository quotations;
    private final AuditLogService audit;
    private final ObjectMapper mapper;
    private final Clock clock;
    public ApiBillingWriteService(ApiReadService identity,BillingWriteSettings config,BillingDraftParser parser,JdbcTemplate jdbc,
            TenantRepository tenants,TenantSettingsRepository settings,TenantInvoiceRepository invoices,
            TenantQuotationRepository quotations,AuditLogService audit,ObjectMapper mapper,Clock clock) {
        this.identity=identity;this.config=config;this.parser=parser;this.jdbc=jdbc;this.tenants=tenants;this.settings=settings;
        this.invoices=invoices;this.quotations=quotations;this.audit=audit;this.mapper=mapper;this.clock=clock;
    }
    public record Result(int status,String body,String location) {}
    @Transactional
    public Result create(boolean invoice,String key,String body) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null||!auth.isAuthenticated()||!(auth.getPrincipal() instanceof ApiKeyPrincipal principal)
                ||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("SCOPE_billing:write")))
            throw new AccessDeniedException("Billing write scope required");
        long tenant=identity.me().tenantId();
        if(!config.enabled()) throw new BillingWriteProblem(403,"API_WRITES_DISABLED");
        if(key==null||key.isBlank()) throw new BillingWriteProblem(400,"IDEMPOTENCY_KEY_REQUIRED");
        if(!key.matches("[!-~]{1,128}")) throw new BillingWriteProblem(400,"INVALID_IDEMPOTENCY_KEY");
        var parsed=parser.parse(body,invoice);
        String endpoint=invoice?"/api/v1/invoices":"/api/v1/quotations";
        String hash=hash("POST\n"+endpoint+"\n"+parsed.canonical());
        byte[] binary=key.getBytes(StandardCharsets.US_ASCII);
        var now=LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);
        // A tenant lock also makes lazy sequence-row creation safe across application instances.
        // NOWAIT returns a retryable conflict instead of waiting behind an in-flight duplicate.
        try { jdbc.queryForObject("SELECT id FROM tenants WHERE id=? FOR UPDATE NOWAIT",Long.class,tenant); }
        catch(org.springframework.dao.DataAccessException ex) {
            if(ex instanceof org.springframework.dao.PessimisticLockingFailureException
                    ||ex instanceof org.springframework.dao.QueryTimeoutException
                    ||ex.getMostSpecificCause() instanceof java.sql.SQLException sql && sql.getErrorCode()==3572)
                throw new BillingWriteProblem(409,"IDEMPOTENCY_IN_PROGRESS");
            throw ex;
        }
        var previous=jdbc.queryForList("SELECT request_hash,response_status,response_body,response_location,expires_at FROM api_idempotency WHERE tenant_id=? AND idempotency_key=? FOR UPDATE",tenant,binary);
        if(!previous.isEmpty()) {
            var p=previous.getFirst();
            if(((Timestamp)p.get("expires_at")).toLocalDateTime().isAfter(now)) {
                if(!hash.equals(p.get("request_hash"))) throw new BillingWriteProblem(422,"IDEMPOTENCY_KEY_REUSED");
                if(p.get("response_status")==null) throw new BillingWriteProblem(409,"IDEMPOTENCY_IN_PROGRESS");
                return new Result(((Number)p.get("response_status")).intValue(),(String)p.get("response_body"),(String)p.get("response_location"));
            }
            jdbc.update("DELETE FROM api_idempotency WHERE tenant_id=? AND idempotency_key=?",tenant,binary);
        }
        if(settings.findByTenantId(tenant).map(TenantSettings::isMultiCurrencyEnabled).orElse(false))
            throw new BillingWriteProblem(422,"MULTI_CURRENCY_TENANT_UNSUPPORTED");
        jdbc.update("INSERT INTO api_idempotency(tenant_id,api_key_id,idempotency_key,request_hash,created_at,expires_at) VALUES(?,?,?,?,?,?)",
                tenant,principal.getApiKey().getId(),binary,hash,now,now.plusHours(24));
        String type=invoice?"INVOICE":"QUOTATION";
        var values=jdbc.queryForList("SELECT next_value FROM api_document_sequence WHERE tenant_id=? AND document_type=? FOR UPDATE",Long.class,tenant,type);
        long number;
        if(values.isEmpty()) { number=1; jdbc.update("INSERT INTO api_document_sequence(tenant_id,document_type,next_value) VALUES(?,?,?)",tenant,type,2L); }
        else { number=values.getFirst(); jdbc.update("UPDATE api_document_sequence SET next_value=? WHERE tenant_id=? AND document_type=?",Math.addExact(number,1),tenant,type); }
        String label=(invoice?"API-INV-":"API-QUO-")+String.format(Locale.ROOT,"%06d",number);
        var owner=tenants.getReferenceById(tenant); var r=parsed.request();
        long id; Object dto;
        if(invoice) {
            var d=new TenantInvoice();d.setTenant(owner);d.setInvoiceNumber(label);d.setStatus(InvoiceStatus.DRAFT);
            d.setCustomerName(r.customerName());d.setCustomerEmail(r.customerEmail());d.setCustomerPhone(r.customerPhone());
            d.setCustomerAddress(r.customerAddress());d.setNotes(r.notes());d.setIssueDate(r.issueDate());d.setDueDate(r.dueDate());
            for(var line:r.items()) { var item=new TenantInvoiceItem();item.setDescription(line.description());
                item.setQuantity(BillingDraftParser.money(line.quantity()));item.setUnitPrice(BillingDraftParser.money(line.unitPrice()));d.addItem(item);check(item.getTotalAmount()); }
            check(d.getTotalAmount());invoices.saveAndFlush(d);id=d.getId();
            dto=new BillingReadDtos.Invoice(id,label,d.getCustomerName(),d.getStatus().name(),d.getIssueDate(),d.getDueDate(),
                    money(d.getSubtotal()),money(d.getTaxAmount()),money(d.getTotalAmount()),money(d.getAmountPaid()),d.getUpdatedAt(),
                    d.getItems().stream().map(i->new BillingReadDtos.Line(i.getId(),i.getDescription(),money(i.getQuantity()),money(i.getUnitPrice()),money(i.getTaxRate()),money(i.getTotalAmount()))).toList());
        } else {
            var d=new TenantQuotation();d.setTenant(owner);d.setQuotationNumber(label);d.setStatus(QuotationStatus.DRAFT);
            d.setCustomerName(r.customerName());d.setCustomerEmail(r.customerEmail());d.setCustomerPhone(r.customerPhone());
            d.setCustomerAddress(r.customerAddress());d.setNotes(r.notes());d.setIssueDate(r.issueDate());d.setValidUntilDate(r.validUntilDate());
            for(var line:r.items()) { var item=new TenantQuotationItem();item.setDescription(line.description());
                item.setQuantity(BillingDraftParser.money(line.quantity()));item.setUnitPrice(BillingDraftParser.money(line.unitPrice()));d.addItem(item);check(item.getTotalAmount()); }
            check(d.getTotalAmount());quotations.saveAndFlush(d);id=d.getId();
            dto=new BillingReadDtos.Quotation(id,label,d.getCustomerName(),d.getStatus().name(),d.getIssueDate(),d.getValidUntilDate(),
                    money(d.getSubtotal()),money(d.getTaxAmount()),money(d.getTotalAmount()),d.getUpdatedAt(),
                    d.getItems().stream().map(i->new BillingReadDtos.Line(i.getId(),i.getDescription(),money(i.getQuantity()),money(i.getUnitPrice()),money(i.getTaxRate()),money(i.getTotalAmount()))).toList());
        }
        String location=endpoint+"/"+id;
        String response;
        try { response=mapper.writeValueAsString(Map.of("data",dto)); }
        catch(com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException("Unable to serialize draft",ex); }
        audit.logForTenant(tenant,"API_DRAFT_CREATED",type,String.valueOf(id),"apiKeyId="+principal.getApiKey().getId()+" endpoint="+endpoint+" idempotencyKey="+key+" resourceId="+id);
        jdbc.update("UPDATE api_idempotency SET response_status=201,response_body=?,response_location=?,resource_id=? WHERE tenant_id=? AND idempotency_key=?",response,location,id,tenant,binary);
        return new Result(201,response,location);
    }
    private static String money(BigDecimal v) { return v.setScale(2).toPlainString(); }
    private static void check(BigDecimal value) { if(value.compareTo(new BigDecimal("9999999999.99"))>0) throw new BillingWriteProblem(422,"INVALID_MONEY"); }
    private static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
