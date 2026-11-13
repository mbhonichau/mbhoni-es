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
import com.mbhoni_creative.config.TenantAccessService;
import org.springframework.security.access.AccessDeniedException;

@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractMilestoneRepository milestoneRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessService access;

    public ContractServiceImpl(
            ContractRepository contractRepository,
            ContractMilestoneRepository milestoneRepository,
            TenantRepository tenantRepository, TenantAccessService access) {
        this.contractRepository = contractRepository;
        this.milestoneRepository = milestoneRepository;
        this.tenantRepository = tenantRepository;
        this.access = access;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Contract> getContractsByTenant(Long tenantId) {
        tenantId = access.resolveAccessibleTenantId(tenantId);
        return tenantId != null ? contractRepository.findByTenantId(tenantId) : contractRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getContractById(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found with ID: " + contractId));
        authorize(contract);
        return contract;
    }

    @Override
    @Transactional(readOnly = true)
    public Contract getContractByNumber(String contractNumber) {
        Contract contract = contractRepository.findByContractNumber(contractNumber)
                .orElseThrow(() -> new RuntimeException("Contract not found with number: " + contractNumber));
        authorize(contract);
        return contract;
    }

    @Override
    public Contract createOrUpdateContract(Long tenantId, Contract contract) {
        tenantId = access.resolveAccessibleTenantId(tenantId);
        access.requireAccess(tenantId);
        Contract target = contract.getId() == null ? new Contract() : getContractById(contract.getId());
        if (target.getTenant() != null && !tenantId.equals(target.getTenant().getId()))
            throw new AccessDeniedException("Contracts cannot be reassigned between tenants");
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        // Never merge a payload's tenant or cascading child graph.
        target.setTenant(tenant);
        target.setContractNumber(contract.getContractNumber()); target.setTitle(contract.getTitle());
        target.setContractType(contract.getContractType()); target.setStatus(contract.getStatus());
        target.setStartDate(contract.getStartDate()); target.setEndDate(contract.getEndDate());
        target.setTotalValue(contract.getTotalValue()); target.setCurrency(contract.getCurrency());
        target.setAutoRenew(contract.isAutoRenew()); target.setNoticePeriodDays(contract.getNoticePeriodDays());
        target.setDocumentUrl(contract.getDocumentUrl()); target.setTermsAndConditions(contract.getTermsAndConditions());
        if (contract.getMilestones() != null) for (ContractMilestone child : contract.getMilestones()) {
            ContractMilestone saved = copyMilestone(target, child);
            if (saved.getId() == null) target.getMilestones().add(saved);
        }
        return contractRepository.save(target);
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
        return milestoneRepository.save(copyMilestone(contract, milestone));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractMilestone> getContractMilestones(Long contractId) {
        getContractById(contractId);
        return milestoneRepository.findByContractId(contractId);
    }

    private void authorize(Contract contract) {
        access.requireAccess(contract.getTenant() == null ? null : contract.getTenant().getId());
    }

    private ContractMilestone copyMilestone(Contract contract, ContractMilestone source) {
        ContractMilestone target = source.getId() == null ? new ContractMilestone() : milestoneRepository.findById(source.getId())
                .orElseThrow(() -> new IllegalArgumentException("Milestone not found"));
        if (target.getId() != null && (target.getContract() == null || contract.getId() == null
                || !contract.getId().equals(target.getContract().getId())))
            throw new AccessDeniedException("Milestone belongs to another contract");
        target.setContract(contract); target.setTitle(source.getTitle()); target.setDueDate(source.getDueDate());
        target.setAmount(source.getAmount()); target.setStatus(source.getStatus()); target.setCompletionDate(source.getCompletionDate());
        return target;
    }
}
