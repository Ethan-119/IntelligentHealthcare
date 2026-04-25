package com.intelligenthealthcare.importjob.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.intelligenthealthcare.importjob.domain.DatasetTypes;
import com.intelligenthealthcare.importjob.domain.ImportJobFactory;
import com.intelligenthealthcare.importjob.domain.ImportJobStatuses;
import com.intelligenthealthcare.importjob.domain.exception.ImportJobNotFoundException;
import com.intelligenthealthcare.importjob.domain.exception.ImportReviewItemNotFoundException;
import com.intelligenthealthcare.importjob.domain.model.DiseaseAliasImportLine;
import com.intelligenthealthcare.importjob.domain.model.DiseaseAliasParseResult;
import com.intelligenthealthcare.importjob.domain.model.DiseaseMasterImportLine;
import com.intelligenthealthcare.importjob.domain.model.DiseaseMasterParseResult;
import com.intelligenthealthcare.importjob.domain.model.ImportFailureLog;
import com.intelligenthealthcare.importjob.domain.model.ImportJobCompletion;
import com.intelligenthealthcare.importjob.domain.model.ImportJobProgress;
import com.intelligenthealthcare.importjob.domain.model.ImportJobRecord;
import com.intelligenthealthcare.importjob.domain.model.ImportLineProcessResult;
import com.intelligenthealthcare.importjob.domain.model.ImportReviewItem;
import com.intelligenthealthcare.importjob.domain.model.ImportTableRow;
import com.intelligenthealthcare.importjob.domain.port.KnowledgeImportGateway;
import com.intelligenthealthcare.importjob.domain.service.DiseaseAliasImportPolicy;
import com.intelligenthealthcare.importjob.domain.service.DiseaseMasterImportPolicy;
import com.intelligenthealthcare.importjob.infrastructure.file.ImportFileTableReader;
import com.intelligenthealthcare.importjob.infrastructure.persistence.ImportFailureLogMapper;
import com.intelligenthealthcare.importjob.infrastructure.persistence.ImportJobRecordMapper;
import com.intelligenthealthcare.importjob.infrastructure.persistence.ImportReviewItemMapper;
import com.intelligenthealthcare.knowledge.domain.model.DiseaseMaster;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用例编排：创建任务、读文件、调用领域策略与防腐层落库，不在此写业务规则。
 */
@Service
public class ImportJobApplicationService {

    private static final int MAX_JOB_LIST = 200;

    private final ImportJobRecordMapper importJobRecordMapper;
    private final ImportFailureLogMapper importFailureLogMapper;
    private final ImportReviewItemMapper importReviewItemMapper;
    private final KnowledgeImportGateway knowledgeImportGateway;

    public ImportJobApplicationService(
            ImportJobRecordMapper importJobRecordMapper,
            ImportFailureLogMapper importFailureLogMapper,
            ImportReviewItemMapper importReviewItemMapper,
            KnowledgeImportGateway knowledgeImportGateway) {
        this.importJobRecordMapper = importJobRecordMapper;
        this.importFailureLogMapper = importFailureLogMapper;
        this.importReviewItemMapper = importReviewItemMapper;
        this.knowledgeImportGateway = knowledgeImportGateway;
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
        importJobRecordMapper.insert(job);
        Long jobId = job.getId();

        List<ImportTableRow> rows;
        try {
            rows = ImportFileTableReader.read(file);
        } catch (IOException e) {
            markJobFailed(jobId, "无法读取文件：" + e.getMessage());
            return mustGetJob(jobId);
        } catch (RuntimeException e) {
            markJobFailed(jobId, e.getMessage() != null ? e.getMessage() : "无法解析文件");
            return mustGetJob(jobId);
        }

        ImportJobProgress progress = new ImportJobProgress();
        if (DatasetTypes.DISEASE_MASTER.equals(datasetType)) {
            runDiseaseMasterImport(jobId, rows, progress);
        } else if (DatasetTypes.DISEASE_ALIAS.equals(datasetType)) {
            runDiseaseAliasImport(jobId, rows, progress);
        } else {
            markJobFailed(jobId, "未实现的 datasetType：" + datasetType);
            return mustGetJob(jobId);
        }

        finishJobWithProgress(jobId, progress);
        return mustGetJob(jobId);
    }

    private void runDiseaseMasterImport(long jobId, List<ImportTableRow> rows, ImportJobProgress progress) {
        for (int i = 0; i < rows.size(); i++) {
            ImportTableRow tableRow = rows.get(i);
            DiseaseMasterParseResult pr = DiseaseMasterImportLine.parse(tableRow);
            if (!pr.isOk()) {
                progress.apply(ImportLineProcessResult.fail(pr.getErrorMessage()));
                importFailureLogMapper.insert(
                        ImportFailureLog.fromLineError(
                                jobId, tableRow.getFileLineNumber(), tableRow.toRawText(), pr.getErrorMessage()));
                continue;
            }
            DiseaseMasterImportLine line = pr.getLine();
            DiseaseMaster existing = knowledgeImportGateway.findActiveDiseaseByCode(line.getDiseaseCode());
            ImportLineProcessResult decided =
                    DiseaseMasterImportPolicy.evaluate(line, existing, tableRow.toRawText());
            persistDiseaseMasterLineOutcome(jobId, tableRow, progress, decided);
        }
    }

    private void persistDiseaseMasterLineOutcome(
            long jobId, ImportTableRow tableRow, ImportJobProgress progress, ImportLineProcessResult decided) {
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.INSERT_DISEASE_MASTER) {
            try {
                knowledgeImportGateway.saveDiseaseMaster(decided.getMasterToInsert());
                progress.apply(decided);
            } catch (RuntimeException ex) {
                progress.apply(ImportLineProcessResult.fail("写入 disease_master 失败：" + ex.getMessage()));
                importFailureLogMapper.insert(
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
            importReviewItemMapper.insert(ImportReviewItem.fromDraft(jobId, decided.getReview()));
            return;
        }
        progress.apply(decided);
    }

    private void runDiseaseAliasImport(long jobId, List<ImportTableRow> rows, ImportJobProgress progress) {
        for (int i = 0; i < rows.size(); i++) {
            ImportTableRow tableRow = rows.get(i);
            DiseaseAliasParseResult pr = DiseaseAliasImportLine.parse(tableRow);
            if (!pr.isOk()) {
                progress.apply(ImportLineProcessResult.fail(pr.getErrorMessage()));
                importFailureLogMapper.insert(
                        ImportFailureLog.fromLineError(
                                jobId, tableRow.getFileLineNumber(), tableRow.toRawText(), pr.getErrorMessage()));
                continue;
            }
            DiseaseAliasImportLine line = pr.getLine();
            DiseaseMaster disease = knowledgeImportGateway.findActiveDiseaseByCode(line.getDiseaseCode());
            boolean exists = disease != null;
            boolean aliasExists =
                    knowledgeImportGateway.existsDiseaseAlias(line.getDiseaseCode(), line.getAliasName());
            ImportLineProcessResult decided =
                    DiseaseAliasImportPolicy.evaluate(line, exists, aliasExists);
            persistDiseaseAliasLineOutcome(jobId, tableRow, progress, decided);
        }
    }

    private void persistDiseaseAliasLineOutcome(
            long jobId, ImportTableRow tableRow, ImportJobProgress progress, ImportLineProcessResult decided) {
        if (decided.getOutcome() == ImportLineProcessResult.Outcome.INSERT_DISEASE_ALIAS) {
            try {
                knowledgeImportGateway.saveDiseaseAlias(decided.getAliasToInsert());
                progress.apply(decided);
            } catch (RuntimeException ex) {
                progress.apply(ImportLineProcessResult.fail("写入 disease_alias 失败：" + ex.getMessage()));
                importFailureLogMapper.insert(
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
            importFailureLogMapper.insert(
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
        LambdaUpdateWrapper<ImportJobRecord> u = new LambdaUpdateWrapper<>();
        u.eq(ImportJobRecord::getId, jobId);
        u.set(ImportJobRecord::getStatus, c.getStatus());
        u.set(ImportJobRecord::getSuccessCount, progress.getSuccessCount());
        u.set(ImportJobRecord::getFailureCount, progress.getFailureCount());
        u.set(ImportJobRecord::getReviewCount, progress.getReviewCount());
        u.set(ImportJobRecord::getAutoMappedCount, progress.getAutoMappedCount());
        u.set(ImportJobRecord::getMessage, c.getMessage());
        importJobRecordMapper.update(null, u);
    }

    public List<ImportJobRecord> listRecentJobs() {
        LambdaQueryWrapper<ImportJobRecord> w = new LambdaQueryWrapper<>();
        w.orderByDesc(ImportJobRecord::getId);
        w.last("LIMIT " + MAX_JOB_LIST);
        return importJobRecordMapper.selectList(w);
    }

    public ImportJobRecord getJobOrThrow(long jobId) {
        ImportJobRecord r = importJobRecordMapper.selectById(jobId);
        if (r == null) {
            throw new ImportJobNotFoundException();
        }
        return r;
    }

    public List<ImportFailureLog> listFailureLogs(long jobId) {
        getJobOrThrow(jobId);
        LambdaQueryWrapper<ImportFailureLog> w = new LambdaQueryWrapper<>();
        w.eq(ImportFailureLog::getJobId, jobId);
        w.orderByAsc(ImportFailureLog::getId);
        return importFailureLogMapper.selectList(w);
    }

    public List<ImportReviewItem> listReviewItems(long jobId, Boolean resolved) {
        getJobOrThrow(jobId);
        LambdaQueryWrapper<ImportReviewItem> w = new LambdaQueryWrapper<>();
        w.eq(ImportReviewItem::getJobId, jobId);
        if (resolved != null) {
            w.eq(ImportReviewItem::getResolved, Boolean.TRUE.equals(resolved) ? 1 : 0);
        }
        w.orderByAsc(ImportReviewItem::getId);
        return importReviewItemMapper.selectList(w);
    }

    public ImportReviewItem resolveReviewItem(long id, String resolutionNote) {
        ImportReviewItem item = importReviewItemMapper.selectById(id);
        if (item == null) {
            throw new ImportReviewItemNotFoundException();
        }
        item.markResolvedWithNote(resolutionNote);
        importReviewItemMapper.updateById(item);
        return importReviewItemMapper.selectById(id);
    }

    private void markJobFailed(long jobId, String message) {
        LambdaUpdateWrapper<ImportJobRecord> u = new LambdaUpdateWrapper<>();
        u.eq(ImportJobRecord::getId, jobId);
        u.set(ImportJobRecord::getStatus, ImportJobStatuses.FAILED);
        u.set(ImportJobRecord::getMessage, message);
        importJobRecordMapper.update(null, u);
    }

    private ImportJobRecord mustGetJob(long jobId) {
        ImportJobRecord r = importJobRecordMapper.selectById(jobId);
        if (r == null) {
            throw new ImportJobNotFoundException();
        }
        return r;
    }
}
