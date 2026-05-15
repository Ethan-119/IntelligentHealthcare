package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.util.List;
import java.util.Optional;

public interface DiseaseMasterRepository {
    List<DiseaseMaster> findAllActive();
    Optional<DiseaseMaster> findByCode(String diseaseCode);
    List<DiseaseMaster> searchByKeyword(String keyword);
}
