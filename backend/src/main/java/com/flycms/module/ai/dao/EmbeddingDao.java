package com.flycms.module.ai.dao;

import com.flycms.module.ai.model.Embedding;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 内容向量 DAO（G21）。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface EmbeddingDao {

    int add(Embedding embedding);

    List<Embedding> findByModel(@Param("targetModel") String targetModel);

    List<Embedding> findByTarget(@Param("targetModel") String targetModel,
                                 @Param("targetId") Long targetId);

    int deleteByTarget(@Param("targetModel") String targetModel,
                       @Param("targetIds") List<Long> targetIds);
}
