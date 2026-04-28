package com.intelligenthealthcare.shared.config;

import com.intelligenthealthcare.shared.security.CurrentPatientArgumentResolver;
import com.intelligenthealthcare.shared.security.CurrentPatientContextInterceptor;
import com.intelligenthealthcare.shared.security.LoginRequiredInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 层横切：注册 {@link com.intelligenthealthcare.shared.security.CurrentPatient} 等参数解析器。
 * 业务异常到 HTTP 的映射由 {@link com.intelligenthealthcare.shared.api.GlobalExceptionHandler} 统一处理。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentPatientArgumentResolver currentPatientArgumentResolver;
    private final CurrentPatientContextInterceptor currentPatientContextInterceptor;
    private final LoginRequiredInterceptor loginRequiredInterceptor;

    public WebMvcConfig(
            CurrentPatientArgumentResolver currentPatientArgumentResolver,
            CurrentPatientContextInterceptor currentPatientContextInterceptor,
            LoginRequiredInterceptor loginRequiredInterceptor) {
        this.currentPatientArgumentResolver = currentPatientArgumentResolver;
        this.currentPatientContextInterceptor = currentPatientContextInterceptor;
        this.loginRequiredInterceptor = loginRequiredInterceptor;
    }

    @Override
    // 注册自定义参数解析器，让控制器可直接使用 @CurrentPatient 注入当前登录用户
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentPatientArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(currentPatientContextInterceptor)
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(loginRequiredInterceptor)
                .addPathPatterns("/api/auth/me")
                .order(1);
    }
}
