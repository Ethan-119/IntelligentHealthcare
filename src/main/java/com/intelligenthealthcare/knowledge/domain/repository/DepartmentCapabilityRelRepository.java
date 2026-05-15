package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DepartmentCapabilityRel;
import java.util.List;

public interface DepartmentCapabilityRelRepository {
    List<DepartmentCapabilityRel> findByDepartmentId(Long departmentId);
    List<DepartmentCapabilityRel> findByCapabilityCode(String capabilityCode);
}
