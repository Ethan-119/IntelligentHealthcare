package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DoctorProfileMapper extends BaseMapper<DoctorProfile> {}
