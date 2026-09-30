package com.flycms.module.ad.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 广告按日统计 DAO（fly_ad_stat_daily）。
 * 每次「展现」（标签渲染 / JS 分发）与「点击」（/ad/click/{id}）都按 (广告, 日期) upsert 累加。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AdStatDao {

    /** 当日展现 +1（不存在则建行） */
    int upsertView(@Param("id") Long id, @Param("adId") Long adId, @Param("positionId") Long positionId);

    /** 当日点击 +1（不存在则建行） */
    int upsertClick(@Param("id") Long id, @Param("adId") Long adId, @Param("positionId") Long positionId);

    /** 按日聚合：adId/positionId 可选过滤，返回 [{statDate, views, clicks}]（新→旧） */
    List<Map<String, Object>> listDaily(@Param("adId") Long adId,
                                        @Param("positionId") Long positionId,
                                        @Param("days") int days);
}
