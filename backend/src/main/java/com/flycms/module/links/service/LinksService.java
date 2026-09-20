package com.flycms.module.links.service;

import java.util.Date;
import java.util.List;

import com.flycms.core.entity.DataVo;
import com.flycms.core.entity.PageVo;
import com.flycms.core.utils.SnowFlake;
import com.flycms.module.links.dao.LinksDao;
import com.flycms.module.links.model.Links;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Open source house, All rights reserved
 * 版权：28844.com<br/>
 * 开发公司：28844.com<br/>
 *
 * 友情链接服务
 *
 * @author sun-kaifei
 * @version 1.0 <br/>
 * @email 79678111@qq.com
 * @Date: 17:12 2018/11/3
 */
@Service
public class LinksService {
	
	@Autowired
	private LinksDao linksDao;
	
	// ///////////////////////////////
	// ///// 增加 ////////
	// ///////////////////////////////		
	
	/**
	 * 添加网站友情链接
	 *
	 * @param links
	 * @return
	 * @throws Exception
	 */
	public DataVo addLinks(Links links) {
		DataVo data = DataVo.failure("操作失败");
		if(this.checkLinksByLinkUrl(links.getLinkUrl())){
			return DataVo.failure("网站连接已存在！");
		}
		SnowFlake snowFlake = new SnowFlake(2, 3);
		links.setId(snowFlake.nextId());
		links.setCreateTime(new Date());
		int totalCount=linksDao.addLinks(links);
		if(totalCount > 0){
			data = DataVo.success("添加成功", DataVo.NOOP);
		}else{
			data=DataVo.failure("未知错误！");
		}
		return data;
	}
	
	// ///////////////////////////////
	// ///// 刪除 ////////
	// ///////////////////////////////
	/**
	 * 按id删除友情链接信息
	 *
	 * @param id
	 * @return
	 */
	public boolean deleteLinksById(Integer id) {
		int totalCount=linksDao.deleteLinksById(id);
		return totalCount > 0 ? true : false;
	}
	
	// ///////////////////////////////
	// ///// 修改 ////////
	// ///////////////////////////////
	/**
	 * 修改友情链接信息
	 *
	 * @param links
	 * @return
	 */
	public DataVo updateLinksById(Links links) {
		DataVo data = DataVo.failure("操作失败");
		links.setCreateTime(new Date());
		int totalCount=linksDao.updateLinksById(links);
		if(totalCount > 0){
			data = DataVo.success("添加成功", DataVo.NOOP);
		}else{
			data=DataVo.failure("未知错误！");
		}
		return data;
	}
	
	// ///////////////////////////////
	// ///// 查询 ////////
	// ///////////////////////////////
	/**
	 * 按id查询友情链接信息
	 *
	 * @param id
	 * @return
	 */
	public Links findLinksById(Integer id){
		return linksDao.findLinksById(id);
	}

	/**
	 * 查询网站链接是否存在
	 *
	 * @param linkUrl
	 *         网站链接地址
	 * @return
	 */
	public boolean checkLinksByLinkUrl(String linkUrl) {
		int totalCount = linksDao.checkLinksByLinkUrl(linkUrl);
        return totalCount > 0 ? true : false; 
	}
	
	/**
	 * @return
	 * @throws Exception
	 */
	public int getLinksCount(long type, long show) throws Exception {
		return linksDao.getLinksCount(type, show);
		
	}
	
	/**
	 * @param offset
	 * @param rows
	 * @return
	 * @throws Exception
	 */
	public List<Links> getLinksList(int type, int show, int offset, int rows) throws Exception {
		List<Links> friendLinkList = linksDao.getLinksList(type, show,offset, rows);
		return friendLinkList;
	}
	
	/**
	 * @param pageNum
	 * @return
	 * @throws Exception
	 */
	public PageVo<Links> getLinksListPage(int type, int show, int pageNum, int rows) throws Exception {
		PageVo<Links> pageVo = new PageVo<Links>(pageNum);
		pageVo.setRows(rows);
		pageVo.setList(this.getLinksList(type, show, pageVo.getOffset(), pageVo.getRows()));
		pageVo.setCount(this.getLinksCount(type,show));
		return pageVo;
	}
}
