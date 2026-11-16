package com.mbhoni_creative.adminservice.impl;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.MasterDataService;
import com.mbhoni_creative.config.*;

@Service
@Transactional
public class MasterDataServiceImpl implements MasterDataService {
    private final LookupCategoryRepository categoryRepository;
    private final LookupCodeRepository codeRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessService access;
    private final TenantGovernance governance;
    public MasterDataServiceImpl(LookupCategoryRepository categories, LookupCodeRepository codes,
            TenantRepository tenants, TenantAccessService access, TenantGovernance governance) {
        this.categoryRepository = categories; this.codeRepository = codes; this.tenantRepository = tenants;
        this.access = access; this.governance = governance;
    }
    @Override @Transactional(readOnly = true)
    public List<LookupCategory> getAllCategories(Long tenantId) {
        tenantId = access.resolveAccessibleTenantId(tenantId);
        Map<String, LookupCategory> effective = new TreeMap<>();
        List<LookupCategory> records = categoryRepository.findByTenantIdOrTenantIsNull(tenantId);
        records.stream().filter(c -> c.getTenant() == null).forEach(c -> effective.put(c.getCode(), c));
        records.stream().filter(c -> c.getTenant() != null).forEach(c -> effective.put(c.getCode(), c));
        return new ArrayList<>(effective.values());
    }
    @Override
    public LookupCategory createCategory(LookupCategory source, Long tenantId) {
        tenantId = access.resolveAccessibleTenantId(tenantId);
        governance.requireDefaultsWrite(tenantId);
        if (source.getId() != null) rawCategory(source.getId());
        if (tenantId != null) tenantRepository.lockById(tenantId).orElseThrow();
        LookupCategory target;
        if (source.getId() != null) {
            LookupCategory stored = rawCategory(source.getId());
            if (!Objects.equals(source.getCode(), stored.getCode())) throw new IllegalArgumentException("Category keys cannot be changed");
            target = writableCategory(stored, tenantId);
        } else {
            if (source.getCode() == null || source.getCode().isBlank()) throw new IllegalArgumentException("Category code required");
            Optional<LookupCategory> duplicate = tenantId == null ? categoryRepository.findByCodeAndTenantIsNull(source.getCode())
                    : categoryRepository.findByCodeAndTenantId(source.getCode(), tenantId);
            if (duplicate.isPresent()) throw new IllegalArgumentException("Category key already exists in this scope");
            target = new LookupCategory(); target.setCode(source.getCode());
            if (tenantId != null) target.setTenant(tenantRepository.findById(tenantId).orElseThrow());
        }
        // Validate nested identifiers before persisting any client-controlled content.
        for (LookupCode code : source.getLookupCodes()) if (code.getId() != null) {
            LookupCode stored = getLookupCodeById(code.getId());
            if (source.getId() == null || !source.getId().equals(stored.getCategory().getId()))
                throw new AccessDeniedException("Lookup code category cannot be reassigned");
        }
        target.setName(source.getName()); target.setDescription(source.getDescription());
        if (tenantId == null) target.setSystem(source.isSystem());
        categoryRepository.saveAndFlush(target);
        for (LookupCode code : source.getLookupCodes()) saveCode(target, code);
        return target;
    }
    @Override @Transactional(readOnly = true)
    public LookupCategory getCategoryById(Long id) {
        LookupCategory category = rawCategory(id);
        Long tenantId = access.resolveAccessibleTenantId(null);
        if (category.getTenant() == null && tenantId != null)
            return categoryRepository.findByCodeAndTenantId(category.getCode(), tenantId).orElse(category);
        return category;
    }
    @Override @Transactional(readOnly = true)
    public LookupCategory getCategoryByCode(String code) {
        Long tenantId = access.resolveAccessibleTenantId(null);
        return (tenantId == null ? Optional.<LookupCategory>empty() : categoryRepository.findByCodeAndTenantId(code, tenantId))
                .or(() -> categoryRepository.findByCodeAndTenantIsNull(code)).orElseThrow();
    }
    @Override @Transactional(readOnly = true)
    public List<LookupCode> getActiveLookupCodes(String code) {
        return getLookupCodesByCategoryId(getCategoryByCode(code).getId()).stream().filter(LookupCode::isActive).toList();
    }
    @Override @Transactional(readOnly = true)
    public List<LookupCode> getLookupCodesByCategoryId(Long id) {
        LookupCategory category = getCategoryById(id);
        Map<String, LookupCode> effective = new LinkedHashMap<>();
        if (category.getTenant() != null) categoryRepository.findByCodeAndTenantIsNull(category.getCode()).ifPresent(global ->
                codeRepository.findByCategoryIdOrderByDisplayOrderAsc(global.getId()).forEach(c -> effective.put(c.getCode(), c)));
        codeRepository.findByCategoryIdOrderByDisplayOrderAsc(category.getId()).forEach(c -> effective.put(c.getCode(), c));
        return effective.values().stream().sorted(Comparator.comparing(LookupCode::getDisplayOrder,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(LookupCode::getCode)).toList();
    }
    @Override @Transactional(readOnly = true)
    public LookupCode getLookupCodeById(Long id) {
        LookupCode code = codeRepository.findById(id).orElseThrow();
        requireCategoryAccess(code.getCategory()); return code;
    }
    @Override
    public LookupCode saveLookupCode(Long categoryId, LookupCode source) {
        LookupCategory storedCategory = rawCategory(categoryId);
        if (source.getId() != null) {
            LookupCode stored = getLookupCodeById(source.getId());
            if (!Objects.equals(stored.getCategory().getId(), categoryId)
                    && !(stored.getCategory().getTenant() == null && Objects.equals(stored.getCategory().getCode(), storedCategory.getCode())))
                throw new AccessDeniedException("Lookup code category cannot be reassigned");
            if (!Objects.equals(source.getCode(), stored.getCode())) throw new IllegalArgumentException("Lookup keys cannot be changed");
        }
        Long scope = storedCategory.getTenant() == null ? access.resolveAccessibleTenantId(null) : storedCategory.getTenant().getId();
        LookupCategory target = writableCategory(storedCategory, scope);
        categoryRepository.saveAndFlush(target);
        return saveCode(target, source);
    }
    private LookupCode saveCode(LookupCategory target, LookupCode source) {
        LookupCode code = codeRepository.findByCategoryIdAndCode(target.getId(), source.getCode()).orElseGet(LookupCode::new);
        code.setCategory(target); code.setCode(source.getCode()); code.setValue(source.getValue()); code.setLocale(source.getLocale());
        code.setDisplayOrder(source.getDisplayOrder()); code.setActive(source.isActive()); code.setMetadata(source.getMetadata());
        return codeRepository.save(code);
    }
    @Override
    public void deleteLookupCode(Long id) {
        LookupCode stored = getLookupCodeById(id);
        LookupCategory category = stored.getCategory();
        Long scope = category.getTenant() == null ? access.resolveAccessibleTenantId(null) : category.getTenant().getId();
        LookupCategory target = writableCategory(category, scope); categoryRepository.saveAndFlush(target);
        LookupCode tombstone = new LookupCode(); tombstone.setCode(stored.getCode()); tombstone.setValue(stored.getValue());
        tombstone.setLocale(stored.getLocale()); tombstone.setDisplayOrder(stored.getDisplayOrder()); tombstone.setMetadata(stored.getMetadata());
        tombstone.setActive(false); saveCode(target, tombstone);
    }
    private LookupCategory writableCategory(LookupCategory stored, Long scope) {
        governance.requireDefaultsWrite(scope);
        if (scope != null) tenantRepository.lockById(scope).orElseThrow();
        if (stored.getTenant() != null) {
            if (!Objects.equals(scope, stored.getTenant().getId())) throw new AccessDeniedException("Category tenant cannot be reassigned");
            return stored;
        }
        if (scope == null) return stored;
        return categoryRepository.findByCodeAndTenantId(stored.getCode(), scope).orElseGet(() -> {
            LookupCategory copy = new LookupCategory(); copy.setTenant(tenantRepository.findById(scope).orElseThrow());
            copy.setCode(stored.getCode()); copy.setName(stored.getName()); copy.setDescription(stored.getDescription()); return copy;
        });
    }
    private LookupCategory rawCategory(Long id) {
        LookupCategory category = categoryRepository.findById(id).orElseThrow(); requireCategoryAccess(category); return category;
    }
    private void requireCategoryAccess(LookupCategory category) {
        if (category == null) throw new AccessDeniedException("Lookup category required");
        access.requireAccessIfTenantOwned(category.getTenant() == null ? null : category.getTenant().getId());
    }
}
