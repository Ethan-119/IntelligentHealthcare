package com.intelligenthealthcare.auth.domain;

import com.intelligenthealthcare.patient.domain.model.PatientRole;
import lombok.Builder;
import lombok.Value;

/**
 * 放入 {@link org.springframework.security.core.context.SecurityContext} 的患者身份信息。
 */
@Value
@Builder
public class PatientAuthPrincipal {

    Long id;
    String phone;
    String username;
    PatientRole role;
}
