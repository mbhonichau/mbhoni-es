package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeStatus;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    List<Employee> findByTenantId(Long tenantId);
    Optional<Employee> findByEmployeeNumber(String employeeNumber);
    Optional<Employee> findByUserId(Long userId);
    List<Employee> findByTenantIdAndStatus(Long tenantId, EmployeeStatus status);
    List<Employee> findByManagerId(Long managerId);
}
