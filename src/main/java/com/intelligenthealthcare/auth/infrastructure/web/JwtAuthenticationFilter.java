package com.intelligenthealthcare.auth.infrastructure.web;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.auth.infrastructure.jwt.JwtService;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.infrastructure.persistence.PatientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 无状态 API：从 {@code Authorization: Bearer &lt;token&gt;} 解析患者 ID，再查库填充 {@code PatientAuthPrincipal}。
 * 解析失败不中断请求链，由 {@link com.intelligenthealthcare.shared.config.SecurityConfig} 的鉴权规则决定 401/放行。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final PatientRepository patientRepository;

    public JwtAuthenticationFilter(JwtService jwtService, PatientRepository patientRepository) {
        this.jwtService = jwtService;
        this.patientRepository = patientRepository;
    }

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
                    // 仅当主键仍存在于库中时建立登录态；已删除则视为未登录
                    patientRepository.findById(patientId).ifPresent(this::setContext);
                } catch (RuntimeException ignored) {
                    // 签名错误、过期、ID 非数字等：不写入认证信息，等效于未登录访问
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /** 将当前患者写入 {@code SecurityContext}，供控制器通过 {@code @AuthenticationPrincipal} 取当前用户。 */
    private void setContext(Patient patient) {
        var principal = new PatientAuthPrincipal(
                patient.getId(), patient.getPhone(), patient.getEmail(), patient.getName());
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
