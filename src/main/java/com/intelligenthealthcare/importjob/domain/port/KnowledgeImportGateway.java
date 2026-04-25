package com.intelligenthealthcare.importjob.domain.port;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;

/**
 * 知识库导入所需读写的防腐层接口，由基础设施用 MyBatis 等实现；领域与具体存储解耦。
 */
public interface KnowledgeImportGateway {

    /** 未删除且存在的疾病主数据；不存在则返回 {@code null}。 */
    DiseaseMaster findActiveDiseaseByCode(String diseaseCode);

    void saveDiseaseMaster(DiseaseMaster entity);

    boolean existsDiseaseAlias(String diseaseCode, String aliasName);

    void saveDiseaseAlias(DiseaseAlias entity);
}
