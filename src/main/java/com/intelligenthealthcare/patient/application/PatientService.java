package com.intelligenthealthcare.patient.application;

import com.intelligenthealthcare.patient.domain.model.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientService {

    Patient save(Patient patient);

    List<Patient> findAll();

    Optional<Patient> findById(Long id);
}
