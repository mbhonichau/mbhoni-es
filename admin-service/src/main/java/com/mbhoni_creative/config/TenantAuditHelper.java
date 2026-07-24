package com.mbhoni_creative.config;

import org.springframework.stereotype.Component;

import com.mbhoni_creative.security.TenantContext;

@Component
public class TenantAuditHelper {

    public Long getTenantId() {
        return TenantContext.getTenantId();
    }
}