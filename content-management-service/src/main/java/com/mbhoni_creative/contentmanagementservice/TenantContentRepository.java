package com.mbhoni_creative.contentmanagementservice;

  import org.springframework.data.jpa.repository.JpaRepository;

  public interface TenantContentRepository extends JpaRepository<TenantContent, String> {
  }