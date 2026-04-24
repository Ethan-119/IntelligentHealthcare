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

@TableName("department_capability_rel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentCapabilityRel {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("department_id")
    private Long departmentId;

    @TableField("capability_code")
    private String capabilityCode;

    @TableField("support_level")
    private String supportLevel;

    private BigDecimal weight;

    private String source;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
