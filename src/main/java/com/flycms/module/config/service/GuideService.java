package com.flycms.module.config.service;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.module.config.dao.GuideDao;
import com.flycms.module.config.model.Guide;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////



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
        if(order==null){
            order="desc";
        }
        pageVo.setList(guideDao.getGuideList(name,status,orderby,order,pageVo.getOffset(), pageVo.getRows()));
        pageVo.setCount(guideDao.getGuideCount(name,status));
        return pageVo;
    }

}
