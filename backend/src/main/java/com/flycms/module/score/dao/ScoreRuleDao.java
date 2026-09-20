package com.flycms.module.score.dao;

import java.util.Date;
import java.util.List;

import com.flycms.module.score.model.ScoreRule;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;



/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 积分规则数据操作类
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
@Repository
public interface ScoreRuleDao {
	// ///////////////////////////////
	// /////     增加         ////////
	// ///////////////////////////////
	/** 
	 * 保存积分规则
	 * 
	 * @param scoreRule
	 * @return
	 */
	public int saveScoreRule(ScoreRule scoreRule);

	// ///////////////////////////////
	// /////        刪除      ////////
	// ///////////////////////////////
    //按id删除本条积分规则
	public int deleteScoreRuleById(@Param("id") int id);


	// ///////////////////////////////
	// /////        修改      ////////
	// ///////////////////////////////
	
	/**
	 * 修改积分规则
	 * 
	 * @param scoreRule
	 * @return
	 */
	public int updateScoreRule(ScoreRule scoreRule);

	/**
	 * 按id查询规则
	 *
	 * @param id
	 * @return
	 */
	public int updateScoreRuleEnabled(@Param("id") Long id);


	// ///////////////////////////////
	// /////        查詢      ////////
	// ///////////////////////////////
	/**
	 * 根据规则名称查找
	 * 
	 * @param name
	 * @return
	 */
	public ScoreRule findScoreRuleByName(@Param("name") Integer name);
	
	/**
	 * 根据规则id查找
	 * 
	 * @param id
	 * @return
	 */
	public ScoreRule findScoreRuleById(@Param("id") Long id,@Param("status") Integer status);
	
	
	/**
	 * 所有规则列表
	 * 
	 * @return
	 */
	public List<ScoreRule> allScoreRuleList();

	//查询积分规则总数
	public int getScoreRuleCount(@Param("name") String name,
							@Param("createTime") Date createTime,
							@Param("status") Integer status);

	//查询积分规则列表
	public List<ScoreRule> getScoreRuleList(@Param("name") String name,
											@Param("createTime") Date createTime,
											@Param("status") Integer status,
											@Param("orderby") String orderby,
											@Param("order") String order,
											@Param("offset") int offset,
											@Param("rows") int rows);

}
