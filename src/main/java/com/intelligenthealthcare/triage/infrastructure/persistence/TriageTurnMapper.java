package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.triage.domain.model.TriageTurn;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TriageTurnMapper extends BaseMapper<TriageTurn> {}
