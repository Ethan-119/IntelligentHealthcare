package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DepartmentCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.repository.DepartmentCapabilityRelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDepartmentCapabilityRelRepository implements DepartmentCapabilityRelRepository {

    private final DepartmentCapabilityRelMapper departmentCapabilityRelMapper;

    @Override
    public List<DepartmentCapabilityRel> findByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<DepartmentCapabilityRel> query = new LambdaQueryWrapper<>();
        query.eq(DepartmentCapabilityRel::getDepartmentId, departmentId);
        query.orderByDesc(DepartmentCapabilityRel::getWeight);
        return departmentCapabilityRelMapper.selectList(query);
    }

    @Override
    public List<DepartmentCapabilityRel> findByCapabilityCode(String capabilityCode) {
        LambdaQueryWrapper<DepartmentCapabilityRel> query = new LambdaQueryWrapper<>();
        query.eq(DepartmentCapabilityRel::getCapabilityCode, capabilityCode);
        query.orderByDesc(DepartmentCapabilityRel::getWeight);
        return departmentCapabilityRelMapper.selectList(query);
    }
}
