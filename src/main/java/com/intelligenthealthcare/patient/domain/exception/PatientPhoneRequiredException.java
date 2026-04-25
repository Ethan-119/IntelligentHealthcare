package com.intelligenthealthcare.patient.domain.exception;

/**
 * 患者手机号缺失。
 */
public class PatientPhoneRequiredException extends RuntimeException {

    public PatientPhoneRequiredException() {
        super("手机号不能为空");
    }
}
