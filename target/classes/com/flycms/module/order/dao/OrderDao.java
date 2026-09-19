package com.flycms.module.order.dao;

import com.flycms.module.order.model.Order;
import com.flycms.module.share.model.Share;
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
public interface OrderDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加分享订单信息
    public int addSharOrdere(Order order);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////
    public Order findOrderByid(@Param("id") int id);

    //分享总数
    public int getShareOrderCount(@Param("shareId") Integer shareId,
                                  @Param("userId") Integer userId,
                                  @Param("createTime") String createTime);

    //分享列表
    public List<Share> getShareOrder(@Param("shareId") Integer shareId,
                                     @Param("userId") Integer userId,
                                     @Param("createTime") String createTime,
                                     @Param("orderby") String orderby,
                                     @Param("order") String order,
                                     @Param("offset") Integer offset,
                                     @Param("rows") Integer rows);
}
