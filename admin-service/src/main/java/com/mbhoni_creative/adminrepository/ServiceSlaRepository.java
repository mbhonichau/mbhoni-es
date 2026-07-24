package com.mbhoni_creative.adminrepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.ServiceSla;

@Repository
public interface ServiceSlaRepository extends JpaRepository<ServiceSla, Long> {
    List<ServiceSla> findByServiceCatalogId(Long serviceCatalogId);
}
