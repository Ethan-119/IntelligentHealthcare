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

@TableName("hospital")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("hospital_id")
    private String hospitalId;

    @TableField("hospital_name")
    private String hospitalName;

    private String city;

    @TableField("district_name")
    private String districtName;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @TableField("hospital_level")
    private String hospitalLevel;

    @TableField("is_emergency")
    private Integer isEmergency;

    @TableField("authority_score")
    private BigDecimal authorityScore;

    @TableField("active_status")
    private Integer activeStatus;

    private Integer deleted;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
