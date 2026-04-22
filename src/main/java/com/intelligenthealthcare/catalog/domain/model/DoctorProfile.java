package com.intelligenthealthcare.catalog.domain.model;

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
@Table(name = "doctor_profile")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hospital_id", nullable = false, length = 64)
    private String hospitalId;
    @Column(name = "department_id", nullable = false)
    private Long departmentId;
    @Column(name = "doctor_name", nullable = false, length = 128)
    private String doctorName;
    @Column(name = "title", length = 64)
    private String title;
    @Column(name = "specialty_text", columnDefinition = "TEXT")
    private String specialtyText;
    @Column(name = "gender_rule", length = 32)
    private String genderRule;
    @Column(name = "age_min")
    private Integer ageMin;
    @Column(name = "age_max")
    private Integer ageMax;
    @Column(name = "crowd_tags_json", columnDefinition = "TEXT")
    private String crowdTagsJson;
    @Column(name = "authority_score", precision = 10, scale = 2)
    private BigDecimal authorityScore;
    @Column(name = "academic_title_score", precision = 10, scale = 2)
    private BigDecimal academicTitleScore;
    @Column(name = "is_expert")
    private Integer isExpert;
    @Column(name = "campus_name", length = 128)
    private String campusName;
    @Column(name = "active_status")
    private Integer activeStatus;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
