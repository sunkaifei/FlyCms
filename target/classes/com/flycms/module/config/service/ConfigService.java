/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */

package com.flycms.module.config.service;

import java.util.ArrayList;
import java.util.List;

import com.flycms.core.entity.PageVo;
import com.flycms.module.config.dao.ConfigDao;
import com.flycms.module.config.model.Guide;
import com.flycms.module.config.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;



/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 用户服务类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年11月12日 　用户模块所有服务类操作函数<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email 79678111@qq.com
 * @version 1.0,2017年11月12日 <br/>
 * 
 */
@Service
public class ConfigService {

	@Autowired
	private ConfigDao configDao;

	// ///////////////////////////////
	// /////       增加       ////////
	// ///////////////////////////////

	/**
	 * 增加配置
	 * 
	 * @param config
	 * @return Config
	 */
    @CacheEvict(value = "config", allEntries = true)
	public void addConfig(Config config) {
		configDao.addConfig(config);
	}

	//添加设置导航信息
    @CacheEvict(value = "config", allEntries = true)
	public void addGuide(String name,String link,Integer sort) {
		Guide guide=new Guide();
		guide.setName(name);
		guide.setLink(link);
		guide.setSort(sort);
		configDao.addGuide(guide);
	}
	// ///////////////////////////////
	// ///// 刪除 ////////
	// ///////////////////////////////

	/**
	 * 删除配置
	 * 
	 * @param keycode
	 * @return Integer
	 */
    @CacheEvict(value = "config", allEntries = true)
	public int deleteConfigByKey(String keycode) {
	    return configDao.deleteConfig(keycode);
	}

	// ///////////////////////////////
	// ///// 修改 ////////
	// ///////////////////////////////
    /**
     * 更新配置
     *
     * @param key
     * @param value
     * @return Integer
     */
    @CacheEvict(value = "config", allEntries = true)
    public int updagteConfigByKey(String key, String value) {
        Config config = new Config();
        config.setKeycode(key);
        config.setKeyvalue(value);
        return configDao.updagteConfigByKey(config);
    }

	/**
	 * 更新配置
	 *
	 * @param webConfig
	 * @return Integer
	 */
    @CacheEvict(value = "config", allEntries = true)
	public int updagteConfigByKey(Config webConfig) {
		Config config = new Config();
		config.setId(webConfig.getId());
		config.setKeycode(webConfig.getKeycode());
		config.setKeyvalue(webConfig.getKeyvalue());
		config.setTypebase(webConfig.getTypebase());
		config.setDescription(webConfig.getDescription());
		config.setSort(webConfig.getSort());
		return configDao.updagteConfigByKey(config);
	}
	
	// ///////////////////////////////
	// /////       查询       ////////
	// ///////////////////////////////

	/**
	 * @param keycode
	 * @return
	 */
	public String getStringByKey(String keycode) {
		Config config = configDao.getConfigByKey(keycode);
		if (config == null) {
			return "";
		} else {
			return config.getKeyvalue();
		}
	}

	/**
	 * @param key
	 * @return
	 */
	public int getIntKey(String key) {
		Config config = configDao.getConfigByKey(key);
		if (config == null) {
			return 0;
		} else {
			return Integer.parseInt("1");
		}
	}
	
	@Cacheable(value="config")
	public Config getConfigByKey(String key) {
		return configDao.getConfigByKey(key);
	}
	
	/**
	 * 配置信息总数
	 * 
	 * @return
	 */
	public int getConfigCount(){
		return configDao.getConfigCount();
	}
	
	/**
	 * 配置信息翻页列表
	 * 
	 * @param offset
	 * @param rows
	 * @return
	 * @throws Exception
	 */
	public List<Config> getConfigList(int offset, int rows){
		List<Config> groupList = configDao.getConfigList(offset, rows);
		return groupList;
	}
	
	
	
	/**
	 * 查看配置列表分页
	 * 
	 * @return List<Config>
	 * @throws Exception 
	 */
	public PageVo<Config> getConfigVoPage(int pageNum, int rows) throws Exception{
		PageVo<Config> pageVo = new PageVo<Config>(pageNum);
		pageVo.setRows(rows);
		List<Config> list = new ArrayList<Config>();
		int count = 0;
		count = this.getConfigCount();
		pageVo.setList(this.getConfigList(pageVo.getOffset(), pageVo.getRows()));
		pageVo.setCount(count);
		return pageVo;
	}

	/**
	 * 所有配置列表信息
	 *
	 * @return
	 * @throws Exception
	 */
	@Cacheable(value = "config")
	public List<Config> getConfigAllList(){
		return configDao.getConfigAllList();
	}

}
