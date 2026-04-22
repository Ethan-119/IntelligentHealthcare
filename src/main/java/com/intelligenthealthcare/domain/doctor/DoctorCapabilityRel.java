package com.intelligenthealthcare.entity.doctor;

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
@Table(name = "doctor_capability_rel")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorCapabilityRel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;
    @Column(name = "capability_code", length = 64)
    private String capabilityCode;
    @Column(name = "weight", precision = 8, scale = 2)
    private BigDecimal weight;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
