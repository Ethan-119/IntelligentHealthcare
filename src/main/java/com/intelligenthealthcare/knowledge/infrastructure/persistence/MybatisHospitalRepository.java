package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisHospitalRepository implements HospitalRepository {

    private final HospitalMapper hospitalMapper;

    @Override
    public List<Hospital> findAllActive() {
        LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
        query.eq(Hospital::getDeleted, 0);
        query.eq(Hospital::getActiveStatus, 1);
        query.orderByAsc(Hospital::getHospitalName);
        return hospitalMapper.selectList(query);
    }

    @Override
    public Optional<Hospital> findByHospitalId(String hospitalId) {
        LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
        query.eq(Hospital::getHospitalId, hospitalId);
        query.eq(Hospital::getDeleted, 0);
        return Optional.ofNullable(hospitalMapper.selectOne(query));
    }

    @Override
    public Optional<Hospital> findByHospitalIdAll(String hospitalId) {
        // 不限状态，管理后台切换用
        LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
        query.eq(Hospital::getHospitalId, hospitalId);
        return Optional.ofNullable(hospitalMapper.selectOne(query));
    }

    @Override
    public List<Hospital> findAll() {
        LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
        query.orderByAsc(Hospital::getHospitalName);
        return hospitalMapper.selectList(query);
    }

    @Override
    public void update(Hospital entity) {
        hospitalMapper.updateById(entity);
    }
}
