package com.intelligenthealthcare.entity.audit;

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
@Table(name = "ai_recall_audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecallAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms;
    @Column(name = "gender", length = 32)
    private String gender;
    @Column(name = "age")
    private Integer age;
    @Column(name = "age_group", length = 32)
    private String ageGroup;
    @Column(name = "eligible_disease_count")
    private Integer eligibleDiseaseCount;
    @Column(name = "rule_candidate_codes_json", columnDefinition = "TEXT")
    private String ruleCandidateCodesJson;
    @Column(name = "suggested_codes_json", columnDefinition = "TEXT")
    private String suggestedCodesJson;
    @Column(name = "status", length = 64)
    private String status;
    @Column(name = "message", length = 500)
    private String message;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
