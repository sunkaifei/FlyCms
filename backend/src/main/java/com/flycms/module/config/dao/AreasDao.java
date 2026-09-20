package com.flycms.module.config.dao;

import com.flycms.module.config.model.Areas;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 开发公司：28844.com<br/>
 * 版权：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 23:10 2018/7/6
 */
@Repository
public interface AreasDao {
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
    /**
     * 按id查询地区信息是否存在
     *
     * @param areaId
     * @return
     */
    public Areas findAreasByid(@Param("areaId") int areaId);

    /**
     * 按父级id查询所有地区信息
     *
     * @param parentId
     * @return
     */
    public List<Areas> selectAreasByPid(@Param("parentId") int parentId);
}
