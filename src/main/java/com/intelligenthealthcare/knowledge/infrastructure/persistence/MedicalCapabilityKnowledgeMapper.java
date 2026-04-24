package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MedicalCapabilityKnowledgeMapper extends BaseMapper<MedicalCapabilityKnowledge> {}
