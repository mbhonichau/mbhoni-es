package com.mbhoni_creative.adminentity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;

    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    private String department;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "time_zone")
    private String timeZone = "Africa/Johannesburg";

    @Column(name = "auth_provider", nullable = false)
    private String authProvider = "LOCAL";

    @Column(name = "provider_id")
    private String providerId;

    private boolean active = true;

    private boolean isGlobalAdmin = false;

    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired = false;

    @Column(name = "mfa_enforced", nullable = false)
    private boolean mfaEnforced = false;

    @Column(name = "sso_enforced", nullable = false)
    private boolean ssoEnforced = false;

    @Column(name = "api_access_allowed", nullable = false)
    private boolean apiAccessAllowed = true;

    @Column(name = "audit_extended", nullable = false)
    private boolean auditExtended = false;

    @Column(name = "password_reset_token", length = 100)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "national_id", length = 100)
    private String nationalId;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "extended_attributes_json", columnDefinition = "TEXT")
    private String extendedAttributesJson;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(String roleName) {
        return roles.stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isGlobalAdmin() {
        return isGlobalAdmin;
    }

    public void setGlobalAdmin(boolean globalAdmin) {
        isGlobalAdmin = globalAdmin;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
    }

    public boolean isMfaEnforced() {
        return mfaEnforced;
    }

    public void setMfaEnforced(boolean mfaEnforced) {
        this.mfaEnforced = mfaEnforced;
    }

    public boolean isSsoEnforced() {
        return ssoEnforced;
    }

    public void setSsoEnforced(boolean ssoEnforced) {
        this.ssoEnforced = ssoEnforced;
    }

    public boolean isApiAccessAllowed() {
        return apiAccessAllowed;
    }

    public void setApiAccessAllowed(boolean apiAccessAllowed) {
        this.apiAccessAllowed = apiAccessAllowed;
    }

    public boolean isAuditExtended() {
        return auditExtended;
    }

    public void setAuditExtended(boolean auditExtended) {
        this.auditExtended = auditExtended;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public LocalDateTime getPasswordResetTokenExpiry() {
        return passwordResetTokenExpiry;
    }

    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) {
        this.passwordResetTokenExpiry = passwordResetTokenExpiry;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getExtendedAttributesJson() { return extendedAttributesJson; }
    public void setExtendedAttributesJson(String extendedAttributesJson) { this.extendedAttributesJson = extendedAttributesJson; }
}
