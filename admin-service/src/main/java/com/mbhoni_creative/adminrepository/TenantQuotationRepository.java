package com.mbhoni_creative.adminrepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.TenantQuotation;

@Repository
public interface TenantQuotationRepository extends JpaRepository<TenantQuotation, Long> {
    List<TenantQuotation> findByTenantIdOrderByIdDesc(Long tenantId);
    long countByTenantId(Long tenantId);
}
