package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "employee_number", nullable = false, unique = true, length = 100)
    private String employeeNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "work_phone", length = 50)
    private String workPhone;

    @Column(name = "job_title", length = 150)
    private String jobTitle;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organization_unit_id")
    private OrganizationUnit organizationUnit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "manager_employee_id")
    private Employee manager;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 50)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 50)
    private ComplianceEmploymentStatus complianceEmploymentStatus = ComplianceEmploymentStatus.PENDING_ONBOARDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "background_check_status", nullable = false, length = 50)
    private BackgroundCheckStatus backgroundCheckStatus = BackgroundCheckStatus.NOT_STARTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_provider_id")
    private Tenant serviceProvider;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "national_id", length = 100)
    private String nationalId;

    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "license_category", length = 50)
    private String licenseCategory;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @Column(name = "residential_address", length = 500)
    private String residentialAddress;

    @Column(name = "extended_attributes_json", columnDefinition = "TEXT")
    private String extendedAttributesJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getWorkPhone() { return workPhone; }
    public void setWorkPhone(String workPhone) { this.workPhone = workPhone; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public OrganizationUnit getOrganizationUnit() { return organizationUnit; }
    public void setOrganizationUnit(OrganizationUnit organizationUnit) { this.organizationUnit = organizationUnit; }

    public Employee getManager() { return manager; }
    public void setManager(Employee manager) { this.manager = manager; }

    public String getCostCenter() { return costCenter; }
    public void setCostCenter(String costCenter) { this.costCenter = costCenter; }

    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public String getLicenseCategory() { return licenseCategory; }
    public void setLicenseCategory(String licenseCategory) { this.licenseCategory = licenseCategory; }

    public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
    public void setLicenseExpiryDate(LocalDate licenseExpiryDate) { this.licenseExpiryDate = licenseExpiryDate; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getEmergencyContactRelation() { return emergencyContactRelation; }
    public void setEmergencyContactRelation(String emergencyContactRelation) { this.emergencyContactRelation = emergencyContactRelation; }

    public String getResidentialAddress() { return residentialAddress; }
    public void setResidentialAddress(String residentialAddress) { this.residentialAddress = residentialAddress; }

    public String getExtendedAttributesJson() { return extendedAttributesJson; }
    public void setExtendedAttributesJson(String extendedAttributesJson) { this.extendedAttributesJson = extendedAttributesJson; }

    public ComplianceEmploymentStatus getComplianceEmploymentStatus() { return complianceEmploymentStatus; }
    public void setComplianceEmploymentStatus(ComplianceEmploymentStatus complianceEmploymentStatus) { this.complianceEmploymentStatus = complianceEmploymentStatus; }

    public BackgroundCheckStatus getBackgroundCheckStatus() { return backgroundCheckStatus; }
    public void setBackgroundCheckStatus(BackgroundCheckStatus backgroundCheckStatus) { this.backgroundCheckStatus = backgroundCheckStatus; }

    public Tenant getServiceProvider() { return serviceProvider; }
    public void setServiceProvider(Tenant serviceProvider) { this.serviceProvider = serviceProvider; }
}
