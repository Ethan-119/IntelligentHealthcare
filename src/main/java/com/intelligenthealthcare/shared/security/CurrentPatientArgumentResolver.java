package com.intelligenthealthcare.shared.security;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * 统一解析当前登录用户，避免在控制器里重复空值校验。
 */
@Component
public class CurrentPatientArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPatient.class)
                && PatientAuthPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        PatientAuthPrincipal principal = CurrentPatientContext.get();
        if (principal != null) {
            return principal;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
    }
}
