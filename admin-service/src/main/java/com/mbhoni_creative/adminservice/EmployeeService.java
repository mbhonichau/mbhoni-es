package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;

public interface EmployeeService {
    List<Employee> getEmployeesByTenant(Long tenantId);
    Employee getEmployeeById(Long id);
    Employee getEmployeeByNumber(String employeeNumber);
    Employee saveEmployee(Long tenantId, Long orgUnitId, Long managerId, Employee employee);
    Employee updateEmployeeStatus(Long id, EmployeeStatus status);
}
