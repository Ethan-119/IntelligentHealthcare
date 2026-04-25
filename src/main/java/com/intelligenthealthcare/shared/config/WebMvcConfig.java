package com.intelligenthealthcare.shared.config;

import com.intelligenthealthcare.shared.security.CurrentPatientArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web 层横切：注册 {@link com.intelligenthealthcare.shared.security.CurrentPatient} 等参数解析器。
 * 业务异常到 HTTP 的映射由 {@link com.intelligenthealthcare.shared.api.GlobalExceptionHandler} 统一处理。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentPatientArgumentResolver currentPatientArgumentResolver;

    public WebMvcConfig(CurrentPatientArgumentResolver currentPatientArgumentResolver) {
        this.currentPatientArgumentResolver = currentPatientArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentPatientArgumentResolver);
    }
}
