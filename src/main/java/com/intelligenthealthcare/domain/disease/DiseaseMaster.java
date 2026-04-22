package com.intelligenthealthcare.entity.disease;

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
@Table(name = "disease_master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_code", nullable = false, unique = true, length = 64)
    private String diseaseCode;

    @Column(name = "disease_name", nullable = false, length = 255)
    private String diseaseName;

    @Column(name = "aliases_json", columnDefinition = "TEXT")
    private String aliasesJson;

    @Column(name = "symptom_keywords", columnDefinition = "TEXT")
    private String symptomKeywords;

    @Column(name = "gender_rule", length = 32)
    private String genderRule;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "age_group", length = 32)
    private String ageGroup;

    @Column(name = "urgency_level", length = 32)
    private String urgencyLevel;

    @Column(name = "review_status", length = 32)
    private String reviewStatus;

    @Column(name = "deleted")
    private Integer deleted;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
