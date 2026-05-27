package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import java.util.List;
import java.util.Optional;

public interface HospitalDepartmentRepository {
    List<HospitalDepartment> findAllActive();

    // 查询全部（含禁用），供管理后台使用
    List<HospitalDepartment> findAll();

    Optional<HospitalDepartment> findById(Long id);

    // 不限状态查询（管理后台切换状态用）
    Optional<HospitalDepartment> findByIdAll(Long id);

    List<HospitalDepartment> findByHospitalId(String hospitalId);

    // 更新单条记录，供管理后台切换 activeStatus
    void update(HospitalDepartment entity);
}
