package com.intelligenthealthcare.knowledge.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.util.List;
import java.util.Optional;

public interface DiseaseMasterRepository {
    List<DiseaseMaster> findAllActive();

    // 查询全部（含已删除），供管理后台使用
    List<DiseaseMaster> findAll();

    Optional<DiseaseMaster> findByCode(String diseaseCode);

    // 不限状态查询（管理后台切换状态用）
    Optional<DiseaseMaster> findByCodeAll(String diseaseCode);

    List<DiseaseMaster> searchByKeyword(String keyword);

    // 更新单条记录，供管理后台切换 deleted 状态
    void update(DiseaseMaster entity);
}
