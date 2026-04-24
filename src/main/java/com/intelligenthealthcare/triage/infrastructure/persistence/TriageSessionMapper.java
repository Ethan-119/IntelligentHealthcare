package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.triage.domain.model.TriageSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TriageSessionMapper extends BaseMapper<TriageSession> {}
