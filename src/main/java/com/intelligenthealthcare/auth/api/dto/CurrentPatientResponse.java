package com.intelligenthealthcare.auth.api.dto;

import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import lombok.Builder;
import lombok.Value;

/**
 * 对外返回的当前用户信息，不含密码。
 */
@Value
@Builder
public class CurrentPatientResponse {

    Long id;
    String phone;
    String username;
    Integer status;
    Integer patientAge;
    Gender patientGender;
    String residentCity;
    String area;
    TriagePrefer triagePrefer;

    public static CurrentPatientResponse fromEntity(Patient p) {
        return CurrentPatientResponse.builder()
                .id(p.getId())
                .phone(p.getPhone())
                .username(p.getUsername())
                .status(p.getStatus())
                .patientAge(p.getPatientAge())
                .patientGender(p.getPatientGender())
                .residentCity(p.getResidentCity())
                .area(p.getArea())
                .triagePrefer(p.getTriagePrefer())
                .build();
    }
}
