package com.intelligenthealthcare.audit.domain.repository;

import com.intelligenthealthcare.audit.domain.model.AiRecallAuditLog;

public interface AiRecallAuditLogRepository {

    void save(AiRecallAuditLog entity);
}
