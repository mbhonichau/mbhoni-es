package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.EmployeeFieldValue;

@Repository
public interface EmployeeFieldValueRepository extends JpaRepository<EmployeeFieldValue, Long> {

    List<EmployeeFieldValue> findByEmployeeId(Long employeeId);

    Optional<EmployeeFieldValue> findByEmployeeIdAndFieldDefinitionId(Long employeeId, Long fieldDefinitionId);

    void deleteByEmployeeId(Long employeeId);
}
