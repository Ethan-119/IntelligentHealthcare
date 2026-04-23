package com.intelligenthealthcare.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录入参；{@link #getAccount} 在服务端按是否含 {@code '@'} 区分邮箱与手机号。
 */
@Data
public class LoginRequest {

    /**
     * 手机号或邮箱，用于定位账号。
     */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;
}
