package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiseaseMasterMapper extends BaseMapper<DiseaseMaster> {}
