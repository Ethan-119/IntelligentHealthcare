package com.intelligenthealthcare.importjob.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ImportReviewItemMapper extends BaseMapper<ImportReviewItem> {}
