package com.flycms.module.ad.dao;

import com.flycms.module.ad.model.Ad;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 广告 DAO。
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface AdDao {

    List<Ad> findAds(@Param("positionId") Long positionId, @Param("keyword") String keyword,
                     @Param("offset") int offset, @Param("rows") int rows);

    int countAds(@Param("positionId") Long positionId, @Param("keyword") String keyword);

    Ad findAdById(@Param("id") Long id);

    int addAd(Ad ad);

    int updateAd(Ad ad);

    int deleteAdById(@Param("id") Long id);

    /** 前台投放：启用的、在时间窗内的广告（weight desc, sort asc） */
    List<Ad> listActiveByPosition(@Param("positionId") Long positionId, @Param("rows") int rows);

    /** 展示计数 +1 */
    int incrementView(@Param("ids") List<Long> ids);

    /** 点击计数 +1 */
    int incrementClick(@Param("id") Long id);
}
