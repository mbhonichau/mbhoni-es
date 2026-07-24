package com.mbhoni_creative.adminrepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.Tenant;
import java.time.LocalDate;
import com.mbhoni_creative.adminentity.InvoiceStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByTenantOrderByIssueDateDesc(Tenant tenant);
    boolean existsByInvoiceNumber(String invoiceNumber);
    
    List<Invoice> findByStatusInAndDueDateBefore(
            List<InvoiceStatus> statuses,
            LocalDate date);
    
    boolean existsByTenantIdAndStatus(Long tenantId, InvoiceStatus status);

    List<Invoice> findByStatus(InvoiceStatus status);
}