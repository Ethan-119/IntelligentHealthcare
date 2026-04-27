package com.intelligenthealthcare.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参；当前仅支持手机号登录。
 */
@Data
public class LoginRequest {

    /**
     * 手机号，用于定位账号。
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
