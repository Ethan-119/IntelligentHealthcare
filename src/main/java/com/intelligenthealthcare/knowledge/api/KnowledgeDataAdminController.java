package com.intelligenthealthcare.knowledge.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import com.intelligenthealthcare.knowledge.domain.model.DoctorProfile;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.knowledge.domain.model.MedicalCapabilityKnowledge;
import com.intelligenthealthcare.knowledge.domain.repository.DiseaseMasterRepository;
import com.intelligenthealthcare.knowledge.domain.repository.DoctorProfileRepository;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalDepartmentRepository;
import com.intelligenthealthcare.knowledge.domain.repository.HospitalRepository;
import com.intelligenthealthcare.knowledge.domain.repository.MedicalCapabilityRepository;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DiseaseMasterMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.DoctorProfileMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.HospitalDepartmentMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.HospitalMapper;
import com.intelligenthealthcare.knowledge.infrastructure.persistence.MedicalCapabilityKnowledgeMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台：查看（分页）和切换 Knowledge 数据的热点状态。
 */
@RestController
@RequestMapping("/api/admin/knowledge/data")
@RequiredArgsConstructor
public class KnowledgeDataAdminController {

    private final DiseaseMasterRepository diseaseMasterRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalDepartmentRepository hospitalDepartmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final MedicalCapabilityRepository medicalCapabilityRepository;

    private final DiseaseMasterMapper diseaseMasterMapper;
    private final HospitalMapper hospitalMapper;
    private final HospitalDepartmentMapper hospitalDepartmentMapper;
    private final DoctorProfileMapper doctorProfileMapper;
    private final MedicalCapabilityKnowledgeMapper capabilityMapper;

    /**
     * 按类型分页查询。
     *
     * @param type     disease / hospital / department / doctor / capability
     * @param page     页码，从 1 开始
     * @param size     每页条数，默认 20
     * @param keyword  可选搜索关键词
     */
    @GetMapping("/{type}")
    public Map<String, Object> pageQuery(
            @PathVariable("type") String type,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword) {

        String normalizedType = type != null ? type.trim().toLowerCase() : "";
        IPage<?> result;

        switch (normalizedType) {
            case "disease": {
                Page<DiseaseMaster> p = new Page<>(page, size);
                result = diseaseMasterMapper.pageQuery(p, keyword);
                break;
            }
            case "hospital": {
                Page<Hospital> p = new Page<>(page, size);
                result = hospitalMapper.pageQuery(p, keyword);
                break;
            }
            case "department": {
                Page<HospitalDepartment> p = new Page<>(page, size);
                result = hospitalDepartmentMapper.pageQuery(p, keyword);
                break;
            }
            case "doctor": {
                Page<DoctorProfile> p = new Page<>(page, size);
                result = doctorProfileMapper.pageQuery(p, keyword);
                break;
            }
            case "capability": {
                Page<MedicalCapabilityKnowledge> p = new Page<>(page, size);
                result = capabilityMapper.pageQuery(p, keyword);
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的数据类型: " + type);
        }

        // 扁平化返回：records + 分页信息
        Map<String, Object> response = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        List<?> records = result.getRecords();
        for (int i = 0; i < records.size(); i++) {
            Object record = records.get(i);
            Map<String, Object> item = toSimpleItem(normalizedType, record);
            items.add(item);
        }
        response.put("records", items);
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        response.put("pages", result.getPages());
        return response;
    }

    /**
     * 将实体转为只有关键字段的简单 Map。
     */
    private Map<String, Object> toSimpleItem(String type, Object record) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (record == null) {
            return item;
        }
        switch (type) {
            case "disease": {
                DiseaseMaster d = (DiseaseMaster) record;
                item.put("id", d.getDiseaseCode());
                item.put("name", d.getDiseaseName());
                boolean active = d.getDeleted() == null || d.getDeleted() == 0;
                item.put("active", active);
                break;
            }
            case "hospital": {
                Hospital h = (Hospital) record;
                item.put("id", h.getHospitalId());
                item.put("name", h.getHospitalName());
                item.put("city", h.getCity());
                boolean active = h.getActiveStatus() != null && h.getActiveStatus() == 1
                        && (h.getDeleted() == null || h.getDeleted() == 0);
                item.put("active", active);
                break;
            }
            case "department": {
                HospitalDepartment d = (HospitalDepartment) record;
                item.put("id", d.getId());
                item.put("name", d.getDepartmentName());
                item.put("hospitalId", d.getHospitalId());
                boolean active = d.getActiveStatus() != null && d.getActiveStatus() == 1
                        && (d.getDeleted() == null || d.getDeleted() == 0);
                item.put("active", active);
                break;
            }
            case "doctor": {
                DoctorProfile d = (DoctorProfile) record;
                item.put("id", d.getId());
                item.put("name", d.getDoctorName());
                item.put("hospitalId", d.getHospitalId());
                item.put("title", d.getTitle());
                boolean active = d.getActiveStatus() != null && d.getActiveStatus() == 1;
                item.put("active", active);
                break;
            }
            case "capability": {
                MedicalCapabilityKnowledge c = (MedicalCapabilityKnowledge) record;
                item.put("id", c.getCapabilityCode());
                item.put("name", c.getCapabilityName());
                item.put("type", c.getCapabilityType());
                boolean active = c.getActiveStatus() != null && c.getActiveStatus() == 1;
                item.put("active", active);
                break;
            }
        }
        return item;
    }

    /**
     * 切换单条数据的 active 状态。
     */
    @PutMapping("/{type}/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable("type") String type, @PathVariable("id") String id) {
        String normalizedType = type != null ? type.trim().toLowerCase() : "";
        boolean newActive;

        switch (normalizedType) {
            case "disease": {
                Optional<DiseaseMaster> opt = diseaseMasterRepository.findByCodeAll(id);
                if (opt.isEmpty()) {
                    throw new IllegalArgumentException("疾病不存在: " + id);
                }
                DiseaseMaster entity = opt.get();
                int currentDeleted = entity.getDeleted() == null ? 0 : entity.getDeleted();
                entity.setDeleted(currentDeleted == 0 ? 1 : 0);
                diseaseMasterRepository.update(entity);
                newActive = entity.getDeleted() == 0;
                break;
            }
            case "hospital": {
                Optional<Hospital> opt = hospitalRepository.findByHospitalIdAll(id);
                if (opt.isEmpty()) {
                    throw new IllegalArgumentException("医院不存在: " + id);
                }
                Hospital entity = opt.get();
                int current = entity.getActiveStatus() == null ? 1 : entity.getActiveStatus();
                entity.setActiveStatus(current == 1 ? 0 : 1);
                hospitalRepository.update(entity);
                newActive = entity.getActiveStatus() == 1;
                break;
            }
            case "department": {
                Long deptId;
                try {
                    deptId = Long.valueOf(id);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("科室 ID 格式错误: " + id);
                }
                Optional<HospitalDepartment> opt = hospitalDepartmentRepository.findByIdAll(deptId);
                if (opt.isEmpty()) {
                    throw new IllegalArgumentException("科室不存在: " + id);
                }
                HospitalDepartment entity = opt.get();
                int current = entity.getActiveStatus() == null ? 1 : entity.getActiveStatus();
                entity.setActiveStatus(current == 1 ? 0 : 1);
                hospitalDepartmentRepository.update(entity);
                newActive = entity.getActiveStatus() == 1;
                break;
            }
            case "doctor": {
                Long doctorId;
                try {
                    doctorId = Long.valueOf(id);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("医生 ID 格式错误: " + id);
                }
                Optional<DoctorProfile> opt = doctorProfileRepository.findByIdAll(doctorId);
                if (opt.isEmpty()) {
                    throw new IllegalArgumentException("医生不存在: " + id);
                }
                DoctorProfile entity = opt.get();
                int current = entity.getActiveStatus() == null ? 1 : entity.getActiveStatus();
                entity.setActiveStatus(current == 1 ? 0 : 1);
                doctorProfileRepository.update(entity);
                newActive = entity.getActiveStatus() == 1;
                break;
            }
            case "capability": {
                Optional<MedicalCapabilityKnowledge> opt = medicalCapabilityRepository.findByCodeAll(id);
                if (opt.isEmpty()) {
                    throw new IllegalArgumentException("能力不存在: " + id);
                }
                MedicalCapabilityKnowledge entity = opt.get();
                int current = entity.getActiveStatus() == null ? 1 : entity.getActiveStatus();
                entity.setActiveStatus(current == 1 ? 0 : 1);
                medicalCapabilityRepository.update(entity);
                newActive = entity.getActiveStatus() == 1;
                break;
            }
            default:
                throw new IllegalArgumentException("不支持的数据类型: " + type);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", normalizedType);
        result.put("id", id);
        result.put("active", newActive);
        return result;
    }
}
