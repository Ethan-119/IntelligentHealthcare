package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import java.util.List;
import java.util.Optional;

public interface HospitalRepository {
    List<Hospital> findAllActive();

    // 查询全部（含禁用），供管理后台使用
    List<Hospital> findAll();

    Optional<Hospital> findByHospitalId(String hospitalId);

    // 不限状态查询（管理后台切换状态用）
    Optional<Hospital> findByHospitalIdAll(String hospitalId);

    // 更新单条记录，供管理后台切换 activeStatus
    void update(Hospital entity);
}
