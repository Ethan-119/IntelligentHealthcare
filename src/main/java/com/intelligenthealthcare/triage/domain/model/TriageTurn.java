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

@TableName("triage_turn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageTurn {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private String sessionId;

    @TableField("turn_no")
    private Integer turnNo;

    @TableField("user_message")
    private String userMessage;

    @TableField("normalized_query")
    private String normalizedQuery;

    private String intent;
    private String stage;

    @TableField("reply_text")
    private String replyText;

    @TableField("raw_decision_json")
    private String rawDecisionJson;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
