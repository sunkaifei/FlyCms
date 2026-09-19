package com.flycms.core.entity;

import java.io.Serializable;
/**
 * 
 * 开发公司：97560.com<br/>
 * 版权：97560.com<br/>
 * <p>
 * 
 * 实体类
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
 * @version 1.0,2017年7月25日 <br/>
 * 
 */
public class UpImgMsg implements Serializable {
	private static final long serialVersionUID = 1L;
	private int code;
	private String msg;
	private String imgurl;
	private double filesize;
	private int filenum;
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getMsg() {
		return msg;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getImgurl() {
		return imgurl;
	}
	public void setImgurl(String imgurl) {
		this.imgurl = imgurl;
	}
	public double getFilesize() {
		return filesize;
	}
	public void setFilesize(double filesize) {
		this.filesize = filesize;
	}
	public int getFilenum() {
		return filenum;
	}
	public void setFilenum(int filenum) {
		this.filenum = filenum;
	}
}
