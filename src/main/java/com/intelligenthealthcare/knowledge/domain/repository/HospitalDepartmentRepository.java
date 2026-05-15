package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import java.util.List;
import java.util.Optional;

public interface HospitalDepartmentRepository {
    List<HospitalDepartment> findAllActive();
    Optional<HospitalDepartment> findById(Long id);
    List<HospitalDepartment> findByHospitalId(String hospitalId);
}
