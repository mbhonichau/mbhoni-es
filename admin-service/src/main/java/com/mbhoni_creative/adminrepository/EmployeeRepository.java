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
    // JSON-only getUserId()/getManagerId() accessors are not mapped JPA attributes.
    @org.springframework.data.jpa.repository.Query("select e from Employee e where e.user.id = :userId")
    Optional<Employee> findByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);
    List<Employee> findByTenantIdAndStatus(Long tenantId, EmployeeStatus status);
    @org.springframework.data.jpa.repository.Query("select e from Employee e where e.manager.id = :managerId")
    List<Employee> findByManagerId(@org.springframework.data.repository.query.Param("managerId") Long managerId);
}
