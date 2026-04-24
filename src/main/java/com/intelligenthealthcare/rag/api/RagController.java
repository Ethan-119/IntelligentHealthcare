package com.intelligenthealthcare.rag.api;

import com.intelligenthealthcare.rag.api.dto.RagIngestRequest;
import com.intelligenthealthcare.rag.api.dto.RagIngestResponse;
import com.intelligenthealthcare.rag.api.dto.RagSearchRequest;
import com.intelligenthealthcare.rag.api.dto.RagSearchResponse;
import com.intelligenthealthcare.rag.application.RagIngestCommand;
import com.intelligenthealthcare.rag.application.RagIngestionService;
import com.intelligenthealthcare.rag.application.RagQueryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagIngestionService ingestionService;
    private final RagQueryService queryService;

    public RagController(RagIngestionService ingestionService, RagQueryService queryService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
    }

    @PostMapping("/ingest")
    public RagIngestResponse ingest(@Valid @RequestBody RagIngestRequest request) {
        var cmd =
                new RagIngestCommand(
                        request.getSourceType(),
                        request.getSourceId(),
                        request.getChunkKey(),
                        request.getContent());
        return new RagIngestResponse(ingestionService.upsert(cmd));
    }

    @PostMapping("/search")
    public RagSearchResponse search(@Valid @RequestBody RagSearchRequest request) {
        var hits = queryService.search(request.getQuery(), request.getTopK());
        var items =
                hits.stream()
                        .map(
                                h ->
                                        new RagSearchResponse.RagSearchItem(
                                                h.id(),
                                                h.sourceType(),
                                                h.sourceId(),
                                                h.chunkKey(),
                                                h.content(),
                                                h.distance()))
                        .toList();
        return new RagSearchResponse(items);
    }
}
