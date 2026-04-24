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

@TableName("disease_alias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseAlias {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("disease_code")
    private String diseaseCode;

    @TableField("alias_name")
    private String aliasName;

    @TableField("alias_type")
    private String aliasType;

    private String source;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
