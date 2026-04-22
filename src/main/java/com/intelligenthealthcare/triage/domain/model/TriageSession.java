package com.intelligenthealthcare.triage.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "triage_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;
    @Column(name = "user_id", length = 64)
    private String userId;
    @Column(name = "dialog_id", length = 64)
    private String dialogId;
    @Column(name = "current_stage", length = 32)
    private String currentStage;
    @Column(name = "ask_round")
    private Integer askRound;
    @Column(name = "invalid_answer_count")
    private Integer invalidAnswerCount;
    @Column(name = "city", length = 64)
    private String city;
    @Column(name = "area", length = 64)
    private String area;
    @Column(name = "nearby")
    private Integer nearby;
    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;
    @Column(name = "patient_age")
    private Integer patientAge;
    @Column(name = "patient_gender", length = 32)
    private String patientGender;
    @Column(name = "severity_level", length = 32)
    private String severityLevel;
    @Column(name = "route_type", length = 32)
    private String routeType;
    @Column(name = "status", length = 32)
    private String status;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
