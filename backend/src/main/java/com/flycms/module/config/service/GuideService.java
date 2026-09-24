package com.flycms.module.config.service;

import com.flycms.core.utils.OrderbyUtils;
import com.flycms.core.utils.SnowFlake;
import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.dao.GuideDao;
import com.flycms.module.config.model.Guide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:19 2018/9/18
 */
@Service
public class GuideService {
    @Autowired
    private GuideDao guideDao;

    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 添加导航信息
     *
     * @param guide
     * @return
     */
    @Transactional
    public DataVo addGuide(Guide guide) {
        DataVo data = DataVo.failure("操作失败");
        // 历史 Bug：fly_guide.id 为 NOT NULL 且无自增，不显式赋值会插入失败，
        // 这里统一用雪花 ID 补齐（与项目其余 bigint 主键风格一致）。
        if (guide.getId() == null || guide.getId() <= 0) {
            guide.setId(SnowFlake.getInstance().nextId());
        }

        int totalCount=guideDao.addGuide(guide);
        if(totalCount > 0){
            data = DataVo.success("添加导航添加成功");
        }else{
            data=DataVo.failure("添加导航添加失败！");
        }
        return data;
    }

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    /**
     * 删除导航
     */
    @Transactional
    public DataVo deleteGuide(Long id) {
        if (id == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = guideDao.deleteGuideById(id);
        return rows > 0 ? DataVo.success("删除成功") : DataVo.failure("导航不存在");
    }

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    /**
     * 更新导航
     */
    @Transactional
    public DataVo updateGuide(Guide guide) {
        if (guide == null || guide.getId() == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = guideDao.updateGuideById(guide);
        return rows > 0 ? DataVo.success("操作成功") : DataVo.failure("导航不存在或内容未变化");
    }

    /**
     * 单独切换显示状态
     */
    @Transactional
    public DataVo updateGuideStatus(Long id, Integer status) {
        if (id == null || status == null) {
            return DataVo.failure("参数不完整");
        }
        int rows = guideDao.updateGuideStatus(id, status);
        return rows > 0 ? DataVo.success("操作成功") : DataVo.failure("导航不存在");
    }

    /**
     * 新增或更新（后台保存统一入口）
     */
    @Transactional
    public DataVo saveGuide(Guide guide) {
        if (guide == null) {
            return DataVo.failure("参数不完整");
        }
        if (guide.getId() == null || guide.getId() <= 0 || guideDao.findGuideById(guide.getId()) == null) {
            return addGuide(guide);
        }
        return updateGuide(guide);
    }



    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    /**
     * 导行翻页查询
     *
     * @param pageNum
     * @param rows
     * @return
     * @throws Exception
     */
    public PageVo<Guide> getGuideListPage(String name, Integer status, String orderby, String order, int pageNum, int rows) {
        PageVo<Guide> pageVo = new PageVo<Guide>(pageNum);
        pageVo.setRows(rows);
        List<Guide> list = new ArrayList<Guide>();
        if(orderby==null){
            orderby="id";
            }
        orderby = OrderbyUtils.check(orderby, "id");
        if(order==null){
            order="desc";
            }
        order = OrderbyUtils.check(order, "desc");
        pageVo.setList(guideDao.getGuideList(name,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(guideDao.getGuideCount(name,status));
        return pageVo;
    }

    /**
     * 按 id 查询导航
     */
    public Guide findGuideById(Long id) {
        return id == null ? null : guideDao.findGuideById(id);
    }

    /**
     * 查询全部导航（后台列表/树用）。
     * 说明：存量 fly_guide 是平表（无 parent 字段），故本期以「排序后的扁平列表」对外，
     * 前端按 sort 渲染即可；如需真树形，先给表加父级列再扩展。
     */
    public List<Guide> getGuideAll(Integer status) {
        return guideDao.getGuideAll(status);
    }

}
