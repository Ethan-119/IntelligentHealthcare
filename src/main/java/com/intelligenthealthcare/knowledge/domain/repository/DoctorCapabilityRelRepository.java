package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DoctorCapabilityRel;
import java.util.List;

public interface DoctorCapabilityRelRepository {
    List<DoctorCapabilityRel> findByDoctorId(Long doctorId);
}
