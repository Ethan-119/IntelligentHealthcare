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

/**
 * 医疗能力/检查项等知识元数据。物理表名沿用 {@code medical_capability_catalog} 以兼容既有库，无需改表可照常映射。
 */
@TableName("medical_capability_catalog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalCapabilityKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("capability_code")
    private String capabilityCode;

    @TableField("capability_name")
    private String capabilityName;

    @TableField("capability_type")
    private String capabilityType;

    @TableField("parent_code")
    private String parentCode;

    @TableField("standard_dept_code")
    private String standardDeptCode;

    @TableField("aliases_json")
    private String aliasesJson;

    @TableField("gender_rule")
    private String genderRule;

    @TableField("age_min")
    private Integer ageMin;

    @TableField("age_max")
    private Integer ageMax;

    @TableField("crowd_tags_json")
    private String crowdTagsJson;

    @TableField("pathway_tags_json")
    private String pathwayTagsJson;

    @TableField("active_status")
    private Integer activeStatus;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
