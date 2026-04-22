package com.intelligenthealthcare.repository;

import com.intelligenthealthcare.entity.patient.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByMedicalRecordNo(String medicalRecordNo);
}
