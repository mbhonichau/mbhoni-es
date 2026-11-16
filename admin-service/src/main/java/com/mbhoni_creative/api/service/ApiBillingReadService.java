package com.mbhoni_creative.api.service;

import com.mbhoni_creative.api.dto.ApiEnvelope;
import com.mbhoni_creative.api.dto.BillingReadDtos.*;
import com.mbhoni_creative.api.repository.BillingReadRepository;
import com.mbhoni_creative.api.repository.BillingReadRepository.*;
import com.mbhoni_creative.api.error.BillingReadProblem;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class ApiBillingReadService {
    private final BillingReadRepository repository;
    private final ApiReadService identity;
    public ApiBillingReadService(BillingReadRepository repository,ApiReadService identity) { this.repository=repository; this.identity=identity; }
    private long tenant() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null||!auth.isAuthenticated()||auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("SCOPE_billing:read")))
            throw new AccessDeniedException("Billing read scope required");
        return identity.me().tenantId();
    }
    public ApiEnvelope<List<Invoice>> invoices(Query q) {
        long t=tenant(); long count=repository.count(Kind.INVOICE,t,q); return page(invoiceRows(t,q,null),q,count);
    }
    public Invoice invoice(long id) { return one(invoiceRows(tenant(),single(),id),"INVOICE_NOT_FOUND"); }
    private List<Invoice> invoiceRows(long t,Query q,Long id) {
        var rows=repository.documents(Kind.INVOICE,t,q,id); var lines=repository.lines(Kind.INVOICE,t,rows.stream().map(Document::id).toList());
        return rows.stream().map(d->new Invoice(d.id(),d.number(),d.customer(),d.status(),d.issued(),d.end(),d.subtotal(),d.tax(),d.total(),d.paid(),
                d.updated(),lines.getOrDefault(d.id(),List.of()))).toList();
    }
    public ApiEnvelope<List<Quotation>> quotations(Query q) {
        long t=tenant(); long count=repository.count(Kind.QUOTATION,t,q); return page(quotationRows(t,q,null),q,count);
    }
    public Quotation quotation(long id) { return one(quotationRows(tenant(),single(),id),"QUOTATION_NOT_FOUND"); }
    private List<Quotation> quotationRows(long t,Query q,Long id) {
        var rows=repository.documents(Kind.QUOTATION,t,q,id); var lines=repository.lines(Kind.QUOTATION,t,rows.stream().map(Document::id).toList());
        return rows.stream().map(d->new Quotation(d.id(),d.number(),d.customer(),d.status(),d.issued(),d.end(),d.subtotal(),d.tax(),d.total(),
                d.updated(),lines.getOrDefault(d.id(),List.of()))).toList();
    }
    public ApiEnvelope<List<Expense>> expenses(Query q) {
        long t=tenant(); long count=repository.count(Kind.EXPENSE,t,q); return page(repository.expenses(t,q,null),q,count);
    }
    public Expense expense(long id) { return one(repository.expenses(tenant(),single(),id),"EXPENSE_NOT_FOUND"); }
    private static Query single() { return new Query(0,1,null,null,null,null); }
    private static <T> T one(List<T> rows,String code) { if(rows.isEmpty()) throw new BillingReadProblem(404,code); return rows.getFirst(); }
    private static <T> ApiEnvelope<List<T>> page(List<T> rows,Query q,long count) { return new ApiEnvelope<>(rows,new ApiEnvelope.Page(q.page(),q.size(),count)); }
}
