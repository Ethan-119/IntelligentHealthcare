package com.intelligenthealthcare.patient.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import com.intelligenthealthcare.patient.domain.exception.PatientPhoneRequiredException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户聚合根：登录账号 + AI导诊上下文
 * 标准 DDD 实体：包含状态 + 领域行为
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("patient")
public class Patient {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 登录账号
    private String phone;
    private String username;
    private String password;

    // 账号状态
    @Builder.Default
    private Integer status = 1;

    // AI 导诊上下文
    private Integer patientAge;

    @TableField("patient_gender")
    @Builder.Default
    private Gender patientGender = Gender.UNKNOWN;

    @TableField("resident_city")
    private String residentCity;

    private String area;

    @TableField("triage_prefer")
    @Builder.Default
    private TriagePrefer triagePrefer = TriagePrefer.NEARBY;

    // 通用
    @Builder.Default
    private Integer deleted = 0;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 更新个人资料 + 导诊上下文
     */
    public void updateProfile(
            String username,
            String phone,
            Integer patientAge,
            Gender patientGender,
            String residentCity,
            String area,
            TriagePrefer triagePrefer
    ) {
        // 用户名
        this.username = trim(username);

        // 手机号（必须有值）
        if (phone == null || phone.isBlank()) {
            throw new PatientPhoneRequiredException();
        }
        this.phone = phone.trim();

        // 年龄
        this.patientAge = patientAge;

        // 性别（默认未知）
        this.patientGender = patientGender == null ? Gender.UNKNOWN : patientGender;

        // 城市、区域
        this.residentCity = trim(residentCity);
        this.area = trim(area);

        // 导诊偏好（默认就近）
        this.triagePrefer = triagePrefer == null ? TriagePrefer.NEARBY : triagePrefer;
    }

    /**
     *  trim 工具：内部使用，DDD 实体内部工具方法
     */
    private String trim(String str) {
        return str == null ? null : str.trim();
    }
}