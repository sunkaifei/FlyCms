package com.flycms.module.config.service;

import com.flycms.module.config.dao.AreasDao;
import com.flycms.module.config.model.Areas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:50 2018/7/7
 */
@Service
public class AreasService {

    @Autowired
    private AreasDao dao;
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
    // /////        查询      ////////
    // ///////////////////////////////

    /**按id查询省市区是否存在,0为true
     *
     * @param areaId
     *        区域id
     * @return
     */
    @Cacheable(value = "areas")
    public Areas findAreasByid(int areaId) {
        return dao.findAreasByid(areaId);
    }

    @Cacheable(value = "areas")
    public List<Areas> selectAreasByPid(int parentId){
        return dao.selectAreasByPid(parentId);
    };
}
