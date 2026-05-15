package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DoctorCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.repository.DoctorCapabilityRelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDoctorCapabilityRelRepository implements DoctorCapabilityRelRepository {

    private final DoctorCapabilityRelMapper doctorCapabilityRelMapper;

    @Override
    public List<DoctorCapabilityRel> findByDoctorId(Long doctorId) {
        LambdaQueryWrapper<DoctorCapabilityRel> query = new LambdaQueryWrapper<>();
        query.eq(DoctorCapabilityRel::getDoctorId, doctorId);
        query.orderByDesc(DoctorCapabilityRel::getWeight);
        return doctorCapabilityRelMapper.selectList(query);
    }
}
