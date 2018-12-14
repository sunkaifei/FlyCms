/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */

package com.flycms.core.entity;

import java.io.Serializable;
/**
 * json数据返回实体类
 * 
 * @author sunkaifei
 */

public class DataVo implements Serializable {
	private static final long serialVersionUID = 1L;
	public static int CODE_SUCCESS = 0;
	public static int CODE_FAILURED = -1;
	public static String[] NOOP = new String[0];
	private int code;
	private String message;
	private Object data;
	private String url;

	private DataVo(int code, String message, String url, Object data) {
		this.code = code;
		this.message = message;
		this.url = url;
		this.data = data;
	}

	public static final DataVo success(Object data) {
		return new DataVo(CODE_SUCCESS,null, "操作成功", data);
	}

	public static final DataVo success(String message) {
		return new DataVo(CODE_SUCCESS, message,null, NOOP);
	}

	public static final DataVo success(String message, Object data) {
		return new DataVo(CODE_SUCCESS, message,null, data);
	}

	public static final DataVo failure(int code, String message) {
		return new DataVo(code, message,null, NOOP);
	}
	
	public static final DataVo jump(String message, String url) {
		return new DataVo(CODE_SUCCESS, message,url, NOOP);
	}

	public static final DataVo failure(String message) {
		return failure(CODE_FAILURED, message);
	}

	public int getCode() {
		return this.code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return this.message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return this.data;
	}

	public void setData(Object data) {
		this.data = data;
	}
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return "{code:\"" + this.code + "\", message:\"" + this.message + "\", url:\"" + this.url + "\", data:\"" + this.data.toString() + "\"}";
	}


}