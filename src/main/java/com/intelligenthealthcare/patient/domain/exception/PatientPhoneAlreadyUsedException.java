package com.intelligenthealthcare.patient.domain.exception;

/**
 * 患者手机号冲突。
 */
public class PatientPhoneAlreadyUsedException extends RuntimeException {

    public PatientPhoneAlreadyUsedException() {
        super("该手机号已被使用");
    }
}
