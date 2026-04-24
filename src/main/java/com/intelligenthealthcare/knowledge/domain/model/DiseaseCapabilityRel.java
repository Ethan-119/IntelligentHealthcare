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

@TableName("disease_capability_rel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseCapabilityRel {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("disease_code")
    private String diseaseCode;

    @TableField("capability_code")
    private String capabilityCode;

    @TableField("rel_type")
    private String relType;

    @TableField("priority_score")
    private BigDecimal priorityScore;

    @TableField("crowd_constraint")
    private String crowdConstraint;

    private String note;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
