package com.intelligenthealthcare.entity.capability;

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

@Entity
@Table(name = "disease_capability_rel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseCapabilityRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_code", nullable = false, length = 64)
    private String diseaseCode;

    @Column(name = "capability_code", nullable = false, length = 64)
    private String capabilityCode;

    @Column(name = "rel_type", length = 64)
    private String relType;

    @Column(name = "priority_score", precision = 8, scale = 2)
    private BigDecimal priorityScore;

    @Column(name = "crowd_constraint", length = 255)
    private String crowdConstraint;

    @Column(name = "note", length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
