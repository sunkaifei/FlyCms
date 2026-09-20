/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */
package com.flycms.module.search.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
@Setter
@Getter
public class Search implements Serializable {
	private static final long serialVersionUID = 1L;
	//查询数量
	private int maxDoc;
	//查询列表
	private List<Info> searcher;
	//翻页
	private String labelPage;

}
