package com.intelligenthealthcare.audit.infrastructure.persistence;

import com.intelligenthealthcare.audit.domain.model.AiRecallAuditLog;
import com.intelligenthealthcare.audit.domain.repository.AiRecallAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisAiRecallAuditLogRepository implements AiRecallAuditLogRepository {

    private final AiRecallAuditLogMapper aiRecallAuditLogMapper;

    @Override
    public void save(AiRecallAuditLog entity) {
        aiRecallAuditLogMapper.insert(entity);
    }
}
