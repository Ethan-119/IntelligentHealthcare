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

@Entity
@Table(name = "department_capability_rel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentCapabilityRel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "department_id", nullable = false)
    private Long departmentId;
    @Column(name = "capability_code", nullable = false, length = 64)
    private String capabilityCode;
    @Column(name = "support_level", length = 32)
    private String supportLevel;
    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;
    @Column(name = "source", length = 64)
    private String source;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
