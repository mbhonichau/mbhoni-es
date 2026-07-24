package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.ServiceCatalog;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    Optional<ServiceCatalog> findByServiceCode(String serviceCode);
    List<ServiceCatalog> findByStatus(String status);
    List<ServiceCatalog> findByCategory(String category);
}
