package com.intelligenthealthcare.rag.api;

import com.intelligenthealthcare.rag.api.dto.DocumentSummaryResponse;
import com.intelligenthealthcare.rag.api.dto.DocumentUploadResponse;
import com.intelligenthealthcare.rag.application.RagIngestionService;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import com.intelligenthealthcare.rag.domain.repository.RagDocumentRepository;
import com.intelligenthealthcare.rag.infrastructure.parsing.DocumentTextExtractor;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理后台文档摄入接口：管理员上传多格式文档（PDF/DOC/PPT/CSV 等），
 * 系统自动提取文本、滑动窗口切分并写入向量库。
 */
@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
public class DocumentIngestController {

    private final RagIngestionService ragIngestionService;
    private final DocumentTextExtractor textExtractor;
    private final RagDocumentRepository ragDocumentRepository;
    private final MongoTemplate mongoTemplate;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentUploadResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }

        String originalFilename = file.getOriginalFilename();
        String documentName = sanitizeFilename(originalFilename);

        String text;
        try {
            text = textExtractor.extract(originalFilename, file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("文件内容提取失败: " + e.getMessage(), e);
        }

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("无法从文件中提取文本内容，文件可能为空或仅包含图片");
        }

        int chunkCount = ragIngestionService.ingestDocument(documentName, text);

        return DocumentUploadResponse.builder()
                .documentName(documentName)
                .chunkCount(chunkCount)
                .message("上传成功，共生成 " + chunkCount + " 个文本块")
                .build();
    }

    @GetMapping
    public List<DocumentSummaryResponse> list() {
        List<String> sourceIds = ragDocumentRepository.findDistinctSourceIdsBySourceType(RagSourceType.DOCUMENT);
        List<DocumentSummaryResponse> result = new ArrayList<>();
        for (int i = 0; i < sourceIds.size(); i++) {
            String sourceId = sourceIds.get(i);

            // 总块数
            Query totalQuery = new Query();
            totalQuery.addCriteria(Criteria.where("sourceType").is(RagSourceType.DOCUMENT)
                    .and("sourceId").is(sourceId));
            long totalCount = mongoTemplate.count(totalQuery, RagDocumentChunk.class);

            // 活跃块数：大于 0 表示文档处于"上架"状态
            Query activeQuery = new Query();
            activeQuery.addCriteria(Criteria.where("sourceType").is(RagSourceType.DOCUMENT)
                    .and("sourceId").is(sourceId)
                    .and("active").is(true));
            long activeCount = mongoTemplate.count(activeQuery, RagDocumentChunk.class);
            boolean isActive = activeCount > 0;

            result.add(DocumentSummaryResponse.builder()
                    .documentName(sourceId)
                    .chunkCount((int) totalCount)
                    .active(isActive)
                    .build());
        }
        return result;
    }

    /**
     * 下架文档：将所有块的 active 设为 false，向量检索不再命中。
     */
    @PutMapping("/{documentName}/deactivate")
    public String deactivate(@PathVariable("documentName") String documentName) {
        String name = sanitizeFilename(documentName);
        ragDocumentRepository.setActiveBySourceTypeAndSourceId(RagSourceType.DOCUMENT, name, false);
        return "已下架：" + name;
    }

    /**
     * 上架文档：将所有块的 active 设为 true，恢复检索。
     */
    @PutMapping("/{documentName}/activate")
    public String activate(@PathVariable("documentName") String documentName) {
        String name = sanitizeFilename(documentName);
        ragDocumentRepository.setActiveBySourceTypeAndSourceId(RagSourceType.DOCUMENT, name, true);
        return "已上架：" + name;
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_document";
        }
        // 去掉路径分隔符，只保留文件名部分
        String name = filename.replace('\\', '/');
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        // 限制长度
        if (name.length() > 255) {
            name = name.substring(0, 255);
        }
        return name.isBlank() ? "unnamed_document" : name;
    }
}
