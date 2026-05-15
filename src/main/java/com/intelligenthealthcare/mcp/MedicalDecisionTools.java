package com.intelligenthealthcare.mcp;

import com.intelligenthealthcare.audit.application.AuditApplicationService;
import com.intelligenthealthcare.knowledge.application.KnowledgeQueryApplicationService;
import com.intelligenthealthcare.knowledge.domain.model.Hospital;
import com.intelligenthealthcare.knowledge.domain.model.HospitalDepartment;
import com.intelligenthealthcare.rag.application.RagQueryService;
import com.intelligenthealthcare.rag.application.dto.RagSearchHitDto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;

/**
 * 医疗决策工具集（MCP）：
 * 1) 知识检索；
 * 2) 医院/科室推荐；
 * 3) 急症预警落日志。
 */
@Component
@RequiredArgsConstructor
public class MedicalDecisionTools {

    private final RagQueryService ragQueryService;
    private final KnowledgeQueryApplicationService knowledgeQueryApplicationService;
    private final AuditApplicationService auditApplicationService;

    @Tool(description = "按症状关键词检索医学知识片段，用于补充诊疗建议依据")
    public String searchMedicalKnowledge(
            @ToolParam(description = "症状或医疗问题描述") String query,
            @ToolParam(description = "返回条数，建议 1~5") Integer topK) {
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
        if (!StringUtils.hasText(normalizedQuery)) {
            return "未提供有效检索问题。";
        }
        int limit = topK == null ? 3 : Math.max(1, Math.min(topK, 5));
        List<RagSearchHitDto> hits = ragQueryService.search(normalizedQuery, limit);
        if (hits.isEmpty()) {
            return "未检索到相关医学知识。";
        }
        StringBuilder sb = new StringBuilder("医学知识检索结果：");
        for (int i = 0; i < hits.size(); i++) {
            RagSearchHitDto hit = hits.get(i);
            sb.append("\n- [").append(i + 1).append("] ")
                    .append(defaultText(hit.content()))
                    .append("（distance=").append(hit.distance()).append("）");
        }
        return sb.toString();
    }

    @Tool(description = "基于城市/区域和症状描述推荐就近医院与科室")
    public String recommendHospital(
            @ToolParam(description = "城市，如 武汉市") String city,
            @ToolParam(description = "区域，如 江汉区，可为空") String area,
            @ToolParam(description = "症状描述") String symptomSummary) {
        List<Hospital> hospitals = knowledgeQueryApplicationService.findActiveHospitals();
        if (hospitals.isEmpty()) {
            return "当前无可推荐医院数据。";
        }

        String normalizedCity = normalizeText(city);
        String normalizedArea = normalizeText(area);
        List<Hospital> scoped = filterHospitals(hospitals, normalizedCity, normalizedArea);
        if (scoped.isEmpty()) {
            scoped = hospitals;
        }
        scoped.sort(new Comparator<Hospital>() {
            @Override
            public int compare(Hospital a, Hospital b) {
                BigDecimal sa = a.getAuthorityScore() == null ? BigDecimal.ZERO : a.getAuthorityScore();
                BigDecimal sb = b.getAuthorityScore() == null ? BigDecimal.ZERO : b.getAuthorityScore();
                return sb.compareTo(sa);
            }
        });
        Hospital chosen = scoped.get(0);
        String department = pickDepartment(chosen.getHospitalId(), symptomSummary);
        StringBuilder sb = new StringBuilder("就近就医推荐：");
        sb.append("\n- 医院：").append(defaultText(chosen.getHospitalName()));
        sb.append("\n- 城市/区域：").append(defaultText(chosen.getCity())).append(" ").append(defaultText(chosen.getDistrictName()));
        sb.append("\n- 建议科室：").append(defaultText(department));
        sb.append("\n- 说明：该推荐基于当前症状关键词和就近规则，非最终临床诊断。");
        return sb.toString();
    }

    @Tool(description = "当疑似急症时记录预警日志，供后续审计追踪")
    public String logEmergency(
            @ToolParam(description = "用户当前症状描述") String symptomSummary,
            @ToolParam(description = "触发预警原因") String reason,
            @ToolParam(description = "会话ID，可为空") String sessionId,
            @ToolParam(description = "用户ID，可为空") String userId) {
        String summary = StringUtils.hasText(symptomSummary) ? symptomSummary.trim() : "未提供症状";
        String warningReason = StringUtils.hasText(reason) ? reason.trim() : "疑似急症";
        String extra = "sessionId=" + normalizeText(sessionId) + ", userId=" + normalizeText(userId);
        auditApplicationService.recordEmergencyAlert(summary, warningReason + "；" + extra);
        return "已记录急症预警：" + warningReason;
    }

    private List<Hospital> filterHospitals(List<Hospital> hospitals, String city, String area) {
        if (!StringUtils.hasText(city) && !StringUtils.hasText(area)) {
            return new ArrayList<>(hospitals);
        }
        List<Hospital> result = new ArrayList<>();
        for (int i = 0; i < hospitals.size(); i++) {
            Hospital hospital = hospitals.get(i);
            if (StringUtils.hasText(city)) {
                String hospitalCity = normalizeText(hospital.getCity());
                if (!hospitalCity.contains(city)) {
                    continue;
                }
            }
            if (StringUtils.hasText(area)) {
                String district = normalizeText(hospital.getDistrictName());
                if (!district.contains(area)) {
                    continue;
                }
            }
            result.add(hospital);
        }
        return result;
    }

    private String pickDepartment(String hospitalId, String symptomSummary) {
        List<HospitalDepartment> departments = knowledgeQueryApplicationService.findDepartmentsByHospital(hospitalId);
        if (departments.isEmpty()) {
            return "全科/急诊";
        }
        String text = normalizeText(symptomSummary);
        for (int i = 0; i < departments.size(); i++) {
            HospitalDepartment d = departments.get(i);
            String name = normalizeText(d.getDepartmentName());
            if (containsAny(text, "发热", "发烧", "感染", "咽痛") && name.contains("发热")) {
                return d.getDepartmentName();
            }
            if (containsAny(text, "呕吐", "恶心", "腹痛", "腹泻") && name.contains("消化")) {
                return d.getDepartmentName();
            }
            if (containsAny(text, "头痛", "头晕", "意识") && name.contains("神经")) {
                return d.getDepartmentName();
            }
        }
        return departments.get(0).getDepartmentName();
    }

    private boolean containsAny(String text, String... keywords) {
        for (int i = 0; i < keywords.length; i++) {
            if (text.contains(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}
