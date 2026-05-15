package com.intelligenthealthcare.triage.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.intelligenthealthcare.triage.domain.model.TriageSlotState;
import com.intelligenthealthcare.triage.domain.repository.TriageSlotStateRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MybatisTriageSlotStateRepository implements TriageSlotStateRepository {

    private final TriageSlotStateMapper triageSlotStateMapper;

    @Override
    public Optional<TriageSlotState> findBySessionId(String sessionId) {
        LambdaQueryWrapper<TriageSlotState> query = new LambdaQueryWrapper<>();
        query.eq(TriageSlotState::getSessionId, sessionId);
        return Optional.ofNullable(triageSlotStateMapper.selectOne(query));
    }

    @Override
    public void save(TriageSlotState slotState) {
        triageSlotStateMapper.insert(slotState);
    }

    @Override
    public void updateById(TriageSlotState slotState) {
        triageSlotStateMapper.updateById(slotState);
    }
}
