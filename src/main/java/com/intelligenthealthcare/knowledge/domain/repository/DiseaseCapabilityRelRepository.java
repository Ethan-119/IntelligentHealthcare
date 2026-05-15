package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseCapabilityRel;
import java.util.List;

public interface DiseaseCapabilityRelRepository {
    List<DiseaseCapabilityRel> findByDiseaseCode(String diseaseCode);
}
