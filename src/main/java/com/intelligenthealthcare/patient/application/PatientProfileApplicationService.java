package com.intelligenthealthcare.patient.application;

import com.intelligenthealthcare.auth.api.dto.CurrentPatientResponse;
import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientProfileApplicationService {

    private final PatientRepository patientRepository;

    /**
     * 获取当前登录用户信息
     */
    public CurrentPatientResponse me(PatientAuthPrincipal principal) {
        return CurrentPatientResponse.fromEntity(patientRepository.findById(principal.getId()).orElseThrow(this::userNotFound));
    }

    /**
     * 更新个人资料（导诊上下文 + 基础信息）
     */
    @Transactional
    public CurrentPatientResponse updateMyProfile(
            PatientAuthPrincipal principal,
            String username,
            String phone,
            Integer patientAge,
            Gender patientGender,
            String residentCity,
            String area,
            TriagePrefer triagePrefer) {

        Patient patient = patientRepository.findById(principal.getId()).orElseThrow(this::userNotFound);

        // 手机号唯一性校验（DDD 应用层负责跨实体规则校验）
        validatePhoneUniqueness(patient, phone);

        // 领域实体自己更新（业务逻辑内聚在实体）
        patient.updateProfile(username, phone, patientAge, patientGender, residentCity, area, triagePrefer);
        patientRepository.save(patient);

        return CurrentPatientResponse.fromEntity(patient);
    }

    // ==================== 私有工具方法 ====================
    private void validatePhoneUniqueness(Patient patient, String newPhone) {
        if (newPhone == null || newPhone.isBlank()) return;

        boolean phoneExists = patientRepository.existsByPhone(newPhone.trim());
        boolean isSameUser = newPhone.trim().equals(patient.getPhone());

        if (!isSameUser && phoneExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该手机号已被使用");
        }
    }


    private ResponseStatusException userNotFound() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在或已删除");
    }
}