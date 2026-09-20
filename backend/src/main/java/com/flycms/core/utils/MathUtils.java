package com.flycms.core.utils;

import java.util.Random;
/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 随机数处理 工具类
 * 
 * <p>
 * 
 * 区分　责任人　日期　　　　说明<br/>
 * 创建　孙开飞　2017年7月20日 　<br/>
 * <p>
 * *******
 * <p>
 * 
 * @author sun-kaifei
 * @email 79678111@qq.com
 * @version 1.0,2017年7月20日 <br/>
 * 
 */
public class MathUtils {
	/**
	 * 获取随机的数值。
	 * @param length	长度
	 * @return
	 */
	public static String getRandomCode(Integer length){
	   String result = "";
	   Random rand = new Random();
	   int n = 20;
	   if(null != length && length > 0){
		   n = length;
	   }
       boolean[]  bool = new boolean[n];
       int randInt = 0;
       for(int i = 0; i < length ; i++) {
            do {
                randInt  = rand.nextInt(n);

            }while(bool[randInt]);

           bool[randInt] = true;
           result += randInt;
       }
       return result;
	}
}