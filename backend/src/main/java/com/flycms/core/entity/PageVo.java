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
		// 历史 Bug：rows 为 0 时除零抛 ArithmeticException。
		// 其余分支保持原有语义（总数为 0 时仍返回 1 页，避免改动既有分页渲染行为）。
		if (this.getRows() <= 0) {
			this.pageCount = 0;
			return this.pageCount;
		}
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

	/**
	 * 单页条数硬上限（收口点）。
	 *
	 * <p>rows 一路来自模板标签参数 / URL 查询串，此前无任何上限：
	 * 传 rows=1000000 会让查询把整表拉回内存（并连带 offset 溢出），是一条公开的
	 * 内存耗尽路径。这里统一钳制，所有分页调用方自动受保护；
	 * 500 高于现有业务上限（API 200、标签 100），不影响任何既有用法。
	 */
	public static final int MAX_ROWS = 500;

	public void setRows(int rows) {
		// 负数归零（历史行为：getPageCount 对 rows<=0 返回 0 页），上限钳制到 MAX_ROWS
		this.rows = Math.min(Math.max(rows, 0), MAX_ROWS);
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
