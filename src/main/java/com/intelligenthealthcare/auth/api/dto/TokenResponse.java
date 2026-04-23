package com.intelligenthealthcare.auth.api.dto;

import lombok.Builder;
import lombok.Value;

/**
 * 注册/登录成功后的响应：客户端保存 {@code accessToken} 并在受保护请求上携带 {@code Authorization} 头。
 */
@Value
@Builder
public class TokenResponse {

    String accessToken;
    @Builder.Default
    String tokenType = "Bearer";
    long expiresInMs;
    CurrentPatientResponse user;
}
