package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.triage.domain.model.TriageSession;
import com.intelligenthealthcare.triage.domain.repository.TriageSessionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisTriageSessionRepository implements TriageSessionRepository {

    private final TriageSessionMapper triageSessionMapper;

    @Override
    public Optional<TriageSession> findByUserIdAndSessionId(String userId, String sessionId) {
        LambdaQueryWrapper<TriageSession> query = new LambdaQueryWrapper<>();
        query.eq(TriageSession::getUserId, userId);
        query.eq(TriageSession::getSessionId, sessionId);
        return Optional.ofNullable(triageSessionMapper.selectOne(query));
    }

    @Override
    public Optional<TriageSession> findBySessionId(String sessionId) {
        LambdaQueryWrapper<TriageSession> query = new LambdaQueryWrapper<>();
        query.eq(TriageSession::getSessionId, sessionId);
        return Optional.ofNullable(triageSessionMapper.selectOne(query));
    }

    @Override
    public List<TriageSession> findByUserId(String userId, int limit) {
        LambdaQueryWrapper<TriageSession> query = new LambdaQueryWrapper<>();
        query.eq(TriageSession::getUserId, userId);
        query.orderByDesc(TriageSession::getUpdateTime);
        query.last("LIMIT " + limit);
        return triageSessionMapper.selectList(query);
    }

    @Override
    public void save(TriageSession session) {
        triageSessionMapper.insert(session);
    }

    @Override
    public void updateById(TriageSession session) {
        triageSessionMapper.updateById(session);
    }
}
