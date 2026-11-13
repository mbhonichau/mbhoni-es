package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "organization_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "unit_id"}))
public class OrganizationMembership {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "unit_id", nullable = false) private OrganizationUnit unit;
    @Column(nullable = false) private boolean administrator;
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public OrganizationUnit getUnit() { return unit; }
    public void setUnit(OrganizationUnit unit) { this.unit = unit; }
    public boolean isAdministrator() { return administrator; }
    public void setAdministrator(boolean administrator) { this.administrator = administrator; }
}
