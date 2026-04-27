package com.intelligenthealthcare.auth.domain;

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

    /**
     * 数据组装器
     * @param id
     * @param phone
     * @param username
     */
    public PatientAuthPrincipal(Long id, String phone, String username) {
        this.id = Objects.requireNonNull(id, "id");
        this.phone = phone;
        this.username = username;
    }

}
