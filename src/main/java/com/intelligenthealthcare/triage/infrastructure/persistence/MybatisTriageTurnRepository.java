package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.triage.domain.model.TriageTurn;
import com.intelligenthealthcare.triage.domain.repository.TriageTurnRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisTriageTurnRepository implements TriageTurnRepository {

    private final TriageTurnMapper triageTurnMapper;

    @Override
    public List<TriageTurn> findRecentBySessionId(String sessionId, int limit) {
        LambdaQueryWrapper<TriageTurn> query = new LambdaQueryWrapper<>();
        query.eq(TriageTurn::getSessionId, sessionId);
        query.orderByDesc(TriageTurn::getTurnNo);
        query.last("LIMIT " + limit);
        List<TriageTurn> desc = triageTurnMapper.selectList(query);
        if (desc.isEmpty()) {
            return Collections.emptyList();
        }
        List<TriageTurn> asc = new ArrayList<>(desc.size());
        for (int i = desc.size() - 1; i >= 0; i--) {
            asc.add(desc.get(i));
        }
        return asc;
    }

    @Override
    public List<TriageTurn> findAllBySessionId(String sessionId) {
        LambdaQueryWrapper<TriageTurn> query = new LambdaQueryWrapper<>();
        query.eq(TriageTurn::getSessionId, sessionId);
        query.orderByAsc(TriageTurn::getTurnNo);
        return triageTurnMapper.selectList(query);
    }

    @Override
    public void save(TriageTurn turn) {
        triageTurnMapper.insert(turn);
    }
}
