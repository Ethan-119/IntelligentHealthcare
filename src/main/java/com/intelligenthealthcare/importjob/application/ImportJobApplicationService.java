package com.intelligenthealthcare.importjob.application;

import com.intelligenthealthcare.importjob.domain.DatasetTypes;
import com.intelligenthealthcare.importjob.domain.ImportJobFactory;
import com.intelligenthealthcare.importjob.domain.exception.ImportJobNotFoundException;
import com.intelligenthealthcare.importjob.domain.exception.ImportReviewItemNotFoundException;
import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import com.intelligenthealthcare.importjob.domain.model.ImportJobCompletion;
import com.intelligenthealthcare.importjob.domain.model.ImportJobProgress;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import com.intelligenthealthcare.importjob.domain.model.ImportTableRow;
import com.intelligenthealthcare.importjob.domain.repository.ImportFailureLogRepository;
import com.intelligenthealthcare.importjob.domain.repository.ImportJobRecordRepository;
import com.intelligenthealthcare.importjob.domain.repository.ImportReviewItemRepository;
import com.intelligenthealthcare.importjob.domain.repository.KnowledgeImportRepository;
import com.intelligenthealthcare.importjob.domain.service.DiseaseAliasImportPolicy;
import com.intelligenthealthcare.importjob.domain.service.DiseaseMasterImportPolicy;
import com.intelligenthealthcare.importjob.infrastructure.file.ImportFileTableReader;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseAlias;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用服务（Application Service）：DDD 中负责**用例编排**与事务边界，不包含领域业务规则。
 * 规则在 {@link com.intelligenthealthcare.importjob.domain.service} 等处；持久化仅通过 {@link com.intelligenthealthcare.importjob.domain.repository}。
 */
@Service
public class ImportJobApplicationService {

    private static final int MAX_JOB_LIST = 200;

    private final ImportJobRecordRepository importJobRecordRepository;
    private final ImportFailureLogRepository importFailureLogRepository;
    private final ImportReviewItemRepository importReviewItemRepository;
    private final KnowledgeImportRepository knowledgeImportRepository;

    public ImportJobApplicationService(
            ImportJobRecordRepository importJobRecordRepository,
            ImportFailureLogRepository importFailureLogRepository,
            ImportReviewItemRepository importReviewItemRepository,
            KnowledgeImportRepository knowledgeImportRepository) {
        this.importJobRecordRepository = importJobRecordRepository;
        this.importFailureLogRepository = importFailureLogRepository;
        this.importReviewItemRepository = importReviewItemRepository;
        this.knowledgeImportRepository = knowledgeImportRepository;
    }

    public ImportJobRecord createAndRunImport(MultipartFile file, String datasetTypeRaw) {
        if (!StringUtils.hasText(datasetTypeRaw)) {
            throw new IllegalArgumentException("datasetType 不能为空");
        }
        String datasetType = datasetTypeRaw.trim().toUpperCase(Locale.ROOT);
        if (!DatasetTypes.isValid(datasetType)) {
            throw new IllegalArgumentException("不支持的 datasetType：" + datasetType);
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件为空");
        }

        ImportJobRecord job = ImportJobFactory.newRunningJob(datasetType, file.getOriginalFilename());
        importJobRecordRepository.insert(job);
        Long jobId = job.getId();

        List<ImportTableRow> rows;
        try {
            rows = ImportFileTableReader.read(file);
        } catch (IOException e) {
            importJobRecordRepository.updateAsFailed(jobId, "无法读取文件：" + e.getMessage());
            return mustGetJob(jobId);
        } catch (RuntimeException e) {
            importJobRecordRepository.updateAsFailed(
                    jobId, e.getMessage() != null ? e.getMessage() : "无法解析文件");
            return mustGetJob(jobId);
        }

        ImportJobProgress progress = new ImportJobProgress();
        if (DatasetTypes.DISEASE_MASTER.equals(datasetType)) {
            runDiseaseMasterImport(jobId, rows, progress);
        } else if (DatasetTypes.DISEASE_ALIAS.equals(datasetType)) {
            runDiseaseAliasImport(jobId, rows, progress);
        } else {
            importJobRecordRepository.updateAsFailed(jobId, "未实现的 datasetType：" + datasetType);
            return mustGetJob(jobId);
        }

        finishJobWithProgress(jobId, progress);
        return mustGetJob(jobId);
    }

    private void runDiseaseMasterImport(long jobId, List<ImportTableRow> rows, ImportJobProgress progress) {
        for (int i = 0; i < rows.size(); i++) {
            ImportTableRow tableRow = rows.get(i);
            DiseaseMasterImportPolicy.ParsedRow pr = DiseaseMasterImportPolicy.parse(tableRow);
            if (!pr.isOk()) {
                progress.apply(ImportLineProcessResult.fail(pr.getErrorMessage()));
                importFailureLogRepository.save(
                        ImportFailureLog.fromLineError(
                                jobId, tableRow.getFileLineNumber(), tableRow.toRawText(), pr.getErrorMessage()));
                continue;
            }
            DiseaseMaster rowAsDisease = pr.getDisease();
            DiseaseMaster existing =
                    knowledgeImportRepository.findActiveDiseaseByCode(rowAsDisease.getDiseaseCode());
            ImportLineProcessResult decided =
                    DiseaseMasterImportPolicy.evaluate(rowAsDisease, existing, tableRow.toRawText());
            persistDiseaseMasterLineOutcome(jobId, tableRow, progress, decided);
        }
    }

    private void persistDiseaseMasterLineOutcome(
            long jobId, ImportTableRow tableRow, ImportJobProgress progress, ImportLineProcessResult decided) {
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.INSERT_DISEASE_MASTER) {
            try {
                knowledgeImportRepository.saveDiseaseMaster(decided.getMasterToInsert());
                progress.apply(decided);
            } catch (RuntimeException ex) {
                progress.apply(ImportLineProcessResult.fail("写入 disease_master 失败：" + ex.getMessage()));
                importFailureLogRepository.save(
                        ImportFailureLog.fromLineError(
                                jobId,
                                tableRow.getFileLineNumber(),
                                tableRow.toRawText(),
                                "写入 disease_master 失败：" + ex.getMessage()));
            }
            return;
        }
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.NEED_REVIEW) {
            progress.apply(decided);
            importReviewItemRepository.save(ImportReviewItem.fromDraft(jobId, decided.getReview()));
            return;
        }
        progress.apply(decided);
    }

    private void runDiseaseAliasImport(long jobId, List<ImportTableRow> rows, ImportJobProgress progress) {
        for (int i = 0; i < rows.size(); i++) {
            ImportTableRow tableRow = rows.get(i);
            DiseaseAliasImportPolicy.ParsedRow pr = DiseaseAliasImportPolicy.parse(tableRow);
            if (!pr.isOk()) {
                progress.apply(ImportLineProcessResult.fail(pr.getErrorMessage()));
                importFailureLogRepository.save(
                        ImportFailureLog.fromLineError(
                                jobId, tableRow.getFileLineNumber(), tableRow.toRawText(), pr.getErrorMessage()));
                continue;
            }
            DiseaseAlias rowAsAlias = pr.getAlias();
            DiseaseMaster disease = knowledgeImportRepository.findActiveDiseaseByCode(rowAsAlias.getDiseaseCode());
            boolean exists = disease != null;
            boolean aliasExists =
                    knowledgeImportRepository.existsDiseaseAlias(
                            rowAsAlias.getDiseaseCode(), rowAsAlias.getAliasName());
            ImportLineProcessResult decided =
                    DiseaseAliasImportPolicy.evaluate(rowAsAlias, exists, aliasExists);
            persistDiseaseAliasLineOutcome(jobId, tableRow, progress, decided);
        }
    }

    private void persistDiseaseAliasLineOutcome(
            long jobId, ImportTableRow tableRow, ImportJobProgress progress, ImportLineProcessResult decided) {
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.INSERT_DISEASE_ALIAS) {
            try {
                knowledgeImportRepository.saveDiseaseAlias(decided.getAliasToInsert());
                progress.apply(decided);
            } catch (RuntimeException ex) {
                progress.apply(ImportLineProcessResult.fail("写入 disease_alias 失败：" + ex.getMessage()));
                importFailureLogRepository.save(
                        ImportFailureLog.fromLineError(
                                jobId,
                                tableRow.getFileLineNumber(),
                                tableRow.toRawText(),
                                "写入 disease_alias 失败：" + ex.getMessage()));
            }
            return;
        }
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.FAIL) {
            progress.apply(decided);
            importFailureLogRepository.save(
                    ImportFailureLog.fromLineError(
                            jobId,
                            tableRow.getFileLineNumber(),
                            tableRow.toRawText(),
                            decided.getFailureMessage()));
            return;
        }
        progress.apply(decided);
    }

    private void finishJobWithProgress(long jobId, ImportJobProgress progress) {
        ImportJobCompletion c = progress.toCompletion();
        importJobRecordRepository.updateOnCompletion(
                jobId,
                c.getStatus(),
                progress.getSuccessCount(),
                progress.getFailureCount(),
                progress.getReviewCount(),
                progress.getAutoMappedCount(),
                c.getMessage());
    }

    public List<ImportJobRecord> listRecentJobs() {
        return importJobRecordRepository.listRecent(MAX_JOB_LIST);
    }

    public ImportJobRecord getJobOrThrow(long jobId) {
        return importJobRecordRepository
                .findById(jobId)
                .orElseThrow(ImportJobNotFoundException::new);
    }

    public List<ImportFailureLog> listFailureLogs(long jobId) {
        getJobOrThrow(jobId);
        return importFailureLogRepository.listByJobIdOrderById(jobId);
    }

    public List<ImportReviewItem> listReviewItems(long jobId, Boolean resolved) {
        getJobOrThrow(jobId);
        return importReviewItemRepository.listByJobId(jobId, resolved);
    }

    public ImportReviewItem resolveReviewItem(long id, String resolutionNote) {
        ImportReviewItem item =
                importReviewItemRepository.findById(id).orElseThrow(ImportReviewItemNotFoundException::new);
        item.markResolvedWithNote(resolutionNote);
        importReviewItemRepository.update(item);
        return importReviewItemRepository.findById(id).orElseThrow(ImportReviewItemNotFoundException::new);
    }

    private ImportJobRecord mustGetJob(long jobId) {
        return importJobRecordRepository.findById(jobId).orElseThrow(ImportJobNotFoundException::new);
    }
}
