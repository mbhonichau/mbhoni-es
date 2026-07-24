package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.Contract;
import com.mbhoni_creative.adminentity.ContractMilestone;
import com.mbhoni_creative.adminentity.ContractStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.ContractMilestoneRepository;
import com.mbhoni_creative.adminrepository.ContractRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.ContractService;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractMilestoneRepository milestoneRepository;
    private final TenantRepository tenantRepository;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractMilestoneRepository milestoneRepository,
            TenantRepository tenantRepository) {
        this.contractRepository = contractRepository;
        this.milestoneRepository = milestoneRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getContractsByTenant(Long tenantId) {
        return tenantId != null ? contractRepository.findByTenantId(tenantId) : contractRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getContractById(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with ID: " + contractId));
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getContractByNumber(String contractNumber) {
        return contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new RuntimeException("Contract not found with number: " + contractNumber));
    }

    @Override
    public Contract createOrUpdateContract(Long tenantId, Contract contract) {
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            contract.setTenant(tenant);
        }
        return contractRepository.save(contract);
    }

    @Override
    public Contract updateContractStatus(Long contractId, ContractStatus status) {
        Contract contract = getContractById(contractId);
        contract.setStatus(status);
        return contractRepository.save(contract);
    }

    @Override
    public ContractMilestone addMilestone(Long contractId, ContractMilestone milestone) {
        Contract contract = getContractById(contractId);
        milestone.setContract(contract);
        return milestoneRepository.save(milestone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractMilestone> getContractMilestones(Long contractId) {
        return milestoneRepository.findByContractId(contractId);
    }
}
