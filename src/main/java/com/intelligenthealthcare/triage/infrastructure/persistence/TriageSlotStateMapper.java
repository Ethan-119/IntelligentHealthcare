package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.triage.domain.model.TriageSlotState;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TriageSlotStateMapper extends BaseMapper<TriageSlotState> {}
