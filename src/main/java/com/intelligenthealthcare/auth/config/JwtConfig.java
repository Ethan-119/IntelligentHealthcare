package com.intelligenthealthcare.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 将 {@link JwtProperties} 注册为 Bean，与 {@code application.yml} 中 {@code app.jwt.*} 绑定。 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
}
