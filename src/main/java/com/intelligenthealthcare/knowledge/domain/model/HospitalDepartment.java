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

@TableName("hospital_department")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDepartment {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hospital_id")
    private String hospitalId;

    @TableField("department_name")
    private String departmentName;

    @TableField("parent_department_name")
    private String parentDepartmentName;

    @TableField("department_intro")
    private String departmentIntro;

    @TableField("service_scope")
    private String serviceScope;

    @TableField("active_status")
    private Integer activeStatus;

    private Integer deleted;

    @TableField("gender_rule")
    private String genderRule;

    @TableField("age_min")
    private Integer ageMin;

    @TableField("age_max")
    private Integer ageMax;

    @TableField("crowd_tags_json")
    private String crowdTagsJson;

    @TableField("standard_dept_code")
    private String standardDeptCode;

    @TableField("subspecialty_code")
    private String subspecialtyCode;

    @TableField("district_name")
    private String districtName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @TableField("is_emergency")
    private Integer isEmergency;

    @TableField("national_key_score")
    private BigDecimal nationalKeyScore;

    @TableField("provincial_key_score")
    private BigDecimal provincialKeyScore;

    @TableField("city_key_score")
    private BigDecimal cityKeyScore;

    @TableField("authority_score")
    private BigDecimal authorityScore;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
