package com.flycms.module.guide.dao;

import com.flycms.module.guide.model.Guide;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 站点导航 DAO（fly_guide）
 *
 * @author sun-kaifei
 * @version 1.0
 */
@Repository
public interface GuideDao {

    void insert(Guide guide);

    int update(Guide guide);

    int deleteById(@Param("id") Long id);

    Guide findById(@Param("id") Long id);

    /** 全部导航项（father_id asc, sort asc, id asc），树在 Service 构建 */
    List<Guide> findAll();

    List<Guide> findPage(@Param("offset") int offset, @Param("rows") int rows);

    int countAll();

    int countChildren(@Param("fatherId") Long fatherId);

    int updateStatus(@Param("id") Long id, @Param("status") int status);
}
