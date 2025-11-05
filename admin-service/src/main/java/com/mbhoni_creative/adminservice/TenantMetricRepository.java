package com.mbhoni_creative.adminservice;

import org.springframework.data.jpa.repository.JpaRepository;
public interface TenantMetricRepository extends JpaRepository<TenantMetric, String> {}