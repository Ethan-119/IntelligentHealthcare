package com.intelligenthealthcare.rag.domain.model;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 可检索语料的来源限界，患者域不参与向量建库。{@code KNOWLEDGE} 对应原知识目录（医院/病种/能力等）数据。
 */
@Getter
@AllArgsConstructor
public enum RagSourceType {
    KNOWLEDGE("KNOWLEDGE"),
    AUDIT("AUDIT"),
    IMPORT_JOB("IMPORT_JOB");

    @EnumValue
    @JsonValue
    private final String code;
}
