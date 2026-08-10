package com.mbhoni_creative.adminservice.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.EmployeeCompletenessReportDto;
import com.mbhoni_creative.adminentity.BackgroundCheckStatus;
import com.mbhoni_creative.adminentity.ComplianceEmploymentStatus;

import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeFieldValue;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminentity.FieldDataType;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.EmployeeFieldValueRepository;
import com.mbhoni_creative.adminrepository.EmployeeRepository;
import com.mbhoni_creative.adminrepository.OrganizationUnitRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.EmployeeService;
import com.mbhoni_creative.adminservice.FieldDefinitionService;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;
    private final OrganizationUnitRepository orgUnitRepository;
    private final FieldDefinitionService fieldDefinitionService;
    private final EmployeeFieldValueRepository fieldValueRepository;
    private final com.mbhoni_creative.adminrepository.PayslipRepository payslipRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            TenantRepository tenantRepository,
            OrganizationUnitRepository orgUnitRepository,
            FieldDefinitionService fieldDefinitionService,
            EmployeeFieldValueRepository fieldValueRepository,
            com.mbhoni_creative.adminrepository.PayslipRepository payslipRepository) {
        this.employeeRepository = employeeRepository;
        this.tenantRepository = tenantRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.fieldDefinitionService = fieldDefinitionService;
        this.fieldValueRepository = fieldValueRepository;
        this.payslipRepository = payslipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByTenant(Long tenantId) {
        return tenantId != null ? employeeRepository.findByTenantId(tenantId) : employeeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getEmployeeByNumber(String employeeNumber) {
        return employeeRepository.findByEmployeeNumber(employeeNumber)
                .orElseThrow(() -> new RuntimeException("Employee not found with number: " + employeeNumber));
    }

    @Override
    public Employee saveEmployee(Long tenantId, Long orgUnitId, Long managerId, Employee employee) {
        // Enforce immutable compliance state-machine & system core invariants
        if (employee.getFirstName() == null || employee.getFirstName().isBlank()) {
            throw new IllegalArgumentException("First Name is a mandatory system core invariant and cannot be empty.");
        }
        if (employee.getLastName() == null || employee.getLastName().isBlank()) {
            throw new IllegalArgumentException("Last Name is a mandatory system core invariant and cannot be empty.");
        }
        if (employee.getEmail() == null || employee.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email Address is a mandatory system core invariant and cannot be empty.");
        }

        if (employee.getId() != null) {
            Employee existing = getEmployeeById(employee.getId());

            existing.setFirstName(employee.getFirstName());
            existing.setLastName(employee.getLastName());
            existing.setEmail(employee.getEmail());
            existing.setJobTitle(employee.getJobTitle());
            existing.setEmploymentType(employee.getEmploymentType());
            existing.setHireDate(employee.getHireDate());
            existing.setNationalId(employee.getNationalId());
            existing.setTaxNumber(employee.getTaxNumber());
            existing.setWorkPhone(employee.getWorkPhone());
            existing.setCostCenter(employee.getCostCenter());
            existing.setLicenseNumber(employee.getLicenseNumber());
            existing.setLicenseCategory(employee.getLicenseCategory());
            existing.setLicenseExpiryDate(employee.getLicenseExpiryDate());
            existing.setEmergencyContactName(employee.getEmergencyContactName());
            existing.setEmergencyContactPhone(employee.getEmergencyContactPhone());
            existing.setEmergencyContactRelation(employee.getEmergencyContactRelation());
            existing.setResidentialAddress(employee.getResidentialAddress());
            existing.setExtendedAttributesJson(employee.getExtendedAttributesJson());
            
            if (employee.getStatus() != null) {
                existing.setStatus(employee.getStatus());
            }
            if (employee.getComplianceEmploymentStatus() != null) {
                existing.setComplianceEmploymentStatus(employee.getComplianceEmploymentStatus());
            }
            if (employee.getBackgroundCheckStatus() != null) {
                existing.setBackgroundCheckStatus(employee.getBackgroundCheckStatus());
            }

            if (orgUnitId != null) {
                OrganizationUnit orgUnit = orgUnitRepository.findById(orgUnitId).orElse(null);
                existing.setOrganizationUnit(orgUnit);
            } else {
                existing.setOrganizationUnit(null);
            }

            if (managerId != null) {
                Employee manager = employeeRepository.findById(managerId).orElse(null);
                existing.setManager(manager);
            } else {
                existing.setManager(null);
            }

            validateComplianceStatusGates(existing);

            return employeeRepository.save(existing);
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
        employee.setTenant(tenant);

        if (orgUnitId != null) {
            OrganizationUnit orgUnit = orgUnitRepository.findById(orgUnitId).orElse(null);
            employee.setOrganizationUnit(orgUnit);
        }

        if (managerId != null) {
            Employee manager = employeeRepository.findById(managerId).orElse(null);
            employee.setManager(manager);
        }

        if (employee.getBackgroundCheckStatus() == BackgroundCheckStatus.PASSED) {
            if (employee.getStatus() == null) {
                employee.setStatus(EmployeeStatus.ACTIVE);
            }
            if (employee.getComplianceEmploymentStatus() == null) {
                employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.ACTIVE);
            }
        } else {
            if (employee.getComplianceEmploymentStatus() == null || employee.getComplianceEmploymentStatus() == ComplianceEmploymentStatus.ACTIVE) {
                employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.PENDING_ONBOARDING);
            }
            if (employee.getStatus() == null || employee.getStatus() == EmployeeStatus.ACTIVE) {
                employee.setStatus(EmployeeStatus.PROBATION);
            }
        }

        validateComplianceStatusGates(employee);

        return employeeRepository.save(employee);
    }

    @Override
    public Employee updateEmployeeStatus(Long id, EmployeeStatus status) {
        Employee employee = getEmployeeById(id);
        employee.setStatus(status);
        validateComplianceStatusGates(employee);
        return employeeRepository.save(employee);
    }

    @Override
    public void deleteEmployee(Long id) {
        Employee employee = getEmployeeById(id);

        // Cascade disassociate managed employees
        List<Employee> managed = employeeRepository.findAll().stream()
                .filter(e -> e.getManager() != null && e.getManager().getId().equals(id))
                .collect(Collectors.toList());
        for (Employee emp : managed) {
            emp.setManager(null);
            employeeRepository.save(emp);
        }

        // Delete dependent employee field values
        fieldValueRepository.deleteByEmployeeId(id);

        // Delete dependent payslips
        payslipRepository.deleteByEmployeeId(id);

        employeeRepository.delete(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeCompletenessReportDto> getCompletenessReport(Long tenantId) {
        List<Employee> employees = getEmployeesByTenant(tenantId);
        List<FieldDefinition> schema = fieldDefinitionService.getAllEffectiveSchemaForTenant(tenantId);
        List<FieldDefinition> requiredDefs = schema.stream()
                .filter(FieldDefinition::isRequired)
                .collect(Collectors.toList());

        List<EmployeeCompletenessReportDto> reportList = new ArrayList<>();

        for (Employee emp : employees) {
            List<EmployeeFieldValue> storedValues = fieldValueRepository.findByEmployeeId(emp.getId());
            Map<Long, String> valueMap = storedValues.stream()
                    .filter(v -> v.getFieldDefinition() != null)
                    .collect(Collectors.toMap(v -> v.getFieldDefinition().getId(), v -> v.getValue() != null ? v.getValue() : "", (v1, v2) -> v1));

            List<String> missingKeys = new ArrayList<>();
            List<String> missingLabels = new ArrayList<>();

            for (FieldDefinition req : requiredDefs) {
                String val = valueMap.get(req.getId());
                if (val == null || val.trim().isEmpty()) {
                    missingKeys.add(req.getFieldKey());
                    missingLabels.add(req.getLabel());
                }
            }

            EmployeeCompletenessReportDto dto = new EmployeeCompletenessReportDto();
            dto.setEmployeeId(emp.getId());
            dto.setEmployeeNumber(emp.getEmployeeNumber());
            dto.setEmployeeName(emp.getFirstName() + " " + emp.getLastName());
            dto.setTenantId(emp.getTenant() != null ? emp.getTenant().getId() : tenantId);
            dto.setComplete(missingKeys.isEmpty());
            dto.setMissingFieldKeys(missingKeys);
            dto.setMissingFieldLabels(missingLabels);

            reportList.add(dto);
        }

        return reportList;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateFieldAssignmentEligibility(Long employeeId) {
        Employee employee = getEmployeeById(employeeId);

        // Gate 1: Must be ACTIVE status
        if (employee.getStatus() != EmployeeStatus.ACTIVE && employee.getComplianceEmploymentStatus() != ComplianceEmploymentStatus.ACTIVE) {
            throw new IllegalStateException("Field Assignment Gate Violation: Employee status must be ACTIVE to receive new assignments.");
        }

        // Gate 2: No required, still-open EXPIRY / DATE-typed field values are past due
        Long tenantId = employee.getTenant() != null ? employee.getTenant().getId() : null;
        List<FieldDefinition> schema = fieldDefinitionService.getAllEffectiveSchemaForTenant(tenantId);
        List<FieldDefinition> requiredDateDefs = schema.stream()
                .filter(d -> d.isRequired() && d.getDataType() == com.mbhoni_creative.adminentity.DataType.DATE)
                .collect(Collectors.toList());

        List<EmployeeFieldValue> storedValues = fieldValueRepository.findByEmployeeId(employeeId);
        Map<Long, String> valueMap = storedValues.stream()
                .filter(v -> v.getFieldDefinition() != null)
                .collect(Collectors.toMap(v -> v.getFieldDefinition().getId(), v -> v.getValue() != null ? v.getValue() : "", (v1, v2) -> v1));

        for (FieldDefinition def : requiredDateDefs) {
            String val = valueMap.get(def.getId());
            if (val != null && !val.trim().isEmpty()) {
                try {
                    LocalDate expiryDate = LocalDate.parse(val.trim());
                    if (expiryDate.isBefore(LocalDate.now())) {
                        throw new IllegalStateException("Field Assignment Gate Violation: Required compliance field '" + def.getLabel() + "' is expired (" + val + ").");
                    }
                } catch (Exception e) {
                    if (e instanceof IllegalStateException) throw (IllegalStateException) e;
                }
            }
        }
    }

    private void validateComplianceStatusGates(Employee employee) {
        // Hardcoded Compliance Gate: Block employmentStatus = ACTIVE unless backgroundCheckStatus = PASSED
        if ((employee.getComplianceEmploymentStatus() == ComplianceEmploymentStatus.ACTIVE || employee.getStatus() == EmployeeStatus.ACTIVE)
                && employee.getBackgroundCheckStatus() != BackgroundCheckStatus.PASSED) {
            throw new IllegalStateException("Compliance State Machine Gate: Employment status cannot be ACTIVE unless backgroundCheckStatus is PASSED (current: " + employee.getBackgroundCheckStatus() + ").");
        }
    }
}
