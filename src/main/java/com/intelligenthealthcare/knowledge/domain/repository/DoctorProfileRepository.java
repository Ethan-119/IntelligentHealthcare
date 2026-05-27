package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository {
    List<DoctorProfile> findHotDoctors(int limit);

    // 查询全部（含禁用），供管理后台使用
    List<DoctorProfile> findAll();

    Optional<DoctorProfile> findById(Long id);

    // 不限状态查询（管理后台切换状态用）
    Optional<DoctorProfile> findByIdAll(Long id);

    List<DoctorProfile> findByDepartmentId(Long departmentId);
    List<DoctorProfile> findByHospitalId(String hospitalId);

    // 更新单条记录，供管理后台切换 activeStatus
    void update(DoctorProfile entity);
}
