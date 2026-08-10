package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.Contract;
import com.mbhoni_creative.adminentity.ContractMilestone;
import com.mbhoni_creative.adminentity.ContractStatus;
import com.mbhoni_creative.adminservice.ContractService;
import com.mbhoni_creative.config.TenantAccessService;

@RestController
@RequestMapping("/api/contracts")
public class ContractApiController {

    private final ContractService contractService;
    private final TenantAccessService tenantAccessService;

    public ContractApiController(ContractService contractService, TenantAccessService tenantAccessService) {
        this.contractService = contractService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<Contract>> getContracts(@RequestParam(required = false) Long tenantId) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(contractService.getContractsByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<Contract> getContract(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        tenantAccessService.requireAccess(contract.getTenant() != null ? contract.getTenant().getId() : null);
        return ResponseEntity.ok(contract);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTRACT_EDIT')")
    public ResponseEntity<Contract> createContract(@RequestParam(required = false) Long tenantId, @RequestBody Contract contract) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(contractService.createOrUpdateContract(tenantId, contract));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CONTRACT_EDIT')")
    public ResponseEntity<Contract> updateStatus(@PathVariable Long id, @RequestParam ContractStatus status) {
        Contract contract = contractService.getContractById(id);
        tenantAccessService.requireAccess(contract.getTenant() != null ? contract.getTenant().getId() : null);
        return ResponseEntity.ok(contractService.updateContractStatus(id, status));
    }

    @PostMapping("/{id}/milestones")
    @PreAuthorize("hasAuthority('CONTRACT_EDIT')")
    public ResponseEntity<ContractMilestone> addMilestone(@PathVariable Long id, @RequestBody ContractMilestone milestone) {
        Contract contract = contractService.getContractById(id);
        tenantAccessService.requireAccess(contract.getTenant() != null ? contract.getTenant().getId() : null);
        return ResponseEntity.ok(contractService.addMilestone(id, milestone));
    }

    @GetMapping("/{id}/milestones")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<ContractMilestone>> getMilestones(@PathVariable Long id) {
        Contract contract = contractService.getContractById(id);
        tenantAccessService.requireAccess(contract.getTenant() != null ? contract.getTenant().getId() : null);
        return ResponseEntity.ok(contractService.getContractMilestones(id));
    }
}
