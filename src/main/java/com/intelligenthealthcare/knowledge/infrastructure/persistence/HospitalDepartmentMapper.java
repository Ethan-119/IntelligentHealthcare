package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HospitalDepartmentMapper extends BaseMapper<HospitalDepartment> {

    IPage<HospitalDepartment> pageQuery(Page<HospitalDepartment> page, @Param("keyword") String keyword);
}
