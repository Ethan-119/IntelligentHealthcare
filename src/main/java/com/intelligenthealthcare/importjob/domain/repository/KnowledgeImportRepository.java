package com.intelligenthealthcare.importjob.domain.repository;

import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;

/**
 * 与「知识库」限界上下文交互的接口：为 importjob 提供疾病主表/别名的读写在领域侧的唯一入口，由基础设施实现（防腐，避免应用层直接依赖 Mapper）。
 * 命名上归入 repository：表达「被导入实体的持久化与查询」。
 */
public interface KnowledgeImportRepository {

    DiseaseMaster findActiveDiseaseByCode(String diseaseCode);

    void saveDiseaseMaster(DiseaseMaster entity);

    boolean existsDiseaseAlias(String diseaseCode, String aliasName);

    void saveDiseaseAlias(DiseaseAlias entity);
}
