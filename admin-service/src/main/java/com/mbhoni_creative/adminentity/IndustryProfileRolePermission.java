package com.mbhoni_creative.adminentity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "industry_profile_role_permissions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_industry_profile_role_permission",
                columnNames = {"industry_profile_role_id", "permission_code"}
        )
)
public class IndustryProfileRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "industry_profile_role_id", nullable = false)
    private IndustryProfileRole industryProfileRole;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    public Long getId() {
        return id;
    }

    public IndustryProfileRole getIndustryProfileRole() {
        return industryProfileRole;
    }

    public void setIndustryProfileRole(IndustryProfileRole industryProfileRole) {
        this.industryProfileRole = industryProfileRole;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}