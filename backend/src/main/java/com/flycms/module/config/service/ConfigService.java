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
import com.flycms.module.config.model.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



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

	private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

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
     * 更新配置（upsert：键不存在时自动插入，避免白名单新增键被静默丢弃）。
     *
     * <p>两点防御，都是为了"清空某个配置"这个合法操作：
     * <ul>
     *   <li>value 为 null 时归一为空串：既保证 SQL 里该字段会被写进去，
     *       也避免把 NULL 存进库后读取方（如 {@link #getStringByKey}）拿到 null 引发 NPE。</li>
     *   <li>key 为空直接忽略：否则 update 匹配不到任何行，会退化成插入一条 keycode 为空的脏数据。</li>
     * </ul>
     *
     * @param key   配置键
     * @param value 配置值，允许空串
     * @return 受影响行数（0 表示键此前不存在，已改为插入）
     */
    @CacheEvict(value = "config", allEntries = true)
    public int updagteConfigByKey(String key, String value) {
        if (key == null || key.isEmpty()) {
            logger.warn("忽略配置更新请求：keycode 为空（value={}）", value);
            return 0;
        }
        Config config = new Config();
        config.setKeycode(key);
        config.setKeyvalue(value == null ? "" : value);
        int rows = configDao.updagteConfigByKey(config);
        if (rows == 0) {
            config.setTypebase(0);
            configDao.addConfig(config);
        }
        return rows;
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
		return getIntKey(key, 0);
	}

	/**
	 * 读取整型配置值
	 *
	 * 修复说明：原实现恒返回 {@code Integer.parseInt("1")}，即任何已存在的配置都返回 1，
	 * 配置值本身被丢弃，属于明显逻辑错误。现改为真正解析 keyvalue，并在缺失/非法值时返回默认值。
	 *
	 * @param key
	 *         配置键
	 * @param def
	 *         配置不存在或不可解析时的默认值
	 * @return 解析后的整数
	 */
	public int getIntKey(String key, int def) {
		Config config = configDao.getConfigByKey(key);
		if (config == null || config.getKeyvalue() == null) {
			return def;
		}
		try {
			return Integer.parseInt(config.getKeyvalue().trim());
		} catch (NumberFormatException e) {
			return def;
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
