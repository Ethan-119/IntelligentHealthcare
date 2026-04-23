package com.intelligenthealthcare.patient.infrastructure.persistence;

import com.intelligenthealthcare.patient.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 患者即当前系统的登录主体；查重与按账号查询供 {@link com.intelligenthealthcare.auth.application.AuthService} 使用。
 */
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMedicalRecordNo(String medicalRecordNo);

    Optional<Patient> findByPhone(String phone);

    Optional<Patient> findByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByMedicalRecordNo(String medicalRecordNo);
}
