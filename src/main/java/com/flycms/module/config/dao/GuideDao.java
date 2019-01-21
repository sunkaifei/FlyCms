package com.flycms.module.config.dao;

import com.flycms.module.config.model.Guide;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:08 2018/7/7
 */
@Repository
public interface GuideDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加导航信息
    public int addGuide(Guide guide);

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////

    //查询所有导航数量
    public int getGuideCount(@Param("name") String name,
                                @Param("status") Integer status);

    //导航列表
    public List<Guide> getGuideList(@Param("name") String name,
                                          @Param("status") Integer status,
                                          @Param("orderby") String orderby,
                                          @Param("order") String order,
                                          @Param("offset") Integer offset,
                                          @Param("rows") Integer rows);
}
