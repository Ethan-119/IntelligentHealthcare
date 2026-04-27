package com.intelligenthealthcare.patient.domain.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 生理性别；用于健康档案与统计，推荐场景请同时尊重用户自填的性别认同表述（可扩展其他字段）。
 */
@Getter
@AllArgsConstructor
public enum Gender {
    MALE("male"),
    FEMALE("female"),
    UNKNOWN("unknown");

    @EnumValue
    @JsonValue
    private final String code;
}
