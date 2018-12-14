package com.flycms.module.other.dao;

import com.flycms.module.other.model.Email;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 10:08 2018/7/7
 */
@Repository
public interface EmailDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //修改邮件模板
    public int updateEmailTempletsById(Email email);


    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询配置信息
    public Email findEmailTempletById(@Param("id") Integer id);

    //按模板标记码查询配置信息
    public Email findEmailTempletByTpCode(@Param("tpCode") String tpCode);

    public int getEmailTempletCount();

    public List<Email> getEmailTempletList(@Param("offset")int offset, @Param("rows") int rows);
}
