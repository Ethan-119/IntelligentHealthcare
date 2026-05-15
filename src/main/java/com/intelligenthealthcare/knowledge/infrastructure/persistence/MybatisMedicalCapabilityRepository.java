package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import com.intelligenthealthcare.knowledge.domain.repository.MedicalCapabilityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisMedicalCapabilityRepository implements MedicalCapabilityRepository {

    private final MedicalCapabilityKnowledgeMapper medicalCapabilityKnowledgeMapper;

    @Override
    public List<MedicalCapabilityKnowledge> findAllActive() {
        LambdaQueryWrapper<MedicalCapabilityKnowledge> query = new LambdaQueryWrapper<>();
        query.eq(MedicalCapabilityKnowledge::getActiveStatus, 1);
        query.orderByAsc(MedicalCapabilityKnowledge::getCapabilityName);
        return medicalCapabilityKnowledgeMapper.selectList(query);
    }
}
