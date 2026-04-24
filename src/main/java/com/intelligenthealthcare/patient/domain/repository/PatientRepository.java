package com.intelligenthealthcare.patient.domain.repository;

import com.intelligenthealthcare.patient.domain.model.Patient;
import java.util.Optional;

/**
 * Patient 聚合根仓储抽象；application 层只依赖此接口，不感知 MyBatis 细节。
 */
public interface PatientRepository {

    // 必须！获取当前登录用户
    Optional<Patient> findById(Long id);

    boolean existsByPhone(String phone);

    /** 返回保存后的实体（获取自增 ID、填充字段）。 */
    Patient save(Patient patient);
}

