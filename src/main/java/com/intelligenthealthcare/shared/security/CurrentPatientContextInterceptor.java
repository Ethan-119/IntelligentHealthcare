package com.intelligenthealthcare.shared.security;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import com.intelligenthealthcare.auth.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 将当前请求中的认证患者写入线程上下文，供参数解析器统一读取。
 * 当前仅用于 /api/auth/me：在 JWT 已通过后，追加 Redis 会话一致性校验。
 */
@Component
public class CurrentPatientContextInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public CurrentPatientContextInterceptor(StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 第一步：先取出 Spring Security 已解析的登录用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PatientAuthPrincipal principal) {
            // 第二步：取请求头中的 Bearer token
            String requestToken = extractBearerToken(request);
            // 第三步：按用户 ID 从 Redis 取服务端保存的 token
            String tokenKey = buildTokenKey(principal.getId());
            String cachedToken = stringRedisTemplate.opsForValue().get(tokenKey);
            // 第四步：只有 Redis 与请求 token 一致时才写入线程上下文，并刷新 TTL
            if (StringUtils.hasText(requestToken)
                    && StringUtils.hasText(cachedToken)
                    && requestToken.equals(cachedToken)) {
                CurrentPatientContext.set(principal);
                stringRedisTemplate.expire(tokenKey, jwtProperties.expirationMs(), TimeUnit.MILLISECONDS);
                return true;
            }
        }
        // 不在这里拦截，交给登录拦截器处理
        CurrentPatientContext.clear();
        return true;
   }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束统一清理，避免线程复用导致脏数据
        CurrentPatientContext.clear();
    }

    private static String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String raw = header.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(raw) ? raw : null;
    }

    private static String buildTokenKey(Long patientId) {
        return "auth:token:patient:" + patientId;
    }
}
