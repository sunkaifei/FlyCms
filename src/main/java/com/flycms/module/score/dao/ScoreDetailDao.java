package com.flycms.module.score.dao;

import com.flycms.module.score.model.ScoreDetail;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 积分记录数据操作类
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
 * @version 1.0,2017年10月1日 <br/>
 * 
 */
public interface ScoreDetailDao {
	// ///////////////////////////////
	// /////     增加         ////////
	// ///////////////////////////////
    /**
     * 保存用户积分记录
     *
     * @param scoreDetail
     * @return
     */
    public int saveScoreDetail(ScoreDetail scoreDetail);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除本条积分记录
    public int deleteScoreDetailById(@Param("id") Long id);


	// ///////////////////////////////
	// /////        修改      ////////
	// ///////////////////////////////
	/**
	 * 修改奖励记录
	 *
	 * @param id
	 */
	public void scoreDetailByCancel(@Param("id") Long id);
	
	// ///////////////////////////////
	// /////       查询       ////////
	// ///////////////////////////////

    /**
     * 按id查询积分记录
     *
     * @param id
     * @return
     */
    public ScoreDetail findScoreDetailById(@Param("id") Long id);

	/**
	 * 按用户id查询积分记录数量
	 * 
	 * @param userId
	 * @return
	 */
	public int scoreDetailCount(@Param("userId") Long userId,
                                @Param("status") Integer status
    );

	/**
	 * 按用户id查询积分记录列表
	 * 
	 * @param userId
	 * @param offset
	 * @param rows
	 * @return
	 */
	public List<ScoreDetail> scoreDetaillist(@Param("userId") Long userId,
                                             @Param("status") Integer status,
                                             @Param("orderby") String orderby,
                                             @Param("order") String order,
                                             @Param("offset") Integer offset,
                                             @Param("rows") Integer rows);

    /**
     * 是否能奖励，如果返回记录为0，表示可以奖励
     * 
	 * @param userId
	 * @param scoreRuleId
	 * @param type
	 * @return
	 */
	public List<ScoreDetail> scoreDetailCanBonus(@Param("userId") Long userId, @Param("scoreRuleId") Long scoreRuleId, @Param("type") String type);

    /**
     * 根据会员、获取奖励的外键、奖励规则ID获取奖励激励，不包括foreign_id=0
     * 
	 * @param userId
	 * @param scoreRuleId
	 * @param foreignId
	 * @return
	 */
	public ScoreDetail findByForeignAndRule(@Param("userId") Long userId,
                                            @Param("scoreRuleId") Long scoreRuleId,
                                            @Param("foreignId") Long foreignId);
	
}