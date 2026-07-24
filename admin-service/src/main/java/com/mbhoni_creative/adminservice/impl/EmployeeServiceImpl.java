package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.EmployeeRepository;
import com.mbhoni_creative.adminrepository.OrganizationUnitRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.EmployeeService;

@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final TenantRepository tenantRepository;
    private final OrganizationUnitRepository orgUnitRepository;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            TenantRepository tenantRepository,
            OrganizationUnitRepository orgUnitRepository) {
        this.employeeRepository = employeeRepository;
        this.tenantRepository = tenantRepository;
        this.orgUnitRepository = orgUnitRepository;
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

        return employeeRepository.save(employee);
    }

    @Override
    public Employee updateEmployeeStatus(Long id, EmployeeStatus status) {
        Employee employee = getEmployeeById(id);
        employee.setStatus(status);
        return employeeRepository.save(employee);
    }
}
