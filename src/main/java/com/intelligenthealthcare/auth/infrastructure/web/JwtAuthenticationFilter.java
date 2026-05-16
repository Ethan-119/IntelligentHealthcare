package com.intelligenthealthcare.auth.infrastructure.web;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.auth.infrastructure.jwt.JwtService;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.PatientRole;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Collections;

/**
 * 无状态 API：从 {@code Authorization: Bearer &lt;token&gt;} 解析患者 ID，
 * 校验 Redis 中服务端 token 是否匹配（防止登出后 JWT 仍可用），
 * 再查库填充 {@code PatientAuthPrincipal}。
 * 解析失败不中断请求链，由 {@link com.intelligenthealthcare.shared.config.SecurityConfig} 的鉴权规则决定 401/放行。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_KEY_PREFIX = "auth:token:patient:";

    private final JwtService jwtService;
    private final PatientRepository patientRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            String raw = header.substring(BEARER_PREFIX.length()).trim();
            if (StringUtils.hasText(raw)) {
                try {
                    long patientId = jwtService.parsePatientIdOrThrow(raw);
                    // 校验 Redis 中服务端 token：登出或过期时 Redis key 不存在/不匹配，拒绝认证。
                    String cachedToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + patientId);
                    if (!StringUtils.hasText(cachedToken) || !raw.equals(cachedToken)) {
                        log.debug("JWT token 与 Redis 不匹配或已登出，patientId={}", patientId);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // 仅当主键仍存在于库中时建立登录态；已删除则视为未登录
                    patientRepository.findById(patientId).ifPresent(this::setContext);
                } catch (RuntimeException ex) {
                    // 签名错误、过期、ID 非数字等：记录日志便于排查，但不写入认证信息
                    log.debug("JWT 解析失败：{}", ex.getMessage());
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 将当前患者写入 {@code SecurityContext}，供控制器通过 {@code @AuthenticationPrincipal} 取当前用户。 */
    private void setContext(Patient patient) {
        PatientRole role = patient.getRole();
        if (role == null) {
            role = PatientRole.PATIENT;
        }
        String authority = "ROLE_" + role.name();
        var principal = PatientAuthPrincipal.builder()
                .id(patient.getId())
                .phone(patient.getPhone())
                .username(patient.getUsername())
                .role(role)
                .build();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
