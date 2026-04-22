package com.intelligenthealthcare.catalog.domain.model;

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
@Table(name = "medical_capability_catalog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalCapabilityCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "capability_code", nullable = false, unique = true, length = 64)
    private String capabilityCode;

    @Column(name = "capability_name", nullable = false, length = 255)
    private String capabilityName;

    @Column(name = "capability_type", nullable = false, length = 64)
    private String capabilityType;

    @Column(name = "parent_code", length = 64)
    private String parentCode;

    @Column(name = "standard_dept_code", length = 64)
    private String standardDeptCode;

    @Column(name = "aliases_json", columnDefinition = "TEXT")
    private String aliasesJson;

    @Column(name = "gender_rule", length = 32)
    private String genderRule;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "crowd_tags_json", columnDefinition = "TEXT")
    private String crowdTagsJson;

    @Column(name = "pathway_tags_json", columnDefinition = "TEXT")
    private String pathwayTagsJson;

    @Column(name = "active_status")
    private Integer activeStatus;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
