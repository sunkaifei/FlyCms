package com.flycms.core.utils;

import java.io.Serializable;
import java.util.Random;
import java.util.UUID;

/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * uuid工具类
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
 * @version 1.0 <br/>
 * 
 */
public class UuidUtils implements Serializable {
	private static final long serialVersionUID = 5952689219411916553L;

	public static String get32UUID() {
		String uuid = UUID.randomUUID().toString().trim().replaceAll("-", "");
		return uuid;
	}
	
	private static Random strGen = new Random();;
	private static Random numGen = new Random();;
	private static char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
			.toCharArray();;
	private static char[] numbers = ("0123456789").toCharArray();;

	/** * 产生随机字符串 * */
	public static final String randomString(int length) {
		if (length < 1) {
			return null;
		}
		char[] randBuffer = new char[length];
		for (int i = 0; i < randBuffer.length; i++) {
			randBuffer[i] = numbersAndLetters[strGen.nextInt(61)];
		}
		return new String(randBuffer);
	}

	/** * 产生随机数值字符串 * */
	public static final String randomNumStr(int length) {
		if (length < 1) {
			return null;
		}
		char[] randBuffer = new char[length];
		for (int i = 0; i < randBuffer.length; i++) {
			randBuffer[i] = numbers[numGen.nextInt(9)];
		}
		return new String(randBuffer);
	}
}

