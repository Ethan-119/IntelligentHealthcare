package com.intelligenthealthcare.shared.security;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 将 Spring Security 认证上下文中的 {@link PatientAuthPrincipal} 写入线程局部变量，
 * 供 {@link CurrentPatientArgumentResolver} 在控制器方法参数注入时读取。
 * <p>
 * Redis token 有效性校验已前置到 {@code JwtAuthenticationFilter}，本拦截器不复核。
 */
@Component
@RequiredArgsConstructor
public class CurrentPatientContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof PatientAuthPrincipal principal) {
            CurrentPatientContext.set(principal);
            return true;
        }
        CurrentPatientContext.clear();
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentPatientContext.clear();
    }
}
