package com.intelligenthealthcare.audit.domain.model;

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

@TableName("ai_recall_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecallAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String symptoms;
    private String gender;
    private Integer age;

    @TableField("age_group")
    private String ageGroup;

    @TableField("eligible_disease_count")
    private Integer eligibleDiseaseCount;

    @TableField("rule_candidate_codes_json")
    private String ruleCandidateCodesJson;

    @TableField("suggested_codes_json")
    private String suggestedCodesJson;

    private String status;
    private String message;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
