package com.intelligenthealthcare.knowledge.application;

import com.intelligenthealthcare.knowledge.domain.model.DepartmentCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.domain.model.DoctorCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import com.intelligenthealthcare.knowledge.domain.repository.DepartmentCapabilityRelRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseAliasRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseCapabilityRelRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseMasterRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DoctorCapabilityRelRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DoctorProfileRepository;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalDepartmentRepository;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalRepository;
import com.intelligenthealthcare.knowledge.domain.repository.MedicalCapabilityRepository;
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

    private final DiseaseMasterRepository diseaseMasterRepository;
    private final DiseaseAliasRepository diseaseAliasRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalDepartmentRepository hospitalDepartmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DepartmentCapabilityRelRepository departmentCapabilityRelRepository;
    private final DiseaseCapabilityRelRepository diseaseCapabilityRelRepository;
    private final DoctorCapabilityRelRepository doctorCapabilityRelRepository;
    private final MedicalCapabilityRepository medicalCapabilityRepository;
    private final RedissonClient redissonClient;

    // ==================== 1. 疾病基础热点数据 ====================

    public List<DiseaseMaster> findActiveDiseases() {
        return getListCache(CACHE_DISEASE, "allActive", TTL_60_MINUTES,
                diseaseMasterRepository::findAllActive);
    }

    public DiseaseMaster findDiseaseByCode(String code) {
        return getObjectCache(CACHE_DISEASE, "code:" + code, TTL_60_MINUTES,
                () -> diseaseMasterRepository.findByCode(code).orElse(null));
    }

    public List<DiseaseAlias> findAliasesByDiseaseCode(String diseaseCode) {
        return getListCache(CACHE_DISEASE, "alias:" + diseaseCode, TTL_30_MINUTES,
                () -> diseaseAliasRepository.findByDiseaseCode(diseaseCode));
    }

    public List<DiseaseMaster> searchDiseases(String keyword) {
        return getListCache(CACHE_DISEASE, "search:" + keyword, TTL_30_MINUTES,
                () -> diseaseMasterRepository.searchByKeyword(keyword));
    }

    // ==================== 2. 科室与医生基础数据 ====================

    public List<Hospital> findActiveHospitals() {
        return getListCache(CACHE_HOSPITAL, "allActive", TTL_30_MINUTES,
                hospitalRepository::findAllActive);
    }

    public Hospital findHospitalById(String hospitalId) {
        return getObjectCache(CACHE_HOSPITAL, "id:" + hospitalId, TTL_30_MINUTES,
                () -> hospitalRepository.findByHospitalId(hospitalId).orElse(null));
    }

    public List<HospitalDepartment> findActiveDepartments() {
        return getListCache(CACHE_DEPARTMENT, "allActive", TTL_30_MINUTES,
                hospitalDepartmentRepository::findAllActive);
    }

    public HospitalDepartment findDepartmentById(Long id) {
        return getObjectCache(CACHE_DEPARTMENT, "id:" + id, TTL_30_MINUTES,
                () -> hospitalDepartmentRepository.findById(id).orElse(null));
    }

    public List<HospitalDepartment> findDepartmentsByHospital(String hospitalId) {
        return getListCache(CACHE_DEPARTMENT, "hospital:" + hospitalId, TTL_30_MINUTES,
                () -> hospitalDepartmentRepository.findByHospitalId(hospitalId));
    }

    public List<DoctorProfile> findHotDoctors() {
        return getListCache(CACHE_DOCTOR, "hot", TTL_10_MINUTES,
                () -> doctorProfileRepository.findHotDoctors(50));
    }

    public List<DoctorProfile> findDoctorsByDepartment(Long departmentId) {
        return getListCache(CACHE_DOCTOR, "dept:" + departmentId, TTL_10_MINUTES,
                () -> doctorProfileRepository.findByDepartmentId(departmentId));
    }

    public DoctorProfile findDoctorById(Long id) {
        return getObjectCache(CACHE_DOCTOR, "id:" + id, TTL_10_MINUTES,
                () -> doctorProfileRepository.findById(id).orElse(null));
    }

    // ==================== 3. 症状库 & 关联关系 ====================

    public List<MedicalCapabilityKnowledge> findAllCapabilityKnowledge() {
        return getListCache(CACHE_CAPABILITY, "knowledge:all", TTL_60_MINUTES,
                medicalCapabilityRepository::findAllActive);
    }

    public List<DiseaseCapabilityRel> findCapabilitiesByDisease(String diseaseCode) {
        return getListCache(CACHE_CAPABILITY, "disease:" + diseaseCode, TTL_30_MINUTES,
                () -> diseaseCapabilityRelRepository.findByDiseaseCode(diseaseCode));
    }

    public List<DepartmentCapabilityRel> findCapabilitiesByDepartment(Long departmentId) {
        return getListCache(CACHE_CAPABILITY, "dept:" + departmentId, TTL_30_MINUTES,
                () -> departmentCapabilityRelRepository.findByDepartmentId(departmentId));
    }

    public List<DoctorCapabilityRel> findCapabilitiesByDoctor(Long doctorId) {
        return getListCache(CACHE_CAPABILITY, "doctor:" + doctorId, TTL_30_MINUTES,
                () -> doctorCapabilityRelRepository.findByDoctorId(doctorId));
    }

    public List<DepartmentCapabilityRel> findDepartmentsByCapability(String capabilityCode) {
        return getListCache(CACHE_CAPABILITY, "deptByCap:" + capabilityCode, TTL_30_MINUTES,
                () -> departmentCapabilityRelRepository.findByCapabilityCode(capabilityCode));
    }

    public void evictKnowledgeCaches() {
        redissonClient.getMapCache(CACHE_DISEASE).clear();
        redissonClient.getMapCache(CACHE_HOSPITAL).clear();
        redissonClient.getMapCache(CACHE_DEPARTMENT).clear();
        redissonClient.getMapCache(CACHE_DOCTOR).clear();
        redissonClient.getMapCache(CACHE_CAPABILITY).clear();
    }

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
