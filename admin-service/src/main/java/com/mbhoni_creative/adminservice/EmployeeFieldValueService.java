package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Map;
import com.mbhoni_creative.adminentity.EmployeeFieldValue;

public interface EmployeeFieldValueService {

    List<EmployeeFieldValue> getValuesForEmployee(Long employeeId);

    Map<String, String> getValueMapForEmployee(Long employeeId);

    void saveValues(Long employeeId, Map<String, String> fieldValues);
}
