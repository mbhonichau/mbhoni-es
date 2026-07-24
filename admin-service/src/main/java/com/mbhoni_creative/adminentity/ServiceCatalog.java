package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_catalogs")
public class ServiceCatalog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_code", nullable = false, unique = true, length = 100)
    private String serviceCode;

    @Column(name = "service_name", nullable = false, length = 150)
    private String serviceName;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category; // INFRASTRUCTURE, CORE_API, INTEGRATION, BUSINESS

    @Column(name = "owner_team", length = 100)
    private String ownerTeam;

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, DEPRECATED, BETA

    @Column(name = "health_endpoint", length = 255)
    private String healthEndpoint;

    @OneToMany(mappedBy = "serviceCatalog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ServiceSla> slas = new ArrayList<>();

    public Long getId() { return id; }

    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getOwnerTeam() { return ownerTeam; }
    public void setOwnerTeam(String ownerTeam) { this.ownerTeam = ownerTeam; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHealthEndpoint() { return healthEndpoint; }
    public void setHealthEndpoint(String healthEndpoint) { this.healthEndpoint = healthEndpoint; }

    public List<ServiceSla> getSlas() { return slas; }
    public void setSlas(List<ServiceSla> slas) { this.slas = slas; }
}
