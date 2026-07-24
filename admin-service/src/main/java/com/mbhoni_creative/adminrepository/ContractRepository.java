package com.mbhoni_creative.adminrepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.Contract;
import com.mbhoni_creative.adminentity.ContractStatus;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    Optional<Contract> findByContractNumber(String contractNumber);
    List<Contract> findByTenantId(Long tenantId);
    List<Contract> findByStatus(ContractStatus status);
    List<Contract> findByEndDateBeforeAndStatus(LocalDate date, ContractStatus status);
}
