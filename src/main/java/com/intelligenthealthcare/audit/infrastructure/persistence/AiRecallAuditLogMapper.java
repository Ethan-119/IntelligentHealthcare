package com.intelligenthealthcare.audit.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.audit.domain.model.AiRecallAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiRecallAuditLogMapper extends BaseMapper<AiRecallAuditLog> {}
