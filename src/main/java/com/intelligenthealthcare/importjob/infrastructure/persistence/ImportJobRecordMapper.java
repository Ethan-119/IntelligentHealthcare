package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportJobRecordMapper extends BaseMapper<ImportJobRecord> {}
