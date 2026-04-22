package com.intelligenthealthcare.entity.hospital;

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
@Table(name = "hospital")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hospital_id", nullable = false, unique = true, length = 64)
    private String hospitalId;
    @Column(name = "hospital_name", nullable = false, length = 255)
    private String hospitalName;
    @Column(name = "city", length = 64)
    private String city;
    @Column(name = "district_name", length = 64)
    private String districtName;
    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;
    @Column(name = "hospital_level", length = 32)
    private String hospitalLevel;
    @Column(name = "is_emergency")
    private Integer isEmergency;
    @Column(name = "authority_score", precision = 10, scale = 2)
    private BigDecimal authorityScore;
    @Column(name = "active_status")
    private Integer activeStatus;
    @Column(name = "deleted")
    private Integer deleted;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
