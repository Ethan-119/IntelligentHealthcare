package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository {
    List<DoctorProfile> findHotDoctors(int limit);
    Optional<DoctorProfile> findById(Long id);
    List<DoctorProfile> findByDepartmentId(Long departmentId);
}
