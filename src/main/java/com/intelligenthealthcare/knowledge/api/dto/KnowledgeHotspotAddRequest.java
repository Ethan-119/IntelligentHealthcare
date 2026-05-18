package com.intelligenthealthcare.knowledge.api.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class KnowledgeHotspotAddRequest {

    @NotEmpty(message = "请至少选择一个热点范围")
    private List<String> scopes;
}
