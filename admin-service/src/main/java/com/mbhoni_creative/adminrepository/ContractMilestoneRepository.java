package com.mbhoni_creative.adminrepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.ContractMilestone;

@Repository
public interface ContractMilestoneRepository extends JpaRepository<ContractMilestone, Long> {
    List<ContractMilestone> findByContractId(Long contractId);
}
