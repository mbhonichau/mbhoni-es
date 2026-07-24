package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.IndustryProfile;

public interface IndustryProfileRepository extends JpaRepository<IndustryProfile, Long> {

    Optional<IndustryProfile> findByCode(String code);

    List<IndustryProfile> findByActiveTrueOrderByNameAsc();
}