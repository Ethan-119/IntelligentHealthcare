package com.intelligenthealthcare.importjob.api;

import com.intelligenthealthcare.importjob.api.dto.ImportFailureLogResponse;
import com.intelligenthealthcare.importjob.api.dto.ImportJobResponse;
import com.intelligenthealthcare.importjob.api.dto.ImportReviewItemResponse;
import com.intelligenthealthcare.importjob.api.dto.ResolveImportReviewRequest;
import com.intelligenthealthcare.importjob.application.ImportJobApplicationService;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理端知识库导入：需登录。上传 Excel/CSV 第一行为表头。
 *
 * <p>DISEASE_MASTER 列名示例：disease_code, disease_name, symptom_keywords, gender_rule, age_min, age_max, age_group,
 * urgency_level, review_status, aliases_json
 *
 * <p>DISEASE_ALIAS 列名示例：disease_code, alias_name, alias_type, source
 */
@RestController
@RequestMapping("/api/admin/import/jobs")
public class ImportJobController {

    private final ImportJobApplicationService importJobApplicationService;

    public ImportJobController(ImportJobApplicationService importJobApplicationService) {
        this.importJobApplicationService = importJobApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportJobResponse upload(
            @RequestParam("file") MultipartFile file, @RequestParam("datasetType") String datasetType) {
        ImportJobRecord job = importJobApplicationService.createAndRunImport(file, datasetType);
        return ImportJobResponse.fromEntity(job);
    }

    @GetMapping
    public List<ImportJobResponse> list() {
        List<ImportJobRecord> list = importJobApplicationService.listRecentJobs();
        List<ImportJobResponse> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(ImportJobResponse.fromEntity(list.get(i)));
        }
        return out;
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable("id") long id) {
        return ImportJobResponse.fromEntity(importJobApplicationService.getJobOrThrow(id));
    }

    @GetMapping("/{id}/failures")
    public List<ImportFailureLogResponse> listFailures(@PathVariable("id") long id) {
        return ImportFailureLogResponse.fromList(importJobApplicationService.listFailureLogs(id));
    }

    @GetMapping("/{id}/review-items")
    public List<ImportReviewItemResponse> listReviewItems(
            @PathVariable("id") long id, @RequestParam(value = "resolved", required = false) Boolean resolved) {
        return ImportReviewItemResponse.fromList(importJobApplicationService.listReviewItems(id, resolved));
    }

    @PutMapping("/review-items/{reviewId}/resolve")
    public ImportReviewItemResponse resolve(
            @PathVariable("reviewId") long reviewId, @Valid @RequestBody ResolveImportReviewRequest body) {
        return ImportReviewItemResponse.fromEntity(
                importJobApplicationService.resolveReviewItem(reviewId, body.getResolutionNote()));
    }
}
