package com.mbhoni_creative.api.repository;

import com.mbhoni_creative.api.dto.BillingReadDtos.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

@Repository
public class BillingReadRepository {
    public enum Kind {
        INVOICE("tenant_invoices","tenant_invoice_items","invoice_id","invoice_number","due_date"),
        QUOTATION("tenant_quotations","tenant_quotation_items","quotation_id","quotation_number","valid_until_date"),
        EXPENSE("business_expenses",null,null,null,null);
        final String table,items,parent,number,endDate;
        Kind(String table,String items,String parent,String number,String endDate) { this.table=table; this.items=items; this.parent=parent; this.number=number; this.endDate=endDate; }
    }
    public record Query(int page,int size,String status,LocalDate from,LocalDate to,LocalDateTime since) {}
    public record Document(Long id,String number,String customer,String status,LocalDate issued,LocalDate end,
            String subtotal,String tax,String total,String paid,LocalDateTime updated) {}
    private final NamedParameterJdbcTemplate jdbc;
    public BillingReadRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static String money(ResultSet r,String name) throws SQLException {
        var v=r.getBigDecimal(name); return v==null?null:v.toPlainString();
    }
    private static LocalDateTime time(ResultSet r,String name) throws SQLException { return r.getObject(name,LocalDateTime.class); }
    private static String valueTime(Kind k) { return "(SELECT MAX(i.updated_at) FROM "+k.items+" i WHERE i."+k.parent+"=d.id)"; }
    private static String where(Kind k,Query q,Long id) {
        String date=k==Kind.EXPENSE?"expense_date":"issue_date";
        return " WHERE d.tenant_id=:tenant"+(id==null?"":" AND d.id=:id")
                +(q.status()==null?"":" AND d.status=:status")
                +(q.from()==null?"":" AND d."+date+">=:from")+(q.to()==null?"":" AND d."+date+"<=:to")
                +(q.since()==null?"":" AND (d.updated_at>=:since"+(k==Kind.EXPENSE?"":" OR EXISTS (SELECT 1 FROM "+k.items+" i WHERE i."+k.parent+"=d.id AND i.updated_at>=:since)")+")");
    }
    private static MapSqlParameterSource params(long tenant,Query q,Long id) {
        return new MapSqlParameterSource("tenant",tenant).addValue("id",id).addValue("status",q.status())
                .addValue("from",q.from()).addValue("to",q.to()).addValue("since",q.since())
                .addValue("limit",q.size()).addValue("offset",(long)q.page()*q.size());
    }
    public long count(Kind k,long tenant,Query q) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM "+k.table+" d"+where(k,q,null),params(tenant,q,null),Long.class);
    }
    public List<Document> documents(Kind k,long tenant,Query q,Long id) {
        if(k==Kind.EXPENSE) throw new IllegalArgumentException("Document kind required");
        return jdbc.query("SELECT d.id,d."+k.number+" AS document_number,d.customer_name,d.status,d.issue_date,d."+k.endDate+" AS end_date,"
                +"d.subtotal,d.tax_amount,d.total_amount,"+(k==Kind.INVOICE?"d.amount_paid":"NULL AS amount_paid")+",d.updated_at,"
                +valueTime(k)+" AS item_updated FROM "+k.table+" d"+where(k,q,id)+" ORDER BY d.id LIMIT :limit OFFSET :offset",params(tenant,q,id),
                (r,n)->{
                    var own=time(r,"updated_at"); var child=time(r,"item_updated");
                    var latest=own==null?child:child==null||own.isAfter(child)?own:child;
                    return new Document(r.getLong("id"),r.getString("document_number"),r.getString("customer_name"),r.getString("status"),
                            r.getObject("issue_date",LocalDate.class),r.getObject("end_date",LocalDate.class),money(r,"subtotal"),money(r,"tax_amount"),
                            money(r,"total_amount"),money(r,"amount_paid"),latest);
                });
    }
    public Map<Long,List<Line>> lines(Kind k,long tenant,List<Long> ids) {
        if(ids.isEmpty()) return Map.of();
        if(k==Kind.EXPENSE) throw new IllegalArgumentException("Document kind required");
        Map<Long,List<Line>> result=new HashMap<>();
        jdbc.query("SELECT i.id,i."+k.parent+" AS parent_id,i.description,i.quantity,i.unit_price,i.tax_rate,i.total_amount FROM "+k.items
                +" i JOIN "+k.table+" d ON d.id=i."+k.parent+" WHERE d.tenant_id=:tenant AND d.id IN (:ids) ORDER BY i.id",
                new MapSqlParameterSource("tenant",tenant).addValue("ids",ids),(org.springframework.jdbc.core.RowCallbackHandler)r->{
                    result.computeIfAbsent(r.getLong("parent_id"),key->new ArrayList<>()).add(new Line(r.getLong("id"),r.getString("description"),
                            money(r,"quantity"),money(r,"unit_price"),money(r,"tax_rate"),money(r,"total_amount")));
                });
        return result;
    }
    public List<Expense> expenses(long tenant,Query q,Long id) {
        return jdbc.query("SELECT d.id,d.expense_date,d.vendor_name,d.expense_category,d.reference_number,d.amount,d.tax_amount,d.updated_at "
                +"FROM business_expenses d"+where(Kind.EXPENSE,q,id)+" ORDER BY d.id LIMIT :limit OFFSET :offset",params(tenant,q,id),
                (r,n)->new Expense(r.getLong("id"),r.getObject("expense_date",LocalDate.class),r.getString("vendor_name"),r.getString("expense_category"),
                        r.getString("reference_number"),money(r,"amount"),money(r,"tax_amount"),time(r,"updated_at")));
    }
}
