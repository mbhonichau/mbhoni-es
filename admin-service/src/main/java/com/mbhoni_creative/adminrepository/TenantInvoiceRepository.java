package com.mbhoni_creative.adminrepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.TenantInvoice;

@Repository
public interface TenantInvoiceRepository extends JpaRepository<TenantInvoice, Long> {
    List<TenantInvoice> findByTenantIdOrderByIdDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
