package com.intelligenthealthcare.triage.domain.repository;

import com.intelligenthealthcare.triage.domain.model.TriageSession;
import java.util.List;
import java.util.Optional;

/**
 * 导诊会话仓储接口（DDD 领域层端口）。
 * 基础设施层通过 {@code MybatisTriageSessionRepository} 实现。
 */
public interface TriageSessionRepository {

    Optional<TriageSession> findByUserIdAndSessionId(String userId, String sessionId);

    Optional<TriageSession> findBySessionId(String sessionId);

    List<TriageSession> findByUserId(String userId, int limit);

    void save(TriageSession session);

    void updateById(TriageSession session);
}
