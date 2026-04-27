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

@TableName("import_failure_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportFailureLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_id")
    private Long jobId;

    @TableField("row_number")
    private Integer rowNumber;

    @TableField("raw_content")
    private String rawContent;

    @TableField("error_message")
    private String errorMessage;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 领域侧工厂：行级错误写入失日志。 */
    public static ImportFailureLog fromLineError(long jobId, int fileLineNumber, String rawContent, String errorMessage) {
        return ImportFailureLog.builder()
                .jobId(jobId)
                .rowNumber(fileLineNumber)
                .rawContent(rawContent)
                .errorMessage(ImportTextLimits.truncateMessage(errorMessage, ImportTextLimits.ERROR_MESSAGE))
                .build();
    }
}
