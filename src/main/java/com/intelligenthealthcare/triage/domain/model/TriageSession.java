package com.intelligenthealthcare.triage.domain.model;

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

@TableName("triage_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_id")
    private String userId;

    @TableField("dialog_id")
    private String dialogId;

    @TableField("current_stage")
    private String currentStage;

    @TableField("ask_round")
    private Integer askRound;

    @TableField("invalid_answer_count")
    private Integer invalidAnswerCount;

    private String city;
    private String area;
    private Integer nearby;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @TableField("patient_age")
    private Integer patientAge;

    @TableField("patient_gender")
    private String patientGender;

    @TableField("severity_level")
    private String severityLevel;

    @TableField("route_type")
    private String routeType;

    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
