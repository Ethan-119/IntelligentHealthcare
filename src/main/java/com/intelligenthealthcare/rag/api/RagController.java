package com.intelligenthealthcare.rag.api;

import com.intelligenthealthcare.rag.api.dto.RagIngestRequest;
import com.intelligenthealthcare.rag.api.dto.RagIngestResponse;
import com.intelligenthealthcare.rag.api.dto.RagSearchRequest;
import com.intelligenthealthcare.rag.api.dto.RagSearchResponse;
import com.intelligenthealthcare.rag.application.RagIngestCommand;
import com.intelligenthealthcare.rag.application.RagIngestionService;
import com.intelligenthealthcare.rag.application.RagQueryService;
import com.intelligenthealthcare.rag.config.RagProperties;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIngestionService ingestionService;
    private final RagQueryService queryService;
    private final RagProperties ragProperties;

    @PostMapping("/ingest")
    public RagIngestResponse ingest(@Valid @RequestBody RagIngestRequest request) {
        var cmd =
                new RagIngestCommand(
                        request.getSourceType(),
                        request.getSourceId(),
                        request.getChunkKey(),
                        request.getContent());
        return RagIngestResponse.builder()
                .id(ingestionService.upsert(cmd))
                .build();
    }

    @PostMapping("/search")
    public RagSearchResponse search(@Valid @RequestBody RagSearchRequest request) {
        int ck = request.getCandidateK() > 0
                ? request.getCandidateK()
                : request.getTopK() * ragProperties.getCandidateMultiplier();
        var hits = queryService.search(request.getQuery(), request.getTopK(), ck);
        var items =
                hits.stream()
                        .map(
                                h ->
                                        RagSearchResponse.RagSearchItem.builder()
                                                .id(h.id())
                                                .sourceType(h.sourceType())
                                                .sourceId(h.sourceId())
                                                .chunkKey(h.chunkKey())
                                                .content(h.content())
                                                .distance(h.distance())
                                                .build())
                        .toList();
        return RagSearchResponse.builder()
                .items(items)
                .build();
    }
}
