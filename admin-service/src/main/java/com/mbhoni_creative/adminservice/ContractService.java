package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.Contract;
import com.mbhoni_creative.adminentity.ContractMilestone;
import com.mbhoni_creative.adminentity.ContractStatus;

public interface ContractService {
    List<Contract> getContractsByTenant(Long tenantId);
    Contract getContractById(Long contractId);
    Contract getContractByNumber(String contractNumber);
    Contract createOrUpdateContract(Long tenantId, Contract contract);
    Contract updateContractStatus(Long contractId, ContractStatus status);
    
    ContractMilestone addMilestone(Long contractId, ContractMilestone milestone);
    List<ContractMilestone> getContractMilestones(Long contractId);
}
