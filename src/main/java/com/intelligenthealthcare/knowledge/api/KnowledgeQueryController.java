package com.intelligenthealthcare.knowledge.api;

import com.intelligenthealthcare.knowledge.application.KnowledgeQueryApplicationService;
import com.intelligenthealthcare.knowledge.domain.model.DepartmentCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.domain.model.DoctorCapabilityRel;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库查询 API —— 暴露疾病 / 科室 / 医生 / 症状关联等基础数据的只读接口。
 * <p>
 * 所有数据经由 {@link KnowledgeQueryApplicationService} 层缓存，减少数据库压力并加速 AI 导诊匹配。
 */
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeQueryController {

    private final KnowledgeQueryApplicationService queryService;

    // ==================== 疾病 ====================

    @GetMapping("/diseases")
    public List<DiseaseMaster> listDiseases() {
        return queryService.findActiveDiseases();
    }

    @GetMapping("/diseases/{code}")
    public DiseaseMaster getDisease(@PathVariable String code) {
        return queryService.findDiseaseByCode(code);
    }

    @GetMapping("/diseases/search")
    public List<DiseaseMaster> searchDiseases(@RequestParam String q) {
        return queryService.searchDiseases(q);
    }

    @GetMapping("/diseases/{code}/aliases")
    public List<DiseaseAlias> getDiseaseAliases(@PathVariable String code) {
        return queryService.findAliasesByDiseaseCode(code);
    }

    // ==================== 医院 ====================

    @GetMapping("/hospitals")
    public List<Hospital> listHospitals() {
        return queryService.findActiveHospitals();
    }

    @GetMapping("/hospitals/{hospitalId}")
    public Hospital getHospital(@PathVariable String hospitalId) {
        return queryService.findHospitalById(hospitalId);
    }

    // ==================== 科室 ====================

    @GetMapping("/departments")
    public List<HospitalDepartment> listDepartments() {
        return queryService.findActiveDepartments();
    }

    @GetMapping("/departments/{id}")
    public HospitalDepartment getDepartment(@PathVariable Long id) {
        return queryService.findDepartmentById(id);
    }

    @GetMapping("/hospitals/{hospitalId}/departments")
    public List<HospitalDepartment> listDepartmentsByHospital(@PathVariable String hospitalId) {
        return queryService.findDepartmentsByHospital(hospitalId);
    }

    // ==================== 医生 ====================

    @GetMapping("/doctors/hot")
    public List<DoctorProfile> listHotDoctors() {
        return queryService.findHotDoctors();
    }

    @GetMapping("/doctors/{id}")
    public DoctorProfile getDoctor(@PathVariable Long id) {
        return queryService.findDoctorById(id);
    }

    @GetMapping("/departments/{departmentId}/doctors")
    public List<DoctorProfile> listDoctorsByDepartment(@PathVariable Long departmentId) {
        return queryService.findDoctorsByDepartment(departmentId);
    }

    // ==================== 医疗能力 / 症状关联 ====================

    @GetMapping("/capabilities")
    public List<MedicalCapabilityKnowledge> listCapabilities() {
        return queryService.findAllCapabilityKnowledge();
    }

    @GetMapping("/diseases/{code}/capabilities")
    public List<DiseaseCapabilityRel> listCapabilitiesByDisease(@PathVariable String code) {
        return queryService.findCapabilitiesByDisease(code);
    }

    @GetMapping("/departments/{id}/capabilities")
    public List<DepartmentCapabilityRel> listCapabilitiesByDepartment(@PathVariable Long id) {
        return queryService.findCapabilitiesByDepartment(id);
    }

    @GetMapping("/doctors/{id}/capabilities")
    public List<DoctorCapabilityRel> listCapabilitiesByDoctor(@PathVariable Long id) {
        return queryService.findCapabilitiesByDoctor(id);
    }

    /** 症状 → 科室路由：根据医疗能力编码反查支持科室 */
    @GetMapping("/capabilities/{code}/departments")
    public List<DepartmentCapabilityRel> listDepartmentsByCapability(@PathVariable String code) {
        return queryService.findDepartmentsByCapability(code);
    }
}
