/*
 *	Copyright © 2015 Zhejiang SKT Science Technology Development Co., Ltd. All rights reserved.
 *	浙江斯凯特科技发展有限公司 版权所有
 *	http://www.28844.com
 */

package com.flycms.core.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * json数据返回实体类
 * 
 * @author sunkaifei
 */
@Setter
@Getter
public class CkeditorUp implements Serializable {
	private static final long serialVersionUID = 1L;
	private int uploaded;
	private int number;
	private String fileName;
	private String message;
	private Object error;
	private String url;

	private CkeditorUp(int uploaded,int number,String fileName, String message, String url, Object error) {
		this.uploaded = uploaded;
		this.number = number;
		this.fileName = fileName;
		this.message = message;
		this.url = url;
		this.error = error;
	}

	public static final CkeditorUp success(int uploaded,String fileName,String url) {
		return new CkeditorUp(uploaded,0,fileName, null, url,null);
	}

    public static final CkeditorUp failure(String message) {
        Map<String, Object> map = new HashMap<>();
        map.put("message", message);
        map.put("number", 1);
        return new CkeditorUp(0,0,null, null, null,map);
    }

/*
	public static final CkeditorUp success(String message, Object data) {
		return new CkeditorUp(CODE_SUCCESS, message,null, data);
	}

	public static final CkeditorUp failure(int code, String message) {
		return new CkeditorUp(code, message,null, NOOP);
	}
	
	public static final CkeditorUp jump(String message, String url) {
		return new CkeditorUp(CODE_SUCCESS, message,url, NOOP);
	}
*/

}