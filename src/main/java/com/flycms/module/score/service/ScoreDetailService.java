package com.flycms.module.score.service;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.score.dao.ScoreDetailDao;
import com.flycms.module.score.model.ScoreDetail;
import com.flycms.module.user.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 积分模块---积分记录服务类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年5月25日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0,2017年7月25日 <br/>
 * 
 */
@Service
public class ScoreDetailService{
    @Resource
    private ScoreDetailDao scoreDetailDao;
    @Autowired
    private UserDao userDao;
    // ///////////////////////////////
    // /////     增加         ////////
    // ///////////////////////////////
    /**
     * 保存用户积分记录
     *
     * @param scoreDetail
     *        积分记录实体
     * @param calculate
     *        运算：plus是加+,reduce是减-
     * @return
     */
    public int saveScoreDetail(ScoreDetail scoreDetail,String calculate) {
        //更新用户积分
        userDao.updateUserAccountScore(calculate,scoreDetail.getScore(), scoreDetail.getUserId());
        SnowFlake snowFlake = new SnowFlake(2, 3);
        scoreDetail.setId(snowFlake.nextId());
        scoreDetail.setCreateTime(new Date());
        return scoreDetailDao.saveScoreDetail(scoreDetail);
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 修改奖励记录
     *
     * @param id
     *
     */
    public void scoreDetailByCancel(Long id) {
        scoreDetailDao.scoreDetailByCancel(id);
    }


    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    /**
     * 是否能奖励，true表示可以奖励
     * @param userId
     * @param scoreRuleId
     * @param type
     * @return
     */
    public boolean scoreDetailCanBonus(Long userId, Long scoreRuleId, String type) {
        List<ScoreDetail> list = scoreDetailDao.scoreDetailCanBonus(userId,scoreRuleId,type);
        return list.size() == 0;
    }

    /**
     * 根据会员、获取奖励的外键、奖励规则ID获取奖励激励，不包括foreign_id=0
     * @param userId
     * @param scoreRuleId
     * @param forgignId
     * @return
     */
    public ScoreDetail findByForeignAndRule(Long userId, Long scoreRuleId, Long forgignId) {
        return scoreDetailDao.findByForeignAndRule(userId,scoreRuleId,forgignId);
    }
    
	/**
	 * 
	 * 
	 * @param userId
	 * @param pageNum
	 * @param rows
	 * @return
	 * @throws Exception
	 */
	public PageVo<ScoreDetail> scoreDetaillistPage(Long userId,Integer status, String orderby, String order, int pageNum, int rows) {
		PageVo<ScoreDetail> pageVo = new PageVo<ScoreDetail>(pageNum);
		pageVo.setRows(rows);
        pageVo.setRows(rows);
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="desc";
        }
		pageVo.setCount(scoreDetailDao.scoreDetailCount(userId,status));
		
		List<ScoreDetail> detaillist = scoreDetailDao.scoreDetaillist(userId,status,orderby,order, pageVo.getOffset(), pageVo.getRows());
		pageVo.setList(detaillist);
		return pageVo;
	}
}
