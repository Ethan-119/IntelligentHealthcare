package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseMasterRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDiseaseMasterRepository implements DiseaseMasterRepository {

    private final DiseaseMasterMapper diseaseMasterMapper;

    @Override
    public List<DiseaseMaster> findAllActive() {
        LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseMaster::getDeleted, 0);
        query.orderByAsc(DiseaseMaster::getDiseaseName);
        return diseaseMasterMapper.selectList(query);
    }

    @Override
    public Optional<DiseaseMaster> findByCode(String diseaseCode) {
        LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseMaster::getDiseaseCode, diseaseCode);
        query.eq(DiseaseMaster::getDeleted, 0);
        return Optional.ofNullable(diseaseMasterMapper.selectOne(query));
    }

    @Override
    public Optional<DiseaseMaster> findByCodeAll(String diseaseCode) {
        // 不限 deleted 状态，管理后台切换用
        LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseMaster::getDiseaseCode, diseaseCode);
        return Optional.ofNullable(diseaseMasterMapper.selectOne(query));
    }

    @Override
    public List<DiseaseMaster> searchByKeyword(String keyword) {
        LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseMaster::getDeleted, 0);
        query.and(w -> w.like(DiseaseMaster::getDiseaseName, keyword)
                .or().like(DiseaseMaster::getSymptomKeywords, keyword));
        query.orderByAsc(DiseaseMaster::getDiseaseName);
        return diseaseMasterMapper.selectList(query);
    }

    @Override
    public List<DiseaseMaster> findAll() {
        // 管理后台查看全部（含已删除），按名称排序
        LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
        query.orderByAsc(DiseaseMaster::getDiseaseName);
        return diseaseMasterMapper.selectList(query);
    }

    @Override
    public void update(DiseaseMaster entity) {
        diseaseMasterMapper.updateById(entity);
    }
}
