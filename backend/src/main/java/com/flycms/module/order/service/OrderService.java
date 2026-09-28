package com.flycms.module.order.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.order.dao.OrderDao;
import com.flycms.module.order.model.Order;
import com.flycms.module.score.model.ScoreDetail;
import com.flycms.module.score.service.ScoreDetailService;
import com.flycms.module.user.model.UserAccount;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.util.Date;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 14:52 2018/9/11
 */
@Service
public class OrderService {
    @Autowired
    private OrderDao orderDao;
    @Autowired
    protected UserService userService;
    @Autowired
    protected ScoreDetailService scoreDetailService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////



}
