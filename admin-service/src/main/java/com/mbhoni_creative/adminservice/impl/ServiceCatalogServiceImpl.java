package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.ServiceCatalog;
import com.mbhoni_creative.adminentity.ServiceSla;
import com.mbhoni_creative.adminrepository.ServiceCatalogRepository;
import com.mbhoni_creative.adminrepository.ServiceSlaRepository;
import com.mbhoni_creative.adminservice.ServiceCatalogService;

@Service
@Transactional
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

    private final ServiceCatalogRepository serviceRepository;
    private final ServiceSlaRepository slaRepository;
    private final com.mbhoni_creative.config.TenantSecurityService security;

    public ServiceCatalogServiceImpl(
            ServiceCatalogRepository serviceRepository,
            ServiceSlaRepository slaRepository, com.mbhoni_creative.config.TenantSecurityService security) {
        this.serviceRepository = serviceRepository;
        this.slaRepository = slaRepository;
        this.security = security;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalog> getAllServices() {
        return serviceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceCatalog getServiceByCode(String code) {
        return serviceRepository.findByServiceCode(code)
                .orElseThrow(() -> new RuntimeException("Service not found with code: " + code));
    }

    @Override
    public ServiceCatalog saveService(ServiceCatalog serviceCatalog) {
        requirePlatformAdmin();
        return serviceRepository.save(serviceCatalog);
    }

    @Override
    public ServiceSla addSla(Long serviceCatalogId, ServiceSla sla) {
        requirePlatformAdmin();
        ServiceCatalog serviceCatalog = serviceRepository.findById(serviceCatalogId)
                .orElseThrow(() -> new RuntimeException("Service not found with ID: " + serviceCatalogId));
        sla.setServiceCatalog(serviceCatalog);
        return slaRepository.save(sla);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceSla> getSlasByServiceId(Long serviceCatalogId) {
        return slaRepository.findByServiceCatalogId(serviceCatalogId);
    }
    private void requirePlatformAdmin() {
        if (!security.isGlobalAdmin()) throw new org.springframework.security.access.AccessDeniedException("Shared service catalog requires platform admin");
    }
}
