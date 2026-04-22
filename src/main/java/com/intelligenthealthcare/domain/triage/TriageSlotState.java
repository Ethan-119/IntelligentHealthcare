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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "triage_slot_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageSlotState {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;
    @Column(name = "symptoms_json", columnDefinition = "TEXT")
    private String symptomsJson;
    @Column(name = "disease_name", length = 255)
    private String diseaseName;
    @Column(name = "target_hospital", length = 255)
    private String targetHospital;
    @Column(name = "target_department", length = 255)
    private String targetDepartment;
    @Column(name = "target_doctor", length = 255)
    private String targetDoctor;
    @Column(name = "missing_slots_json", columnDefinition = "TEXT")
    private String missingSlotsJson;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
