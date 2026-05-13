package com.intelligenthealthcare.auth.domain;

import com.intelligenthealthcare.patient.domain.model.PatientRole;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;

/**
 * 放入 {@link org.springframework.security.core.context.SecurityContext} 的患者身份信息。
 */
@Data
@Builder
public final class PatientAuthPrincipal {

    private final Long id;
    private final String phone;
    private final String username;
    private final PatientRole role;

    /**
     * 数据组装器
     * @param id
     * @param phone
     * @param username
     * @param role
     */
    public PatientAuthPrincipal(Long id, String phone, String username, PatientRole role) {
        this.id = Objects.requireNonNull(id, "id");
        this.phone = phone;
        this.username = username;
        this.role = role;
    }

}
