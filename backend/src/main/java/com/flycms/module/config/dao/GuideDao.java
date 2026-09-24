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
    // ///////////////////////////////
    // /////  导航管理（阶段 B3） /////
    // ///////////////////////////////

    /** 按 id 查询导航 */
    public Guide findGuideById(@Param("id") Long id);

    /** 更新导航 */
    public int updateGuideById(Guide guide);

    /** 删除导航 */
    public int deleteGuideById(@Param("id") Long id);

    /** 单独更新显示状态 */
    public int updateGuideStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 查询全部导航（后台树/列表用，不分页） */
    public List<Guide> getGuideAll(@Param("status") Integer status);

    public List<Guide> getGuideList(@Param("name") String name,
                                          @Param("status") Integer status,
                                          @Param("orderby") String orderby,
                                          @Param("order") String order,
                                          @Param("offset") Integer offset,
                                          @Param("rows") Integer rows);
}
