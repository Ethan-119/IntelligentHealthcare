package com.intelligenthealthcare.patient.api.dto;

import com.intelligenthealthcare.patient.domain.model.Gender;
import com.intelligenthealthcare.patient.domain.model.TriagePrefer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateMyProfileRequest {

    @Size(max = 64, message = "用户名最多 64 字")
    private String username;

    @NotBlank(message = "手机号不能为空")
    @Size(max = 32, message = "手机号最多 32 字")
    private String phone;

    private Integer patientAge;

    private Gender patientGender;

    @Size(max = 64, message = "城市最多 64 字")
    private String residentCity;

    @Size(max = 64, message = "区域最多 64 字")
    private String area;

    @NotNull(message = "就诊偏好不能为空")
    private TriagePrefer triagePrefer;
}
