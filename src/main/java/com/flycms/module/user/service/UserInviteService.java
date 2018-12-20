package com.flycms.module.user.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.user.dao.UserInviteDao;
import com.flycms.module.user.model.UserInvite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 12:14 2018/7/9
 */
@Service
public class UserInviteService {
    @Autowired
    private UserInviteDao userInviteDao;
    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////

    @Transactional
    public DataVo addUserInvite(Long toUserId,Long formUserId){
        DataVo data = DataVo.failure("操作失败");
        if(this.checkUserInvite(toUserId,formUserId)){
            return data = DataVo.failure("该用户已被邀请");
        }
        UserInvite invite=new UserInvite();
        invite.setToUserId(toUserId);
        invite.setFormUserId(formUserId);
        invite.setStatus(1);
        invite.setCreateTime(new Date());
        int total = userInviteDao.addUserInvite(invite);
        if(total > 0){
            data = DataVo.success("邀请信息已被记录");
        }else{
            data=DataVo.failure("邀请信息记录失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        删除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////       修改       ////////
    // ///////////////////////////////


    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    /**
     * 查询当前用户id对邀请用户id是否存在
     *
     * @param toUserId
     *         被邀请人ID
     * @param formUserId
     *         邀请人ID
     * @return
     */
    public boolean checkUserInvite(Long toUserId,Long formUserId){
        int totalCount = userInviteDao.checkUserInvite(toUserId,formUserId);
        return totalCount > 0 ? true : false;
    }

    /**
     * 用户邀请翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<UserInvite> getUserInviteListPage(Long userId,Integer status,String orderby,String order,int pageNum, int rows) {
        PageVo<UserInvite> pageVo = new PageVo<UserInvite>(pageNum);
        pageVo.setRows(rows);
        if(orderby==null){
            orderby="create_time";
        }
        if(order==null){
            order="desc";
        }
        pageVo.setList(userInviteDao.getUserInviteList(userId, status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(userInviteDao.getUserInviteCount(userId, status));
        return pageVo;
    }
}
