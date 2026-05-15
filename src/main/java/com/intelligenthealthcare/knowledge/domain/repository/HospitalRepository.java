package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository {
    List<Hospital> findAllActive();
    Optional<Hospital> findByHospitalId(String hospitalId);
}
