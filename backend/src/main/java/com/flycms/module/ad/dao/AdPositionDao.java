package com.flycms.module.ad.dao;

import com.flycms.module.ad.model.AdPosition;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 广告位 DAO。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AdPositionDao {

    List<AdPosition> findPositions(@Param("offset") int offset, @Param("rows") int rows,
                                   @Param("keyword") String keyword);

    int countPositions(@Param("keyword") String keyword);

    AdPosition findPositionById(@Param("id") Long id);

    AdPosition findPositionByKey(@Param("adKey") String adKey);

    int checkAdKey(@Param("adKey") String adKey, @Param("excludeId") Long excludeId);

    int addPosition(AdPosition position);

    int updatePosition(AdPosition position);

    int deletePositionById(@Param("id") Long id);

    /** 位下广告数（删除前检查） */
    int countAdsByPosition(@Param("positionId") Long positionId);
}
