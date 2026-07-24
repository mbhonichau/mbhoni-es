package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.ServiceCatalog;
import com.mbhoni_creative.adminentity.ServiceSla;

public interface ServiceCatalogService {
    List<ServiceCatalog> getAllServices();
    ServiceCatalog getServiceByCode(String code);
    ServiceCatalog saveService(ServiceCatalog serviceCatalog);
    ServiceSla addSla(Long serviceCatalogId, ServiceSla sla);
    List<ServiceSla> getSlasByServiceId(Long serviceCatalogId);
}
