package com.flycms.module.order.service;

import com.flycms.core.entity.DataVo;
import com.flycms.module.order.dao.OrderDao;
import com.flycms.module.order.model.Order;
import com.flycms.module.score.model.ScoreDetail;
import com.flycms.module.score.service.ScoreDetailService;
import com.flycms.module.share.model.Share;
import com.flycms.module.share.service.ShareService;
import com.flycms.module.user.model.UserAccount;
import com.flycms.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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
    private ShareService shareService;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    protected UserService userService;
    @Autowired
    protected ScoreDetailService scoreDetailService;
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    //添加分享订单信息
    @Transactional
    public DataVo addSharOrdere(Long shareId, Long userId){
        DataVo data = DataVo.failure("操作失败");
        Share share=shareService.findShareById(shareId,2);
        if(share == null){
            return data = DataVo.failure("该分享未审核或者未审核");
        }
        UserAccount account=userService.findUserAccountById(userId);
        if(account.getScore() < share.getNeedmoney()){
            return data = DataVo.failure("账户积分不足，请充值或免费获取");
        }
        //积分操作记录
        ScoreDetail scoreDetail = new ScoreDetail();
        scoreDetail.setType("unlimite");
        scoreDetail.setUserId(userId);
        scoreDetail.setScore(-share.getNeedmoney());
        scoreDetail.setRemark("购买分享资源");
        //购买资源的id
        scoreDetail.setForeignId(shareId);
        scoreDetail.setCreateTime(new Date());
        scoreDetailService.saveScoreDetail(scoreDetail,"reduce");

        Order order=new Order();
        order.setUserId(userId);
        order.setShareId(shareId);
        order.setStatus(1);
        order.setCreateTime(new Date());
        System.out.println("====================================="+userId+"============="+shareId+"=============="+new Date());
        int totalCount=orderDao.addSharOrdere(order);
        if(totalCount > 0){
            data = DataVo.success("已购买成功");
        }else{
            data=DataVo.failure("购买失败");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////

    public boolean checkShareOrder(Integer shareId,Integer userId,String createTime) {
        int totalCount = orderDao.getShareOrderCount(shareId,userId,createTime);
        return totalCount > 0 ? true : false;
    }


}
