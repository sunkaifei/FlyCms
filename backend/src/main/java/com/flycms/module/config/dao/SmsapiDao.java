package com.flycms.module.config.dao;

import com.flycms.module.config.model.Smsapi;
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
 * @Date: 14:15 2018/7/19
 */
@Repository
public interface SmsapiDao {

    // ///////////////////////////////
    // /////      增加        ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //按id更新手机短信API接口信息
    public int updagteSmsapiById(Smsapi smsapi);

    // ///////////////////////////////
    // /////       查询       ////////
    // ///////////////////////////////
    /**
     * 按id查询手机短信API接口信息
     *
     * @param id
     *         短信通道API接口配置id
     * @return
     */
    public Smsapi findSmsapiByid(@Param("id") Integer id);

    //查询模型总数量
    public int getSmsapiCount();

    //查询模型翻页列表
    public List<Smsapi> getSmsapiList(@Param("offset") int offset, @Param("rows") int rows);
}
