package com.flycms.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * ip工具类
 * 
 * @author sunaifei
 */
public class IpUtils{
	private static Logger logger = LoggerFactory.getLogger(IpUtils.class);
	public IpUtils()
	{
	}

	public static boolean isIPAddress(String str1)
	{
		if(str1 == null || str1.length() < 7 || str1.length() > 15)
		{
			return false;
		} else
		{
			String regformat = "^\\d{1,3}[\\.]\\d{1,3}[\\.]\\d{1,3}[\\.]\\d{1,3}$";
			Pattern pat = Pattern.compile(regformat);
			Matcher m = pat.matcher(str1);
			return m.find();
		}
	}

	public static String getIPAddress(HttpServletRequest request)
	{
		String result = "";
		result = request.getHeader("HTTP_X_FORWARDED_FOR");
		if(result != null && !"".equals(result)){
			if(result.indexOf(".") == -1){
				result = null;
			}else{
				if (result.indexOf(",") != -1) {
					result = result.replace(" ", "").replace("'", "");
					String temparyip[] = result.split(",;");
					for (int i = 0; i < temparyip.length; i++){
						if (isIPAddress(temparyip[i]) && temparyip[i].substring(0, 3) != "10."
								&& temparyip[i].substring(0, 7) != "192.168"
								&& temparyip[i].substring(0, 7) != "172.16."){
							return temparyip[i];
						}
					}
				} else {
					if (isIPAddress(result)){
						return result;
					}
					result = null;
				}
			}
		}
		if(result == null || "".equals(result)){
			result = request.getRemoteAddr();
		}
		return result;
	}

	public static final String getIpAddr(final HttpServletRequest request) {
		String ipAddress = null;
		if (request == null) {
			logger.error("未获取到ip信息");
		}
		ipAddress = request.getHeader("x-forwarded-for");
		if (ipAddress == null || ipAddress.length() == 0
				|| "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0
				|| "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0
				|| "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
			if (ipAddress.equals("127.0.0.1")) {
				// 根据网卡取本机配置的IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				ipAddress = inet.getHostAddress();
			}

		}

		// 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
		if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
															// = 15
			if (ipAddress.indexOf(",") > 0) {
				ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
			}
		}
		//System.out.print(ipAddress+"--------------"+getIp2(request));
		return ipAddress;
	} 
	
	
	public static String getIp2(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if(StringUtils.isEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = ip.indexOf(",");
            if(index != -1){
                return ip.substring(0,index);
            }else{
                return ip;
            }
        }
        ip = request.getHeader("X-Real-IP");
        if(StringUtils.isEmpty(ip) && !"unKnown".equalsIgnoreCase(ip)){
            return ip;
        }
        return request.getRemoteAddr();
    }
	
	/**
	 * IP转换成整数值
	 * 
	 * @param strip
	 *        需要处理的ip
	 * @return
	 */
	public static long ip2Long(String strip)
	{
		long ip[] = new long[4];
		int position1 = strip.indexOf(".");
		int position2 = strip.indexOf(".", position1 + 1);
		int position3 = strip.indexOf(".", position2 + 1);
		ip[0] = Long.parseLong(strip.substring(0, position1));
		ip[1] = Long.parseLong(strip.substring(position1 + 1, position2));
		ip[2] = Long.parseLong(strip.substring(position2 + 1, position3));
		ip[3] = Long.parseLong(strip.substring(position3 + 1));
		return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
	}

	/**
	 * 将数值还原成ip地址
	 * 
	 * @param longIp
	 *        需要还原的数值
	 * @return
	 */
	public static String longToIP(long longIp) {
        StringBuffer sb = new StringBuffer("");
        //直接右移24位
        sb.append(String.valueOf((longIp >>> 24)));
        sb.append(".");
        //将高8位置0，然后右移16位
        sb.append(String.valueOf((longIp & 0x00FFFFFF) >>> 16));
        sb.append(".");
        //将高16位置0，然后右移8位
        sb.append(String.valueOf((longIp & 0x0000FFFF) >>>  8 ));
        sb.append(".");
        //将高24位置0
        sb.append(String.valueOf((longIp & 0x000000FF)));
        return sb.toString();
    } 
}