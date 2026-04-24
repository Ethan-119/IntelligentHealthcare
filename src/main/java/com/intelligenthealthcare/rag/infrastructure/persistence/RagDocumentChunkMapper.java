package com.intelligenthealthcare.rag.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.intelligenthealthcare.rag.domain.model.RagDocumentChunk;
import com.intelligenthealthcare.rag.domain.model.RagSourceType;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagDocumentChunkMapper extends BaseMapper<RagDocumentChunk> {

    default Optional<RagDocumentChunk> findBySourceTypeAndSourceIdAndChunkKey(
            RagSourceType sourceType, String sourceId, String chunkKey) {
        return Optional.ofNullable(
                selectOne(
                        new LambdaQueryWrapper<RagDocumentChunk>()
                                .eq(RagDocumentChunk::getSourceType, sourceType)
                                .eq(RagDocumentChunk::getSourceId, sourceId)
                                .eq(RagDocumentChunk::getChunkKey, chunkKey)));
    }
}
