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
        return recommendHospital(city, area, symptomSummary, null, null);
    }

    // 基于城市/区域和症状推荐医院，优先按用户坐标（源自浏览器 Geolocation API）计算
    // Haversine 距离排序；无坐标时降级为仅按权威评分排序。
    public String recommendHospital(
            String city,
            String area,
            String symptomSummary,
            BigDecimal userLatitude,
            BigDecimal userLongitude) {
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
        boolean hasUserLocation = userLatitude != null && userLongitude != null;
        scoped.sort(buildHospitalComparator(hasUserLocation, userLatitude, userLongitude));
        Hospital chosen = scoped.get(0);
        String department = pickDepartment(chosen.getHospitalId(), symptomSummary);
        StringBuilder sb = new StringBuilder("就近就医推荐：");
        sb.append("\n- 医院：").append(defaultText(chosen.getHospitalName()));
        sb.append("\n- 城市/区域：").append(defaultText(chosen.getCity())).append(" ").append(defaultText(chosen.getDistrictName()));
        sb.append("\n- 建议科室：").append(defaultText(department));
        if (hasUserLocation && chosen.getLatitude() != null && chosen.getLongitude() != null) {
            double km = distanceKm(userLatitude, userLongitude, chosen.getLatitude(), chosen.getLongitude());
            sb.append("\n- 预计距离：约 ").append(String.format(Locale.ROOT, "%.1f", km)).append(" km");
        }
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

    // 医院排序策略：有用户坐标时优先按 Haversine 距离升序（距离相同再按权威评分降序）；
    // 无坐标时仅按权威评分降序。
    private Comparator<Hospital> buildHospitalComparator(
            boolean hasUserLocation,
            BigDecimal userLatitude,
            BigDecimal userLongitude) {
        return new Comparator<Hospital>() {
            @Override
            public int compare(Hospital a, Hospital b) {
                if (hasUserLocation) {
                    double da = distanceOrMax(userLatitude, userLongitude, a);
                    double db = distanceOrMax(userLatitude, userLongitude, b);
                    int byDistance = Double.compare(da, db);
                    if (byDistance != 0) {
                        return byDistance;
                    }
                }
                BigDecimal sa = a.getAuthorityScore() == null ? BigDecimal.ZERO : a.getAuthorityScore();
                BigDecimal sb = b.getAuthorityScore() == null ? BigDecimal.ZERO : b.getAuthorityScore();
                return sb.compareTo(sa);
            }
        };
    }

    private double distanceOrMax(BigDecimal userLat, BigDecimal userLon, Hospital hospital) {
        if (hospital == null || hospital.getLatitude() == null || hospital.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return distanceKm(userLat, userLon, hospital.getLatitude(), hospital.getLongitude());
    }

    // Haversine 公式：根据两个经纬度坐标计算球面距离（单位 km）。
    // 地球半径 r = 6371 km，适用于医院推荐等中等精度场景（误差 < 0.3%）。
    private double distanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double r = 6371.0d;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue()))
                * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
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
