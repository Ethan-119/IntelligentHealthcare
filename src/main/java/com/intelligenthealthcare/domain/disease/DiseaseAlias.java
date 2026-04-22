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

@Entity
@Table(name = "disease_alias")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiseaseAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "disease_code", nullable = false, length = 64)
    private String diseaseCode;

    @Column(name = "alias_name", nullable = false, length = 255)
    private String aliasName;

    @Column(name = "alias_type", length = 64)
    private String aliasType;

    @Column(name = "source", length = 64)
    private String source;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
