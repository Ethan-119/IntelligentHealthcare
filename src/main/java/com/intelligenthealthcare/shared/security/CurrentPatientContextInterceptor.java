package com.intelligenthealthcare.shared.security;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.auth.infrastructure.jwt.JwtService;
import com.intelligenthealthcare.patient.domain.model.Patient;
import com.intelligenthealthcare.patient.domain.model.PatientRole;
import com.intelligenthealthcare.patient.domain.repository.PatientRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义 JWT 鉴权拦截器：
 * 1) 兼容旧的 @CurrentPatient 参数注入（写入 CurrentPatientContext）；
 * 2) 统一处理未登录/无权限的 401/403（无需 Spring Security 过滤器链）。
 */
@Component
@RequiredArgsConstructor
public class CurrentPatientContextInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_KEY_PREFIX = "auth:token:patient:";

    private final JwtService jwtService;
    private final PatientRepository patientRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        CurrentPatientContext.clear();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            return true;
        }

        PatientAuthPrincipal principal = resolvePrincipal(request);
        if (principal == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "未登录或凭证无效");
            return false;
        }
        if (isAdminPath(path) && principal.getRole() != PatientRole.ADMIN) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "当前账号没有权限执行该操作。");
            return false;
        }
        CurrentPatientContext.set(principal);
        return true;
    }

    private PatientAuthPrincipal resolvePrincipal(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String raw = header.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        try {
            long patientId = jwtService.parsePatientIdOrThrow(raw);
            String cachedToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + patientId);
            if (!StringUtils.hasText(cachedToken) || !raw.equals(cachedToken)) {
                return null;
            }
            Patient patient = patientRepository.findById(patientId).orElse(null);
            if (patient == null) {
                return null;
            }
            if (!Integer.valueOf(1).equals(patient.getStatus())
                    || (patient.getDeleted() != null && Integer.valueOf(1).equals(patient.getDeleted()))) {
                return null;
            }
            PatientRole role = patient.getRole() == null ? PatientRole.PATIENT : patient.getRole();
            return PatientAuthPrincipal.builder()
                    .id(patient.getId())
                    .phone(patient.getPhone())
                    .username(patient.getUsername())
                    .role(role)
                    .build();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean isPublicPath(String path) {
        if (!StringUtils.hasText(path)) {
            return true;
        }
        String normalizedPath = normalizePath(path);
        return "/".equals(normalizedPath)
                || "/error".equals(normalizedPath)
                || "/health".equals(normalizedPath)
                || normalizedPath.startsWith("/auth/");
    }

    private static boolean isAdminPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        return normalizePath(path).startsWith("/admin/");
    }

    private static String normalizePath(String rawPath) {
        String path = rawPath == null ? "" : rawPath.trim();
        if (!StringUtils.hasText(path)) {
            return "";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.startsWith("/api/")) {
            return path.substring(4);
        }
        if ("/api".equals(path)) {
            return "/";
        }
        return path;
    }

    private static void writeError(HttpServletResponse response, int status, String message) {
        try {
            response.setStatus(status);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            String body = "{\"message\":\"" + escapeJson(message) + "\"}";
            response.getWriter().write(body);
        } catch (IOException ignored) {
            // ignore
        }
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentPatientContext.clear();
    }
}
