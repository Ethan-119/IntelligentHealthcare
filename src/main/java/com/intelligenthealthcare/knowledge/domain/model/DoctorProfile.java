package com.intelligenthealthcare.knowledge.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("doctor_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hospital_id")
    private String hospitalId;

    @TableField("department_id")
    private Long departmentId;

    @TableField("doctor_name")
    private String doctorName;

    private String title;

    @TableField("specialty_text")
    private String specialtyText;

    @TableField("gender_rule")
    private String genderRule;

    @TableField("age_min")
    private Integer ageMin;

    @TableField("age_max")
    private Integer ageMax;

    @TableField("crowd_tags_json")
    private String crowdTagsJson;

    @TableField("authority_score")
    private BigDecimal authorityScore;

    @TableField("academic_title_score")
    private BigDecimal academicTitleScore;

    @TableField("is_expert")
    private Integer isExpert;

    @TableField("campus_name")
    private String campusName;

    @TableField("active_status")
    private Integer activeStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
