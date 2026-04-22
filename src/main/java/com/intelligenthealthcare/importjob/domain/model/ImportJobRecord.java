package com.intelligenthealthcare.importjob.domain.model;

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
@Table(name = "import_job_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "dataset_type", nullable = false, length = 64)
    private String datasetType;
    @Column(name = "file_name", length = 255)
    private String fileName;
    @Column(name = "status", nullable = false, length = 32)
    private String status;
    @Column(name = "success_count")
    private Integer successCount;
    @Column(name = "failure_count")
    private Integer failureCount;
    @Column(name = "review_count")
    private Integer reviewCount;
    @Column(name = "auto_mapped_count")
    private Integer autoMappedCount;
    @Column(name = "message", length = 500)
    private String message;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
