package com.intelligenthealthcare.importjob.domain.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("import_review_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportReviewItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("dataset_type")
    private String datasetType;

    @TableField("item_key")
    private String itemKey;

    @TableField("issue_type")
    private String issueType;

    @TableField("raw_content")
    private String rawContent;

    private String suggestion;
    private Integer resolved;

    @TableField("resolution_note")
    private String resolutionNote;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public static ImportReviewItem fromDraft(long jobId, ImportReviewDraft draft) {
        if (draft == null) {
            return null;
        }
        return ImportReviewItem.builder()
                .jobId(jobId)
                .datasetType(draft.getDatasetType())
                .itemKey(draft.getItemKey())
                .issueType(draft.getIssueType())
                .rawContent(draft.getRawContent())
                .suggestion(draft.getSuggestion())
                .resolved(0)
                .build();
    }

    /** 管理员将待办标为已处理。 */
    public void markResolvedWithNote(String resolutionNote) {
        this.resolved = 1;
        this.resolutionNote = resolutionNote;
    }
}
