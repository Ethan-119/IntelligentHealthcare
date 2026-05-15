package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalDepartmentRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisHospitalDepartmentRepository implements HospitalDepartmentRepository {

    private final HospitalDepartmentMapper hospitalDepartmentMapper;

    @Override
    public List<HospitalDepartment> findAllActive() {
        LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
        query.eq(HospitalDepartment::getDeleted, 0);
        query.eq(HospitalDepartment::getActiveStatus, 1);
        query.orderByAsc(HospitalDepartment::getDepartmentName);
        return hospitalDepartmentMapper.selectList(query);
    }

    @Override
    public Optional<HospitalDepartment> findById(Long id) {
        LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
        query.eq(HospitalDepartment::getId, id);
        query.eq(HospitalDepartment::getDeleted, 0);
        return Optional.ofNullable(hospitalDepartmentMapper.selectOne(query));
    }

    @Override
    public List<HospitalDepartment> findByHospitalId(String hospitalId) {
        LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
        query.eq(HospitalDepartment::getHospitalId, hospitalId);
        query.eq(HospitalDepartment::getDeleted, 0);
        query.eq(HospitalDepartment::getActiveStatus, 1);
        query.orderByAsc(HospitalDepartment::getDepartmentName);
        return hospitalDepartmentMapper.selectList(query);
    }
}
