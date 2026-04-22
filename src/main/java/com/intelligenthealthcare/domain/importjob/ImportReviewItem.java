package com.intelligenthealthcare.entity.importjob;

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
@Table(name = "import_review_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportReviewItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "job_id", nullable = false)
    private Long jobId;
    @Column(name = "dataset_type", length = 64)
    private String datasetType;
    @Column(name = "item_key", length = 128)
    private String itemKey;
    @Column(name = "issue_type", length = 128)
    private String issueType;
    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;
    @Column(name = "suggestion", length = 500)
    private String suggestion;
    @Column(name = "resolved")
    private Integer resolved;
    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
