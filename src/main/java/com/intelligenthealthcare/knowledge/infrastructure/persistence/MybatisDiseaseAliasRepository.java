package com.intelligenthealthcare.knowledge.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseAliasRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisDiseaseAliasRepository implements DiseaseAliasRepository {

    private final DiseaseAliasMapper diseaseAliasMapper;

    @Override
    public List<DiseaseAlias> findByDiseaseCode(String diseaseCode) {
        LambdaQueryWrapper<DiseaseAlias> query = new LambdaQueryWrapper<>();
        query.eq(DiseaseAlias::getDiseaseCode, diseaseCode);
        return diseaseAliasMapper.selectList(query);
    }
}
