package com.intelligenthealthcare.patient.infrastructure.persistence;

import com.intelligenthealthcare.patient.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMedicalRecordNo(String medicalRecordNo);
}
