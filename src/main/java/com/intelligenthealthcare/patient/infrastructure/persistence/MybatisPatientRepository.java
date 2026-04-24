package com.intelligenthealthcare.patient.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisPatientRepository implements PatientRepository {

    private final PatientMapper patientMapper;

    @Override
    public boolean existsByPhone(String phone) {
        return patientMapper.selectCount(
            new LambdaQueryWrapper<Patient>().eq(Patient::getPhone, phone)
        ) > 0;
    }
    
    @Override
    public Optional<Patient> findById(Long id) {
        return Optional.ofNullable(patientMapper.selectById(id));
    }

    @Override
    public Patient save(Patient patient) {
        if (patient.getId() == null) {
            patientMapper.insert(patient);
            return patient;
        } else {
            patientMapper.updateById(patient);
            return patient;
        }
    }
}
