package com.flycms.module.score.service;

import com.flycms.core.utils.DateUtils;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.score.dao.ScoreDetailDao;
import com.flycms.module.score.dao.ScoreRuleDao;
import com.flycms.module.score.model.ScoreDetail;
import com.flycms.module.score.model.ScoreRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;

/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 积分模块---积分规则服务类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年10月15日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0<br/>
 * 
 */
@Service
public class ScoreRuleService {
	@Autowired
    private ScoreRuleDao scoreRuleDao;
    @Autowired
    private ScoreDetailService scoreDetailService;
    @Autowired
    private ScoreDetailDao scoreDetailDao;


    // ///////////////////////////////
    // /////     增加         ////////
    // ///////////////////////////////
	/**
	 * 保存积分规则
	 * 
	 * @param scoreRule
	 * @return
	 */
    @Transactional
	public DataVo addScoreRule(ScoreRule scoreRule) {
        DataVo data = DataVo.failure("操作失败");
        SnowFlake snowFlake = new SnowFlake(2, 3);
        scoreRule.setId(snowFlake.nextId());
        scoreRule.setCreateTime(new Date());
        scoreRule.setUpdateTime(new Date());
    	int totalCount=scoreRuleDao.saveScoreRule(scoreRule);
        if(totalCount > 0){
            data = DataVo.success("积分规则添加成功");
        }else{
            data=DataVo.failure("积分规则添加失败！");
        }
        return data;
    }

    //删除积分规则
    @Transactional
    public DataVo deleteScoreRuleById(Integer id){
        DataVo data = DataVo.failure("操作失败");
        int totalCount=scoreRuleDao.deleteScoreRuleById(id);
        if(totalCount > 0){
            data = DataVo.success("删除成功");
        }else{
            data=DataVo.failure("删除失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 修改积分规则
     *
     * @param scoreRule
     * @return
     */
    public DataVo updateScoreRule(ScoreRule scoreRule) {
        DataVo data = DataVo.failure("操作失败");
        if(scoreRule.getId()==null){
            data=DataVo.failure("规则id不存在或者错误");
        }
        ScoreRule rule=scoreRuleDao.findScoreRuleById(scoreRule.getId(),0);
        if(rule==null){
            data=DataVo.failure("修改规则信息不存在！");
        }
        scoreRule.setUpdateTime(new Date());
        int totalCount = scoreRuleDao.updateScoreRule(scoreRule);
        if(totalCount > 0){
            data = DataVo.success("更新规则成功");
        }else{
            data=DataVo.failure("更新规则失败！");
        }
        return data;

    }

    //开启规则和关闭操作
    public DataVo updateRuleStatus(Long id) {
        DataVo data = DataVo.failure("操作失败");
        ScoreRule rule=scoreRuleDao.findScoreRuleById(id,0);
        if(rule!=null){
            if(rule.getStatus()==1){
                data = DataVo.success("关闭");
            }else{
                data = DataVo.success("开启");
            }
            scoreRuleDao.updateScoreRuleEnabled(id);
        }else{
            data=DataVo.failure("该规则可能不存在！");
        }
        return data;
    }
    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////

    /**
     * 查询所有规则
     * 
     * @return
     */
    public List<ScoreRule> allScoreRuleList() {
        return scoreRuleDao.allScoreRuleList();
    }

    /**
     * 根据规则id查找和审核状态查询
     * 
     * @param id
     * @return
     */
    public ScoreRule findScoreRuleById(Long id,Integer status) {
        return scoreRuleDao.findScoreRuleById(id,status);
    }




    /**
     * 根据积分规则奖励
     * @param userId
     * @param scoreRuleId
     */
    public void scoreRuleBonus(Long userId, Long scoreRuleId) {
        this.scoreRuleBonus(userId,scoreRuleId,null);
    }

    /**
     * 根据积分规则奖励
     * @param userId
     *        用户id
     * @param scoreRuleId
     *        积分规则id
     * @param foreignId
     *        奖励的信息编号id
     */
    public void scoreRuleBonus(Long userId, Long scoreRuleId, Long foreignId) {
        //规则开启状态下才执行奖励
    	ScoreRule scoreRule = this.findScoreRuleById(scoreRuleId,2);
        if(scoreRule != null){
        	if(scoreRule.getScore() != 0){
                String type = scoreRule.getType();
                boolean canBonus = true;
                ScoreDetail scoreDetail = new ScoreDetail();
                scoreDetail.setType(type);
                scoreDetail.setUserId(userId);
                scoreDetail.setForeignId(foreignId);;
                scoreDetail.setScore(scoreRule.getScore());
                scoreDetail.setRemark(scoreRule.getName());
                scoreDetail.setScoreRuleId(scoreRuleId);
                //unlimite为不限制奖励次数
                if(!"unlimite".equals(type)){
                	canBonus = scoreDetailService.scoreDetailCanBonus(userId, scoreRuleId, type);
                    if(canBonus){
                        //每个会员、每个奖励规则、每个外键（不包含0）只能奖励一次
                        if(scoreDetailService.findByForeignAndRule(userId, scoreRuleId, foreignId) == null){
                            scoreDetailService.saveScoreDetail(scoreDetail,"plus");
                        }
                    }
                }else{
                    scoreDetailService.saveScoreDetail(scoreDetail,"plus");
                }

            }
        }
    }

    public void scoreRuleCancelBonus(Long userId, Long scoreRuleId, Long foreignId) {
        ScoreDetail scoreDetail = scoreDetailService.findByForeignAndRule(userId, scoreRuleId, foreignId);
        if(scoreDetail != null){
            scoreDetailService.scoreDetailByCancel(scoreDetail.getId());
            //扣除积分
           // userDao.updateScore("reduce",scoreDetail.getScore(), userId);
            scoreDetail.setType(scoreDetail.getType());
            scoreDetail.setUserId(userId);
            scoreDetail.setForeignId(foreignId);;
            scoreDetail.setScore(-scoreDetail.getScore());
            scoreDetail.setRemark("撤销积分奖励");
            scoreDetail.setScoreRuleId(scoreRuleId);
            scoreDetailService.saveScoreDetail(scoreDetail,"reduce");
        }
    }

    /**
     *
     *
     * @param name
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<ScoreRule> scoreRulelistPage(String name, String createTime, Integer status, String orderby, String order, int pageNum, int rows) {
        PageVo<ScoreRule> pageVo = new PageVo<ScoreRule>(pageNum);

        pageVo.setRows(rows);
        if(orderby==null){
            orderby="id";
        }
        if(order==null){
            order="asc";
        }
        Date addtime=null;
        if(createTime!=null){
            addtime=DateUtils.fomatDate(createTime);
        }
        pageVo.setCount(scoreRuleDao.getScoreRuleCount(name, addtime, status));
        List<ScoreRule> userlist = scoreRuleDao.getScoreRuleList(name, addtime, status, orderby, order, pageVo.getOffset(), pageVo.getRows());
        pageVo.setList(userlist);
        return pageVo;
    }
}
