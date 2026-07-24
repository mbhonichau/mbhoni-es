package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.LookupCode;

@Repository
public interface LookupCodeRepository extends JpaRepository<LookupCode, Long> {
    List<LookupCode> findByCategoryCodeAndActiveTrueOrderByDisplayOrderAsc(String categoryCode);
    List<LookupCode> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);
    Optional<LookupCode> findByCategoryCodeAndCode(String categoryCode, String code);
}
