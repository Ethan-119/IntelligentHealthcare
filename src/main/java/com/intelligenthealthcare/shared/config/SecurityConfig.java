package com.intelligenthealthcare.shared.config;

import com.intelligenthealthcare.auth.infrastructure.web.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * 无状态 JWT：关闭 Session、关闭 Basic 弹窗、在 UsernamePassword 过滤器前插入 {@link com.intelligenthealthcare.auth.infrastructure.web.JwtAuthenticationFilter}。
 * CSRF 对纯 Bearer API 可关闭；前后端分离下配合 CORS 与 OPTIONS 预检。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 与数据库存储的 {@code password} 密文字段配合，使用 BCrypt 自适应哈希。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 浏览器跨域预检
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 首页、健康检查、注册与登录不校验 JWT；管理端 /api/admin/** 与其余 /api 同策略：需已认证
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/api/health",
                                "/api/auth/register",
                                "/api/auth/login"
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .httpBasic(b -> b.disable())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 未携带或无效 token 时返回 JSON 401，避免默认 HTML 错误页
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    objectMapper.writeValueAsString(
                                            Map.of("message", "未登录或凭证无效")));
                        })
                );
        return http.build();
    }

    /** 开发期放宽来源；生产建议改为明确前端域名，并按需打开 credentials。 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("*"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setExposedHeaders(List.of("Authorization"));
        c.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }
}
