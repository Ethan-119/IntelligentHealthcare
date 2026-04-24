package com.intelligenthealthcare.knowledge.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("disease_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseMaster {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("disease_code")
    private String diseaseCode;

    @TableField("disease_name")
    private String diseaseName;

    @TableField("aliases_json")
    private String aliasesJson;

    @TableField("symptom_keywords")
    private String symptomKeywords;

    @TableField("gender_rule")
    private String genderRule;

    @TableField("age_min")
    private Integer ageMin;

    @TableField("age_max")
    private Integer ageMax;

    @TableField("age_group")
    private String ageGroup;

    @TableField("urgency_level")
    private String urgencyLevel;

    @TableField("review_status")
    private String reviewStatus;

    private Integer deleted;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
