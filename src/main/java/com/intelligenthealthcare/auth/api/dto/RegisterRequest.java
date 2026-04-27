package com.intelligenthealthcare.auth.api.dto;

import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新用户注册入参。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 128, message = "密码长度 8~128 位")
    private String password;

    @Size(max = 64)
    private String username;

    private Integer patientAge;

    private Gender patientGender;

    @Size(max = 64)
    private String residentCity;

    @Size(max = 64)
    private String area;

    private TriagePrefer triagePrefer;
}
