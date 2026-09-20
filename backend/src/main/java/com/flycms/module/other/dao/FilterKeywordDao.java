package com.flycms.module.other.dao;

import com.flycms.module.other.model.FilterKeyword;
import org.apache.ibatis.annotations.Param;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Repository
public interface FilterKeywordDao {
    // ///////////////////////////////
    // /////       增加       ////////
    // ///////////////////////////////
    /**
     * 添加违禁关键词
     *
     * @param keyword
     *         关键词
     * @return
     */
    public int addFilterKeyword(FilterKeyword keyword);
    // ///////////////////////////////
    // /////        刪除      ////////
    // ///////////////////////////////
    //按id删除违禁关键词
    public int deleteFilterKeywordById(@Param("id") Long id);

    // ///////////////////////////////
    // /////        修改      ////////
    // ///////////////////////////////
    //按id查询并更新违禁关键词信息
    public int updateFilterKeywordById(@Param("keyword") String keyword,@Param("id") Long id);

    // ///////////////////////////////
    // /////        查詢      ////////
    // ///////////////////////////////
    //按id查询违禁关键词信息
    public FilterKeyword findFilterKeywordById(@Param("id") Long id);

    /**
     * 查询违禁关键词是否存在
     *
     * @param keyword
     *         关键词
     * @return
     */
    public int checkFilterKeyword(@Param("keyword") String keyword);

    /**
     * 查询当前id以外该违禁关键词是否存在
     *
     * @param keyword
     *         关键词
     * @return
     */
    public int checkFilterKeywordNotId(@Param("keyword") String keyword,@Param("id") Long id);

    /**
     *  所有违禁关键词列表
     *
     * @return
     */
    public List<String> getFilterKeywordAllList();

    //违禁关键词总数
    public int getFilterKeywordCount();

    //违禁关键词列表
    public List<FilterKeyword> getFilterKeywordList(@Param("offset") Integer offset, @Param("rows") Integer rows);
}
