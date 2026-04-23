package com.intelligenthealthcare.auth.api.dto;

import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.Patient;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * 对外返回的当前患者档案，不含 {@code passwordHash}。
 */
@Value
@Builder
public class CurrentPatientResponse {

    Long id;
    String name;
    String phone;
    String email;
    String medicalRecordNo;
    Gender gender;
    LocalDate birthDate;
    String medicalHistory;

    public static CurrentPatientResponse fromEntity(Patient p) {
        return CurrentPatientResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .phone(p.getPhone())
                .email(p.getEmail())
                .medicalRecordNo(p.getMedicalRecordNo())
                .gender(p.getGender())
                .birthDate(p.getBirthDate())
                .medicalHistory(p.getMedicalHistory())
                .build();
    }
}
