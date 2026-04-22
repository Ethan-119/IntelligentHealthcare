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

@Entity
@Table(name = "import_failure_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportFailureLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false)
    private Long jobId;
    @Column(name = "row_number")
    private Integer rowNumber;
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
