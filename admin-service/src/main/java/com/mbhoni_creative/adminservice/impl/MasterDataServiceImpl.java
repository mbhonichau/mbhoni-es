package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.LookupCode;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.LookupCategoryRepository;
import com.mbhoni_creative.adminrepository.LookupCodeRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.MasterDataService;

@Service
@Transactional
public class MasterDataServiceImpl implements MasterDataService {

    private final LookupCategoryRepository categoryRepository;
    private final LookupCodeRepository codeRepository;
    private final TenantRepository tenantRepository;

    public MasterDataServiceImpl(
            LookupCategoryRepository categoryRepository,
            LookupCodeRepository codeRepository,
            TenantRepository tenantRepository) {
        this.categoryRepository = categoryRepository;
        this.codeRepository = codeRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupCategory> getAllCategories(Long tenantId) {
        return categoryRepository.findByTenantIdOrTenantIsNull(tenantId);
    }

    @Override
    public LookupCategory createCategory(LookupCategory category, Long tenantId) {
        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
            category.setTenant(tenant);
        }
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public LookupCategory getCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));
    }

    @Override
    @Transactional(readOnly = true)
    public LookupCategory getCategoryByCode(String code) {
        return categoryRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Category not found with code: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupCode> getActiveLookupCodes(String categoryCode) {
        return codeRepository.findByCategoryCodeAndActiveTrueOrderByDisplayOrderAsc(categoryCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupCode> getLookupCodesByCategoryId(Long categoryId) {
        return codeRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId);
    }

    @Override
    @Transactional(readOnly = true)
    public LookupCode getLookupCodeById(Long codeId) {
        return codeRepository.findById(codeId)
                .orElseThrow(() -> new RuntimeException("Lookup code not found with ID: " + codeId));
    }

    @Override
    public LookupCode saveLookupCode(Long categoryId, LookupCode code) {
        LookupCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found with ID: " + categoryId));
        code.setCategory(category);
        return codeRepository.save(code);
    }

    @Override
    public void deleteLookupCode(Long codeId) {
        codeRepository.deleteById(codeId);
    }
}
