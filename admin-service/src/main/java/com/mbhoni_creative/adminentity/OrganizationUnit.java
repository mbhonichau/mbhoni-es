package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "organization_units")
public class OrganizationUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "unit_code", nullable = false, length = 100)
    private String unitCode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "unit_type", nullable = false, length = 50)
    private String unitType; // COMPANY, BUSINESS_UNIT, DIVISION, DEPARTMENT, TEAM, COST_CENTER

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    private OrganizationUnit parentUnit;

    @OneToMany(mappedBy = "parentUnit", cascade = CascadeType.ALL)
    private List<OrganizationUnit> childUnits = new ArrayList<>();

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Column(length = 150)
    private String location;

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public OrganizationUnit getParentUnit() { return parentUnit; }
    public void setParentUnit(OrganizationUnit parentUnit) { this.parentUnit = parentUnit; }

    public List<OrganizationUnit> getChildUnits() { return childUnits; }
    public void setChildUnits(List<OrganizationUnit> childUnits) { this.childUnits = childUnits; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
