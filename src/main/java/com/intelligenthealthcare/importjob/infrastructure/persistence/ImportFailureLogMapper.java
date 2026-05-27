package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@code import_failure_log} 表的 MyBatis-Plus Mapper。
 */
@Mapper
public interface ImportFailureLogMapper extends BaseMapper<ImportFailureLog> {}
