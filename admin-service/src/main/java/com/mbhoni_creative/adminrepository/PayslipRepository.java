package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.mbhoni_creative.adminentity.Payslip;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    List<Payslip> findByTenantIdOrderByPayDateDesc(Long tenantId);
    List<Payslip> findByEmployeeIdOrderByPayDateDesc(Long employeeId);
    Optional<Payslip> findByPayslipNumber(String payslipNumber);
}
