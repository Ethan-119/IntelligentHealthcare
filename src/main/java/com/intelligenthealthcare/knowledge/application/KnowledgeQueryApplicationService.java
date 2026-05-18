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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private static final long HOT_TTL_24_HOURS_MINUTES = 24L * 60L;
    private static final List<String> HOT_SCOPE_ALL = List.of("disease", "hospital", "department", "doctor", "capability");

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

    public List<DoctorProfile> findDoctorsByHospital(String hospitalId) {
        return getListCache(CACHE_DOCTOR, "hospital:" + hospitalId, TTL_10_MINUTES,
                () -> doctorProfileRepository.findByHospitalId(hospitalId));
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
        addHotspots(HOT_SCOPE_ALL);
    }

    public HotspotWarmupResult addHotspots(List<String> scopes) {
        List<String> normalizedScopes = normalizeScopes(scopes);
        int warmedEntries = 0;
        for (int i = 0; i < normalizedScopes.size(); i++) {
            String scope = normalizedScopes.get(i);
            if ("disease".equals(scope)) {
                warmedEntries += warmDiseaseHotspots();
            } else if ("hospital".equals(scope)) {
                warmedEntries += warmHospitalHotspots();
            } else if ("department".equals(scope)) {
                warmedEntries += warmDepartmentHotspots();
            } else if ("doctor".equals(scope)) {
                warmedEntries += warmDoctorHotspots();
            } else if ("capability".equals(scope)) {
                warmedEntries += warmCapabilityHotspots();
            }
        }
        return new HotspotWarmupResult(normalizedScopes, warmedEntries);
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

    private List<String> normalizeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return HOT_SCOPE_ALL;
        }
        List<String> result = new ArrayList<>();
        for (int i = 0; i < scopes.size(); i++) {
            String raw = scopes.get(i);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String normalized = raw.trim().toLowerCase(Locale.ROOT);
            if (!HOT_SCOPE_ALL.contains(normalized)) {
                continue;
            }
            if (!result.contains(normalized)) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? HOT_SCOPE_ALL : result;
    }

    private int warmDiseaseHotspots() {
        int warmed = 0;
        List<DiseaseMaster> diseases = diseaseMasterRepository.findAllActive();
        if (putHotListCache(CACHE_DISEASE, "allActive", diseases)) {
            warmed++;
        }
        for (int i = 0; i < diseases.size(); i++) {
            DiseaseMaster disease = diseases.get(i);
            if (disease == null || disease.getDiseaseCode() == null || disease.getDiseaseCode().isBlank()) {
                continue;
            }
            String code = disease.getDiseaseCode().trim();
            if (putHotObjectCache(CACHE_DISEASE, "code:" + code, disease)) {
                warmed++;
            }
            List<DiseaseAlias> aliases = diseaseAliasRepository.findByDiseaseCode(code);
            if (putHotListCache(CACHE_DISEASE, "alias:" + code, aliases)) {
                warmed++;
            }
            List<DiseaseCapabilityRel> rels = diseaseCapabilityRelRepository.findByDiseaseCode(code);
            if (putHotListCache(CACHE_CAPABILITY, "disease:" + code, rels)) {
                warmed++;
            }
        }
        return warmed;
    }

    private int warmHospitalHotspots() {
        int warmed = 0;
        List<Hospital> hospitals = hospitalRepository.findAllActive();
        if (putHotListCache(CACHE_HOSPITAL, "allActive", hospitals)) {
            warmed++;
        }
        for (int i = 0; i < hospitals.size(); i++) {
            Hospital hospital = hospitals.get(i);
            if (hospital == null || hospital.getHospitalId() == null || hospital.getHospitalId().isBlank()) {
                continue;
            }
            String hospitalId = hospital.getHospitalId().trim();
            if (putHotObjectCache(CACHE_HOSPITAL, "id:" + hospitalId, hospital)) {
                warmed++;
            }
            List<HospitalDepartment> departments = hospitalDepartmentRepository.findByHospitalId(hospitalId);
            if (putHotListCache(CACHE_DEPARTMENT, "hospital:" + hospitalId, departments)) {
                warmed++;
            }
        }
        return warmed;
    }

    private int warmDepartmentHotspots() {
        int warmed = 0;
        List<HospitalDepartment> departments = hospitalDepartmentRepository.findAllActive();
        if (putHotListCache(CACHE_DEPARTMENT, "allActive", departments)) {
            warmed++;
        }
        for (int i = 0; i < departments.size(); i++) {
            HospitalDepartment department = departments.get(i);
            if (department == null || department.getId() == null) {
                continue;
            }
            Long departmentId = department.getId();
            if (putHotObjectCache(CACHE_DEPARTMENT, "id:" + departmentId, department)) {
                warmed++;
            }
            List<DepartmentCapabilityRel> rels = departmentCapabilityRelRepository.findByDepartmentId(departmentId);
            if (putHotListCache(CACHE_CAPABILITY, "dept:" + departmentId, rels)) {
                warmed++;
            }
            List<DoctorProfile> doctors = doctorProfileRepository.findByDepartmentId(departmentId);
            if (putHotListCache(CACHE_DOCTOR, "dept:" + departmentId, doctors)) {
                warmed++;
            }
        }
        return warmed;
    }

    private int warmDoctorHotspots() {
        int warmed = 0;
        List<DoctorProfile> hotDoctors = doctorProfileRepository.findHotDoctors(50);
        if (putHotListCache(CACHE_DOCTOR, "hot", hotDoctors)) {
            warmed++;
        }
        for (int i = 0; i < hotDoctors.size(); i++) {
            DoctorProfile doctor = hotDoctors.get(i);
            if (doctor == null || doctor.getId() == null) {
                continue;
            }
            Long doctorId = doctor.getId();
            if (putHotObjectCache(CACHE_DOCTOR, "id:" + doctorId, doctor)) {
                warmed++;
            }
            List<DoctorCapabilityRel> rels = doctorCapabilityRelRepository.findByDoctorId(doctorId);
            if (putHotListCache(CACHE_CAPABILITY, "doctor:" + doctorId, rels)) {
                warmed++;
            }
        }
        return warmed;
    }

    private int warmCapabilityHotspots() {
        int warmed = 0;
        List<MedicalCapabilityKnowledge> capabilities = medicalCapabilityRepository.findAllActive();
        if (putHotListCache(CACHE_CAPABILITY, "knowledge:all", capabilities)) {
            warmed++;
        }
        for (int i = 0; i < capabilities.size(); i++) {
            MedicalCapabilityKnowledge capability = capabilities.get(i);
            if (capability == null || capability.getCapabilityCode() == null || capability.getCapabilityCode().isBlank()) {
                continue;
            }
            String code = capability.getCapabilityCode().trim();
            List<DepartmentCapabilityRel> departments = departmentCapabilityRelRepository.findByCapabilityCode(code);
            if (putHotListCache(CACHE_CAPABILITY, "deptByCap:" + code, departments)) {
                warmed++;
            }
        }
        return warmed;
    }

    private <T> boolean putHotObjectCache(String mapName, String key, T value) {
        if (value == null) {
            return false;
        }
        redissonClient.getMapCache(mapName).put(key, value, HOT_TTL_24_HOURS_MINUTES, TimeUnit.MINUTES);
        return true;
    }

    private <T> boolean putHotListCache(String mapName, String key, List<T> values) {
        if (values == null) {
            return false;
        }
        return putHotObjectCache(mapName, key, values);
    }

    public record HotspotWarmupResult(List<String> scopes, int warmedEntries) {
    }
}
