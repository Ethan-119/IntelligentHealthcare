package com.intelligenthealthcare.triage.domain.model;

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

@TableName("triage_slot_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageSlotState {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("symptoms_json")
    private String symptomsJson;

    @TableField("disease_name")
    private String diseaseName;

    @TableField("target_hospital")
    private String targetHospital;

    @TableField("target_department")
    private String targetDepartment;

    @TableField("target_doctor")
    private String targetDoctor;

    @TableField("missing_slots_json")
    private String missingSlotsJson;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
