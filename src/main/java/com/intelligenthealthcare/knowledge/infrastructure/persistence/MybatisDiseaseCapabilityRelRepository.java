package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseCapabilityRelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDiseaseCapabilityRelRepository implements DiseaseCapabilityRelRepository {

    private final DiseaseCapabilityRelMapper diseaseCapabilityRelMapper;

    @Override
    public List<DiseaseCapabilityRel> findByDiseaseCode(String diseaseCode) {
        LambdaQueryWrapper<DiseaseCapabilityRel> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseCapabilityRel::getDiseaseCode, diseaseCode);
        query.orderByDesc(DiseaseCapabilityRel::getPriorityScore);
        return diseaseCapabilityRelMapper.selectList(query);
    }
}
