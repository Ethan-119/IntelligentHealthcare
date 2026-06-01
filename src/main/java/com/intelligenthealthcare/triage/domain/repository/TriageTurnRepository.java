package com.intelligenthealthcare.triage.domain.repository;

import com.intelligenthealthcare.triage.domain.model.TriageTurn;
import java.util.List;

/**
 * 导诊对话轮次仓储接口（DDD 领域层端口）。
 */
public interface TriageTurnRepository {

    /** 查询某会话最近 N 轮对话（按 turn_no 升序） */
    List<TriageTurn> findRecentBySessionId(String sessionId, int limit);

    /** 查询某会话全部轮次（按 turn_no 升序） */
    List<TriageTurn> findAllBySessionId(String sessionId);

    void save(TriageTurn turn);

    void deleteBySessionId(String sessionId);
}
