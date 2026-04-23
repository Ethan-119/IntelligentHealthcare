package com.intelligenthealthcare.auth.api.dto;

import com.intelligenthealthcare.patient.domain.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 新患者注册入参；病历号由服务端生成，不从此 DTO 传入。
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 100, message = "密码长度 8~100 位")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 100)
    private String name;

    @Size(max = 120)
    @Email(message = "邮箱格式不正确")
    private String email;

    private Gender gender;

    private LocalDate birthDate;

    private String medicalHistory;
}
