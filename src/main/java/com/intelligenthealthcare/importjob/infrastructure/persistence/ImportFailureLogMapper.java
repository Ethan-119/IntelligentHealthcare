package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportFailureLogMapper extends BaseMapper<ImportFailureLog> {}
