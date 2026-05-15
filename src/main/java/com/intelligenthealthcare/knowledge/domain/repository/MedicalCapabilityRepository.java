package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import java.util.List;

public interface MedicalCapabilityRepository {
    List<MedicalCapabilityKnowledge> findAllActive();
}
