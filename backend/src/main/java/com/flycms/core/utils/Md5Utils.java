package com.flycms.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * MD5 的加密
 * 
 * @author sunkaifei
 * @datetime 2010-8-17 下午05:11:29
 */
public class Md5Utils {
	private static Logger logger = LoggerFactory.getLogger(Md5Utils.class);
	private final static char[] hexDigits = { '0', '1', '2', '3', '4', '5',
        '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
	/**
	 * 對字符串進行MD5加密
	 * @author tianwl
	 * @param s
	 * @return
	 */
	public final static String getStringMD5(String s) {
		byte[] strTemp = s.getBytes();
		return Md5Utils.getByteArrayMD5(strTemp);
	}
	/**
	 * 對byte数组進行MD5加密
	 * @author tianwl
	 * @param source
	 * @return
	 */
	public final static String getByteArrayMD5(byte[] source) {
		try {
			// 使用MD5创建MessageDigest对象
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(source);
			byte[] md = mdTemp.digest();
			int j = md.length;
			char str[] = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte b = md[i];
				// 将没个数(int)b进行双字节加密
				str[k++] = hexDigits[b >> 4 & 0xf];
				str[k++] = hexDigits[b & 0xf];
			}
			return new String(str);
		} catch (Exception e) {
			logger.error("加密错误！",e);
			return null;
		}
	}
	/**
	 * 得到图片的MD5，也可以用于二级栏目的MD5
	 * @author tianwl
	 * @param pic 图片的字符串 
	 * @param url
	 * @return
	 */
	public static String getPictureMD5(String pic,String url){
		String md5 = "";
		StringBuffer sb = new StringBuffer(pic);
		sb.append(url);
		md5 = Md5Utils.getStringMD5(new String(sb));
		return md5;
	}
	
	/**
	 * 生成重庆的二级菜单的resourceNameMD5
	 * @author tianwl
	 * @param name
	 * @param url
	 * @param channelType
	 * @param services 如果没有此数据 输入 ""
	 * @return
	 */
	public static String getMenuMD5(String name ,String url,String channelType ,String services){
		String md5 = "";
		StringBuffer sb = new StringBuffer(name);
		sb.append(url);
		sb.append(channelType);
		if(services != null && !services.equals("")){
			sb.append(services);
		}
		md5 = Md5Utils.getStringMD5(new String(sb));
		return md5;
	}
	
    private static String bytesToHex(byte[] bytes) {  
        StringBuffer sb = new StringBuffer();  
        int t;  
        for (int i = 0; i < 16; i++) {  
            t = bytes[i];  
            if (t < 0) { 
                t += 256;
            }
            sb.append(hexDigits[(t >>> 4)]);  
            sb.append(hexDigits[(t % 16)]);  
        }  
        return sb.toString();  
    }  

    public static String code(String input, int bit) throws Exception {  
        try {  
            MessageDigest md = MessageDigest.getInstance(System.getProperty(  
                    "MD5.algorithm", "MD5"));  
            if (bit == 16)  {
                return bytesToHex(md.digest(input.getBytes("utf-8")))  
                        .substring(8, 24); 
            }
            return bytesToHex(md.digest(input.getBytes("utf-8")));  
        } catch (NoSuchAlgorithmException e) {  
            e.printStackTrace();  
            throw new Exception("Could not found MD5 algorithm.", e);  
        }  
    }  
    
	/**
	 * MD5 加密
	 * @param str
	 * @return
	 * @throws Exception
	 */
	 public static String  getMD5(String str) {  
	        MessageDigest messageDigest = null;  
	            try {
					messageDigest = MessageDigest.getInstance("MD5");
					messageDigest.reset();
					messageDigest.update(str.getBytes("UTF-8"));
				} catch (Exception e) {
					logger.error("MD5转换异常！message：%s", e.getMessage());
				}  
				
	        byte[] byteArray = messageDigest.digest();  
	        StringBuffer md5StrBuff = new StringBuffer();  
	        for (int i = 0; i < byteArray.length; i++) {              
	            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1){  
	                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));  
	            }else{  
	                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));  
	            }
	        }  
	        return md5StrBuff.toString();  
	    }

	public static void main(String[] args) {
		System.out.println(getMD5("1"));
	}
}
