package com.intelligenthealthcare.entity.triage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "triage_turn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageTurn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;
    @Column(name = "turn_no", nullable = false)
    private Integer turnNo;
    @Column(name = "user_message", columnDefinition = "TEXT")
    private String userMessage;
    @Column(name = "normalized_query", columnDefinition = "TEXT")
    private String normalizedQuery;
    @Column(name = "intent", length = 64)
    private String intent;
    @Column(name = "stage", length = 32)
    private String stage;
    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;
    @Column(name = "raw_decision_json", columnDefinition = "TEXT")
    private String rawDecisionJson;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
