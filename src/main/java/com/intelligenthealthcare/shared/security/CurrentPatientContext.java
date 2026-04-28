package com.intelligenthealthcare.shared.security;

import com.intelligenthealthcare.auth.domain.PatientAuthPrincipal;

/**
 * 线程内当前登录患者上下文。
 * 后续可在拦截器中扩展为 Redis 会话校验后再写入此上下文。
 */
public final class CurrentPatientContext {

    private static final ThreadLocal<PatientAuthPrincipal> HOLDER = new ThreadLocal<>();

    private CurrentPatientContext() {
    }

    public static void set(PatientAuthPrincipal principal) {
        HOLDER.set(principal);
    }

    public static PatientAuthPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
