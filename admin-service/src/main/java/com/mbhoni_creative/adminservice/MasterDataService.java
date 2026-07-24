package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.LookupCode;

public interface MasterDataService {
    List<LookupCategory> getAllCategories(Long tenantId);
    LookupCategory createCategory(LookupCategory category, Long tenantId);
    LookupCategory getCategoryByCode(String code);
    
    List<LookupCode> getActiveLookupCodes(String categoryCode);
    List<LookupCode> getLookupCodesByCategoryId(Long categoryId);
    LookupCode saveLookupCode(Long categoryId, LookupCode code);
    void deleteLookupCode(Long codeId);
}
