package com.intelligenthealthcare.patient.domain.exception;

/**
 * 患者不存在或已删除。
 */
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException() {
        super("用户不存在或已删除");
    }
}
