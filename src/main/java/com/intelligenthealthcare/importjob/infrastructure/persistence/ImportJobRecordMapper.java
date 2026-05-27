package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code import_job_record} 表的 MyBatis-Plus Mapper。
 */
@Mapper
public interface ImportJobRecordMapper extends BaseMapper<ImportJobRecord> {}
