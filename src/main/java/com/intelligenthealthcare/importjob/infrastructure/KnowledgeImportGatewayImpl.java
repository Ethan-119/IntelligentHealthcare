package com.intelligenthealthcare.importjob.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.importjob.domain.port.KnowledgeImportGateway;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseAliasMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseMasterMapper;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeImportGatewayImpl implements KnowledgeImportGateway {

    private final DiseaseMasterMapper diseaseMasterMapper;
    private final DiseaseAliasMapper diseaseAliasMapper;

    public KnowledgeImportGatewayImpl(DiseaseMasterMapper diseaseMasterMapper, DiseaseAliasMapper diseaseAliasMapper) {
        this.diseaseMasterMapper = diseaseMasterMapper;
        this.diseaseAliasMapper = diseaseAliasMapper;
    }

    @Override
    public DiseaseMaster findActiveDiseaseByCode(String diseaseCode) {
        if (diseaseCode == null) {
            return null;
        }
        LambdaQueryWrapper<DiseaseMaster> w = new LambdaQueryWrapper<>();
        w.eq(DiseaseMaster::getDiseaseCode, diseaseCode);
        w.eq(DiseaseMaster::getDeleted, 0);
        return diseaseMasterMapper.selectOne(w);
    }

    @Override
    public void saveDiseaseMaster(DiseaseMaster entity) {
        diseaseMasterMapper.insert(entity);
    }

    @Override
    public boolean existsDiseaseAlias(String diseaseCode, String aliasName) {
        LambdaQueryWrapper<DiseaseAlias> aw = new LambdaQueryWrapper<>();
        aw.eq(DiseaseAlias::getDiseaseCode, diseaseCode);
        aw.eq(DiseaseAlias::getAliasName, aliasName);
        Long cnt = diseaseAliasMapper.selectCount(aw);
        return cnt != null && cnt > 0;
    }

    @Override
    public void saveDiseaseAlias(DiseaseAlias entity) {
        diseaseAliasMapper.insert(entity);
    }
}
