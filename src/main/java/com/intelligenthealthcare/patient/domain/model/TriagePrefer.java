package com.intelligenthealthcare.patient.domain.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TriagePrefer {
    NEARBY("nearby"),
    AUTHORITY("authority"),
    EMERGENCY("emergency");

    @EnumValue
    @JsonValue
    private final String code;
}
