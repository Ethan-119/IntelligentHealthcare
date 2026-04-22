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
@Table(name = "hospital_department")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDepartment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "hospital_id", nullable = false, length = 64)
    private String hospitalId;
    @Column(name = "department_name", nullable = false, length = 255)
    private String departmentName;
    @Column(name = "parent_department_name", length = 255)
    private String parentDepartmentName;
    @Column(name = "department_intro", columnDefinition = "TEXT")
    private String departmentIntro;
    @Column(name = "service_scope", columnDefinition = "TEXT")
    private String serviceScope;
    @Column(name = "active_status")
    private Integer activeStatus;
    @Column(name = "deleted")
    private Integer deleted;
    @Column(name = "gender_rule", length = 32)
    private String genderRule;
    @Column(name = "age_min")
    private Integer ageMin;
    @Column(name = "age_max")
    private Integer ageMax;
    @Column(name = "crowd_tags_json", columnDefinition = "TEXT")
    private String crowdTagsJson;
    @Column(name = "standard_dept_code", length = 64)
    private String standardDeptCode;
    @Column(name = "subspecialty_code", length = 64)
    private String subspecialtyCode;
    @Column(name = "district_name", length = 64)
    private String districtName;
    @Column(name = "latitude", precision = 10, scale = 6)
    private BigDecimal latitude;
    @Column(name = "longitude", precision = 10, scale = 6)
    private BigDecimal longitude;
    @Column(name = "is_emergency")
    private Integer isEmergency;
    @Column(name = "national_key_score", precision = 10, scale = 2)
    private BigDecimal nationalKeyScore;
    @Column(name = "provincial_key_score", precision = 10, scale = 2)
    private BigDecimal provincialKeyScore;
    @Column(name = "city_key_score", precision = 10, scale = 2)
    private BigDecimal cityKeyScore;
    @Column(name = "authority_score", precision = 10, scale = 2)
    private BigDecimal authorityScore;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
