/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */

package com.flycms.core.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * 开发公司：浙江斯凯特科技发展有限公司 版权所有 <p>
 * 版权所有：© www.28844.com<p>
 * 博客地址：http://www.28844.com  <p>
 * <p>
 * 
 * 分页器
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年11月12日 　翻页处理类<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email 79678111@qq.com
 * @version 1.0,2017年11月12日 <br/>
 * 
 */
public class PageVo<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * 页码
	 */
	private int pageNum;
	/**
	 * 页码总数
	 */
	private int pageCount;
	/**
	 * 总数
	 */
	private int count;
	/**
	 * 偏移
	 */
	private int offset;
	/**
	 * 数量
	 */
	private int rows;
	/**
	 * 数据
	 */
	private List<T> list;
	/**
	 * 页码HTML
	 */
	@SuppressWarnings("unused")
	private String pageNumHtml;
	/**
	 * 参数
	 */
	private Map<String, String> args = new HashMap<String, String>();

	public PageVo(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getPageNum() {
		if (this.pageNum <= 0) {
			this.pageNum = 1;
			return 1;
		} else {
			return pageNum;
		}
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public int getPageCount() {
		//this.pageCount = ((this.getCount() - 1) / this.getRows()) + 1;
		this.pageCount = (int) (((this.getCount() - 1) / this.getRows()) + 1);
		return pageCount;
	}

	public void setPageCount(int pageCount) {
		this.pageCount = pageCount;
	}

	public int getOffset() {
		this.offset = (this.getPageNum() - 1) * this.getRows();
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public String getUrl(int num) {
		Iterator<Entry<String, String>> iter = this.getArgs().entrySet().iterator();
		List<String> values = new ArrayList<String>();
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			Object key = entry.getKey();
			Object val = entry.getValue();
			values.add(key + "=" + val);
		}
		values.add("p=" + num);
		return "?" + StringUtils.join(values.toArray(), "&");
	}

	public void setPageNumHtml(String pageNumHtml) {
		this.pageNumHtml = pageNumHtml;
	}

	public String getPageNumHtml() {
		StringBuffer sb = new StringBuffer();
		//sb.append("<ul class=\"pagination\">");
		// 首页，上一页
		if (this.getPageNum() != 1) {
			sb.append("<li><a href='" + this.getUrl(1)
					+ "' title='首页'>首页</a></li>");
			sb.append("<li class=\"prev\"><a href='" + this.getUrl(this.getPageNum() - 1)
					+ "' rel=\"prev\" title='上一页'>上一页</a></li>");
		}
		// 页码
		if (this.getPageCount() != 1) {
			int startNum = this.getPageNum() - 3 <= 1 ? 1
					: this.getPageNum() - 3;
			int endNum = this.getPageNum() + 3 >= this.getPageCount() ? this
					.getPageCount() : this.getPageNum() + 3;
			if (startNum > 1) {
				sb.append("<li><a href='javascript:void(0);'>...</a></li>");
			}
			for (int i = startNum; i <= endNum; i++) {
				if (i == pageNum) {
					sb.append("<li class=\"active\"><a   href='" + this.getUrl(i)
							+ "' class='ahover' title='" + i + "'>" + i
							+ "</a></li>");
				} else {
					sb.append("<li><a href='" + this.getUrl(i)
							+ "' title='" + i + "'>" + i
							+ "</a></li>");
				}
			}
			if (endNum < this.getPageCount()) {
				sb.append("<li><a href='javascript:void(0);'>...</a></li>");
			}
		}
		// 下一页，尾页
		if (this.getPageNum() < this.getPageCount()) {
			sb.append("<li class='next'><a href='" + this.getUrl(this.getPageNum() + 1)
					+ "' rel='next' title='下一页'>下一页</a></li>");
			sb.append("<li><a href='" + this.getUrl(this.getPageCount())
					+ "' title='末页'>末页</a></li>");
		}
		//sb.append("</ul>");
		return sb.toString();
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public Map<String, String> getArgs() {
		return args;
	}

	public void setArgs(Map<String, String> args) {
		this.args = args;
	}

}
