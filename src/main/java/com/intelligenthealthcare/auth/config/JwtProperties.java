package com.intelligenthealthcare.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 绑定 app.jwt.*；secret 与过期时间可在外部用环境变量覆盖。
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        @DefaultValue("86400000") long expirationMs) {
}
