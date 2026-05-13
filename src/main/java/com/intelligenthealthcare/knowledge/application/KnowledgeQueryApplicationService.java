package com.intelligenthealthcare.knowledge.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.knowledge.domain.model.DepartmentCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.domain.model.DoctorCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DepartmentCapabilityRelMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseAliasMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseCapabilityRelMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseMasterMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DoctorCapabilityRelMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DoctorProfileMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.HospitalDepartmentMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.HospitalMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.MedicalCapabilityKnowledgeMapper;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识库查询服务 —— 为 AI 导诊 / 前端展示提供全量知识库数据的缓存化读取。
 * <p>
 * 所有查询均为只读事务；写操作由管理员在 PostgreSQL 执行（如导入任务），并在写库成功后删除缓存。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KnowledgeQueryApplicationService {

    private static final String CACHE_DISEASE = "knowledge:disease";
    private static final String CACHE_HOSPITAL = "knowledge:hospital";
    private static final String CACHE_DEPARTMENT = "knowledge:department";
    private static final String CACHE_DOCTOR = "knowledge:doctor";
    private static final String CACHE_CAPABILITY = "knowledge:capability";
    private static final long TTL_10_MINUTES = 10L;
    private static final long TTL_30_MINUTES = 30L;
    private static final long TTL_60_MINUTES = 60L;

    private final DiseaseMasterMapper diseaseMasterMapper;
    private final DiseaseAliasMapper diseaseAliasMapper;
    private final HospitalMapper hospitalMapper;
    private final HospitalDepartmentMapper hospitalDepartmentMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final DepartmentCapabilityRelMapper departmentCapabilityRelMapper;
    private final DiseaseCapabilityRelMapper diseaseCapabilityRelMapper;
    private final DoctorCapabilityRelMapper doctorCapabilityRelMapper;
    private final MedicalCapabilityKnowledgeMapper medicalCapabilityKnowledgeMapper;
    private final RedissonClient redissonClient;

    // ==================== 1. 疾病基础热点数据 ====================

    public List<DiseaseMaster> findActiveDiseases() {
        return getListCache(
                CACHE_DISEASE,
                "allActive",
                TTL_60_MINUTES,
                () -> {
                    LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
                    query.eq(DiseaseMaster::getDeleted, 0);
                    query.orderByAsc(DiseaseMaster::getDiseaseName);
                    return diseaseMasterMapper.selectList(query);
                });
    }

    public DiseaseMaster findDiseaseByCode(String code) {
        return getObjectCache(
                CACHE_DISEASE,
                "code:" + code,
                TTL_60_MINUTES,
                () -> {
                    LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
                    query.eq(DiseaseMaster::getDiseaseCode, code);
                    query.eq(DiseaseMaster::getDeleted, 0);
                    return diseaseMasterMapper.selectOne(query);
                });
    }

    public List<DiseaseAlias> findAliasesByDiseaseCode(String diseaseCode) {
        return getListCache(
                CACHE_DISEASE,
                "alias:" + diseaseCode,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DiseaseAlias> query = new LambdaQueryWrapper<>();
                    query.eq(DiseaseAlias::getDiseaseCode, diseaseCode);
                    return diseaseAliasMapper.selectList(query);
                });
    }

    /** 关键字搜索疾病（模糊匹配疾病名 + 症状关键词），结果缓存 30 分钟 */
    public List<DiseaseMaster> searchDiseases(String keyword) {
        return getListCache(
                CACHE_DISEASE,
                "search:" + keyword,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DiseaseMaster> query = new LambdaQueryWrapper<>();
                    query.eq(DiseaseMaster::getDeleted, 0);
                    query.and(wrapper -> wrapper.like(DiseaseMaster::getDiseaseName, keyword)
                            .or()
                            .like(DiseaseMaster::getSymptomKeywords, keyword));
                    query.orderByAsc(DiseaseMaster::getDiseaseName);
                    return diseaseMasterMapper.selectList(query);
                });
    }

    // ==================== 2. 科室与医生基础数据 ====================

    public List<Hospital> findActiveHospitals() {
        return getListCache(
                CACHE_HOSPITAL,
                "allActive",
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
                    query.eq(Hospital::getDeleted, 0);
                    query.eq(Hospital::getActiveStatus, 1);
                    query.orderByAsc(Hospital::getHospitalName);
                    return hospitalMapper.selectList(query);
                });
    }

    public Hospital findHospitalById(String hospitalId) {
        return getObjectCache(
                CACHE_HOSPITAL,
                "id:" + hospitalId,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<Hospital> query = new LambdaQueryWrapper<>();
                    query.eq(Hospital::getHospitalId, hospitalId);
                    query.eq(Hospital::getDeleted, 0);
                    return hospitalMapper.selectOne(query);
                });
    }

    public List<HospitalDepartment> findActiveDepartments() {
        return getListCache(
                CACHE_DEPARTMENT,
                "allActive",
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
                    query.eq(HospitalDepartment::getDeleted, 0);
                    query.eq(HospitalDepartment::getActiveStatus, 1);
                    query.orderByAsc(HospitalDepartment::getDepartmentName);
                    return hospitalDepartmentMapper.selectList(query);
                });
    }

    public HospitalDepartment findDepartmentById(Long id) {
        return getObjectCache(
                CACHE_DEPARTMENT,
                "id:" + id,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
                    query.eq(HospitalDepartment::getId, id);
                    query.eq(HospitalDepartment::getDeleted, 0);
                    return hospitalDepartmentMapper.selectOne(query);
                });
    }

    public List<HospitalDepartment> findDepartmentsByHospital(String hospitalId) {
        return getListCache(
                CACHE_DEPARTMENT,
                "hospital:" + hospitalId,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<HospitalDepartment> query = new LambdaQueryWrapper<>();
                    query.eq(HospitalDepartment::getHospitalId, hospitalId);
                    query.eq(HospitalDepartment::getDeleted, 0);
                    query.eq(HospitalDepartment::getActiveStatus, 1);
                    query.orderByAsc(HospitalDepartment::getDepartmentName);
                    return hospitalDepartmentMapper.selectList(query);
                });
    }

    /**
     * 热门医生：权威分 ≥ 阈值，按权威分降序
     */
    public List<DoctorProfile> findHotDoctors() {
        return getListCache(
                CACHE_DOCTOR,
                "hot",
                TTL_10_MINUTES,
                () -> {
                    LambdaQueryWrapper<DoctorProfile> query = new LambdaQueryWrapper<>();
                    query.eq(DoctorProfile::getActiveStatus, 1);
                    query.ge(DoctorProfile::getAuthorityScore, new BigDecimal("3.0"));
                    query.orderByDesc(DoctorProfile::getAuthorityScore);
                    query.last("LIMIT 50");
                    return doctorProfileMapper.selectList(query);
                });
    }

    public List<DoctorProfile> findDoctorsByDepartment(Long departmentId) {
        return getListCache(
                CACHE_DOCTOR,
                "dept:" + departmentId,
                TTL_10_MINUTES,
                () -> {
                    LambdaQueryWrapper<DoctorProfile> query = new LambdaQueryWrapper<>();
                    query.eq(DoctorProfile::getDepartmentId, departmentId);
                    query.eq(DoctorProfile::getActiveStatus, 1);
                    query.orderByDesc(DoctorProfile::getAuthorityScore);
                    return doctorProfileMapper.selectList(query);
                });
    }

    public DoctorProfile findDoctorById(Long id) {
        return getObjectCache(
                CACHE_DOCTOR,
                "id:" + id,
                TTL_10_MINUTES,
                () -> doctorProfileMapper.selectById(id));
    }

    // ==================== 3. 症状库 & 关联关系 ====================

    /** 全量医疗能力 / 检查项字典（症状 → 检查项目的基座） */
    public List<MedicalCapabilityKnowledge> findAllCapabilityKnowledge() {
        return getListCache(
                CACHE_CAPABILITY,
                "knowledge:all",
                TTL_60_MINUTES,
                () -> {
                    LambdaQueryWrapper<MedicalCapabilityKnowledge> query = new LambdaQueryWrapper<>();
                    query.eq(MedicalCapabilityKnowledge::getActiveStatus, 1);
                    query.orderByAsc(MedicalCapabilityKnowledge::getCapabilityName);
                    return medicalCapabilityKnowledgeMapper.selectList(query);
                });
    }

    /** 疾病 → 医疗能力 / 检查项 映射 */
    public List<DiseaseCapabilityRel> findCapabilitiesByDisease(String diseaseCode) {
        return getListCache(
                CACHE_CAPABILITY,
                "disease:" + diseaseCode,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DiseaseCapabilityRel> query = new LambdaQueryWrapper<>();
                    query.eq(DiseaseCapabilityRel::getDiseaseCode, diseaseCode);
                    query.orderByDesc(DiseaseCapabilityRel::getPriorityScore);
                    return diseaseCapabilityRelMapper.selectList(query);
                });
    }

    /** 科室 → 医疗能力 / 检查项 映射 */
    public List<DepartmentCapabilityRel> findCapabilitiesByDepartment(Long departmentId) {
        return getListCache(
                CACHE_CAPABILITY,
                "dept:" + departmentId,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DepartmentCapabilityRel> query = new LambdaQueryWrapper<>();
                    query.eq(DepartmentCapabilityRel::getDepartmentId, departmentId);
                    query.orderByDesc(DepartmentCapabilityRel::getWeight);
                    return departmentCapabilityRelMapper.selectList(query);
                });
    }

    /** 医生 → 医疗能力 / 检查项 映射 */
    public List<DoctorCapabilityRel> findCapabilitiesByDoctor(Long doctorId) {
        return getListCache(
                CACHE_CAPABILITY,
                "doctor:" + doctorId,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DoctorCapabilityRel> query = new LambdaQueryWrapper<>();
                    query.eq(DoctorCapabilityRel::getDoctorId, doctorId);
                    query.orderByDesc(DoctorCapabilityRel::getWeight);
                    return doctorCapabilityRelMapper.selectList(query);
                });
    }

    /** 根据医疗能力反查支持科室（症状 → 科室 的关联路由） */
    public List<DepartmentCapabilityRel> findDepartmentsByCapability(String capabilityCode) {
        return getListCache(
                CACHE_CAPABILITY,
                "deptByCap:" + capabilityCode,
                TTL_30_MINUTES,
                () -> {
                    LambdaQueryWrapper<DepartmentCapabilityRel> query = new LambdaQueryWrapper<>();
                    query.eq(DepartmentCapabilityRel::getCapabilityCode, capabilityCode);
                    query.orderByDesc(DepartmentCapabilityRel::getWeight);
                    return departmentCapabilityRelMapper.selectList(query);
                });
    }

    /**
     * 清理 knowledge 相关缓存，供导入任务和定时任务调用。
     */
    public void evictKnowledgeCaches() {
        redissonClient.getMapCache(CACHE_DISEASE).clear();
        redissonClient.getMapCache(CACHE_HOSPITAL).clear();
        redissonClient.getMapCache(CACHE_DEPARTMENT).clear();
        redissonClient.getMapCache(CACHE_DOCTOR).clear();
        redissonClient.getMapCache(CACHE_CAPABILITY).clear();
    }

    /**
     * 预热热点缓存：先清缓存，再加载常用数据。
     */
    public void refreshHotCaches() {
        evictKnowledgeCaches();

        List<DiseaseMaster> diseases = findActiveDiseases();
        List<HospitalDepartment> departments = findActiveDepartments();
        List<DoctorProfile> doctors = findHotDoctors();

        findActiveHospitals();
        findAllCapabilityKnowledge();

        List<String> diseaseCodes = diseases.stream().map(DiseaseMaster::getDiseaseCode).toList();
        for (int i = 0; i < diseaseCodes.size(); i++) {
            String diseaseCode = diseaseCodes.get(i);
            findAliasesByDiseaseCode(diseaseCode);
            findCapabilitiesByDisease(diseaseCode);
        }

        List<Long> departmentIds = departments.stream().map(HospitalDepartment::getId).toList();
        for (int i = 0; i < departmentIds.size(); i++) {
            Long departmentId = departmentIds.get(i);
            findCapabilitiesByDepartment(departmentId);
            findDoctorsByDepartment(departmentId);
        }

        List<Long> doctorIds = doctors.stream().map(DoctorProfile::getId).toList();
        for (int i = 0; i < doctorIds.size(); i++) {
            Long doctorId = doctorIds.get(i);
            findCapabilitiesByDoctor(doctorId);
        }
    }

    private <T> T getObjectCache(String mapName, String key, long ttlMinutes, Supplier<T> loader) {
        RMapCache<String, T> cache = redissonClient.getMapCache(mapName);
        T cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        T loaded = loader.get();
        if (loaded != null) {
            cache.put(key, loaded, ttlMinutes, TimeUnit.MINUTES);
        }
        return loaded;
    }

    private <T> List<T> getListCache(String mapName, String key, long ttlMinutes, Supplier<List<T>> loader) {
        List<T> cached = getObjectCache(mapName, key, ttlMinutes, loader);
        if (cached == null) {
            return Collections.emptyList();
        }
        return cached;
    }
}
