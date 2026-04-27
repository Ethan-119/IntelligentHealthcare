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

@TableName("import_job_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportJobRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("dataset_type")
    private String datasetType;

    @TableField("file_name")
    private String fileName;

    private String status;

    @TableField("success_count")
    private Integer successCount;

    @TableField("failure_count")
    private Integer failureCount;

    @TableField("review_count")
    private Integer reviewCount;

    @TableField("auto_mapped_count")
    private Integer autoMappedCount;

    private String message;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
