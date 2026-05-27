package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import java.util.List;
import java.util.Optional;

public interface MedicalCapabilityRepository {
    List<MedicalCapabilityKnowledge> findAllActive();

    // 查询全部（含禁用），供管理后台使用
    List<MedicalCapabilityKnowledge> findAll();

    // 不限状态查询（管理后台切换状态用）
    Optional<MedicalCapabilityKnowledge> findByCodeAll(String capabilityCode);

    // 更新单条记录，供管理后台切换 activeStatus
    void update(MedicalCapabilityKnowledge entity);
}
