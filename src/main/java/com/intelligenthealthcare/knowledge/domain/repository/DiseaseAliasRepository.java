package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import java.util.List;

public interface DiseaseAliasRepository {
    List<DiseaseAlias> findByDiseaseCode(String diseaseCode);
}
