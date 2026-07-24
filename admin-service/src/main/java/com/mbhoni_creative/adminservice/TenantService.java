package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.admindto.TenantDto;

public interface TenantService {

    List<TenantDto> getAllTenants();

    TenantDto getTenantById(Long id);

    TenantDto saveTenant(TenantDto dto);

    TenantDto updateTenant(Long id, TenantDto dto);

    void deleteTenant(Long id);
}