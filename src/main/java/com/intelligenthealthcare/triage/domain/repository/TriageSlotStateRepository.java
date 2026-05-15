package com.intelligenthealthcare.triage.domain.repository;

import com.intelligenthealthcare.triage.domain.model.TriageSlotState;
import java.util.Optional;

/**
 * 导诊槽位状态仓储接口（DDD 领域层端口）。
 */
public interface TriageSlotStateRepository {

    Optional<TriageSlotState> findBySessionId(String sessionId);

    void save(TriageSlotState slotState);

    void updateById(TriageSlotState slotState);
}
