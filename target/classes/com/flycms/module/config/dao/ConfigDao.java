package com.flycms.module.config.dao;

import com.flycms.module.config.model.Guide;
import com.flycms.module.config.model.Config;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigDao {

    // ///////////////////////////////
    // ///// 增加 ////////
    // ///////////////////////////////
    //添加配置信息
    public int addConfig(Config config);

    //添加设置导航信息
    public int addGuide(Guide guide);
    // ///////////////////////////////
    // ///// 刪除 ////////
    // ///////////////////////////////
    /**
     * 删除配置
     *
     * return Integer
     */
    public int deleteConfig(@Param("keycode") String keycode);

    // ///////////////////////////////
    // ///// 修改 ////////
    // ///////////////////////////////
    public int updagteConfigByKey(Config config);

    // ///////////////////////////////
    // ///// 查詢 ////////
    // ///////////////////////////////

    //按key查询配置信息
    public Config getConfigByKey(@Param("keycode") String keycode);



    public int getConfigCount();

    public List<Config> getConfigList(@Param("offset")int offset, @Param("rows") int rows);

    //所有配置列表信息
    public List<Config> getConfigAllList();
}
