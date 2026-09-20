/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */
package com.flycms.module.links.dao;

import java.util.List;

import com.flycms.module.links.model.Links;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 友情链接模块数据操作类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年5月25日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email admin@97560.com
 * @version 1.0,2017年10月1日 <br/>
 * 
 */

@Repository
public interface LinksDao {
	
		// ///////////////////////////////
		// ///// 增加 ////////
		// ///////////////////////////////
		/**
		 * 添加友情链接
		 * 
		 * @param links
		 * @return
		 */
		public int addLinks(Links links);
		
		
		// ///////////////////////////////
		// ///// 刪除 ////////
		// ///////////////////////////////
		/**
		 * 按id删除友情链接信息
		 * 
		 * @param id
		 * @return
		 */
		public int deleteLinksById(@Param("id") Integer id);
		
		
		// ///////////////////////////////
		// ///// 修改 ////////
		// ///////////////////////////////
		/**
		 * 修改友情链接信息
		 * 
		 * @param links
		 * @return
		 */
		public int updateLinksById(Links links);
		
		
		// ///////////////////////////////
		// ///// 查詢 ////////
		// ///////////////////////////////
		/**
		 * 按id查询友情链接信息
		 * 
		 * @param id
		 * @return
		 */
		public Links findLinksById(@Param("id") Integer id);

	/**
	 * 查询网站链接是否存在
	 *
	 * @param linkUrl
	 *         网站链接地址
	 * @return
	 */
		public int checkLinksByLinkUrl(@Param("linkUrl") String linkUrl);
		
		/**
		 * 查询友情链接数量
		 * 
		 * @param type
		 * @param isShow
		 * @return
		 */
		public int getLinksCount(@Param("type") long type, @Param("isShow") long isShow);
		
		
		/**
		 * 查询友情链接列表
		 * 
		 * @param type
		 * @param isShow
		 * @param offset
		 * @param rows
		 * @return
		 */
		public List<Links> getLinksList(@Param("type") Integer type,
												@Param("isShow") Integer isShow,
												@Param("offset") long offset,
												@Param("rows") long rows);

}
