package com.flycms.core.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 *
 * 字符串处理工具
 *
 * @author SunKaiFei
 *
 */
public class StringHelperUtils {
	private static Logger logger = LoggerFactory.getLogger(StringHelperUtils.class);

	/**
	 * 去掉NULL
	 * 
	 * @param content
	 * @return String
	 */
	public static String delNull(String content) {
		if (content == null) {
			return "";
		} else {
			return content.trim();
		}
	}
	
	/**
	 * 过滤 HTML
	 * 
	 * @param inputString
	 * @return String
	 */
	public static String Html2Text(String inputString) {
		if (inputString == null){
			return "";
		}
		String htmlStr = inputString; // 含HTML标签的字符串
		String textStr = "";
		Pattern p_script;
		Matcher m_script;
		Pattern p_style;
		Matcher m_style;
		Pattern p_html;
		Matcher m_html;
		try {
			String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>"; // 定义script的正则表达式{�?<script[^>]*?>[\\s\\S]*?<\\/script>
			String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>"; // 定义style的正则表达式{�?<style[^>]*?>[\\s\\S]*?<\\/style>
			String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式
			p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
			m_script = p_script.matcher(htmlStr);
			htmlStr = m_script.replaceAll(""); // 过滤script标签

			p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
			m_style = p_style.matcher(htmlStr);
			htmlStr = m_style.replaceAll(""); // 过滤style标签

			p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
			m_html = p_html.matcher(htmlStr);
			htmlStr = m_html.replaceAll(""); // 过滤HTML标签

			htmlStr = htmlStr.replaceAll("\"", " ").replaceAll("\'", " ");
			textStr = htmlStr;
		} catch (Exception e) {
			System.err.println("Html2Text: " + e.getMessage());
		}
		return textStr;// 返回文本字符
	}
	
	/**
	 * 过滤字符文本为可显示的HTML文本
	 * 
	 * @param str
	 * @return
	 */
	public static String htmlReplace(String str) {
		try {
			if (str != null) {
				str = Html2Text(str);
				str = str.replaceAll("&", "&amp;"); // &号
				str = str.replaceAll("×", "&times;"); // 乘号
				str = str.replaceAll("÷", "&divide;"); // 除号
				str = str.replaceAll("¥", "&yen;"); // 日圆
				str = str.replaceAll("\"", "&quot;"); // "号
				str = str.replaceAll("'", "&acute;"); // "号
				str = str.replaceAll("<", "&lt;"); // 正括号
				str = str.replaceAll(">", "&gt;"); // 反括号
				str = str.replaceAll("\"", "\\");
				str = str.replaceAll("\r", ""); // 回车
				str = str.replaceAll("\n", ""); // 回车
				str = str.replaceAll("  ", "&nbsp;"); // 空格
				str = str.replaceAll("/", "&#47;"); // 斜杠
				str = str.replaceAll("©", "&copy;"); // 版权符
				str = str.replaceAll("®", "&reg;"); // 注册符
				str = str.replaceAll("\t", "&nbsp;&nbsp;&nbsp;&nbsp;"); // TAB键
			}
			return str.trim();
		} catch (Exception e) {
			System.out.println("过滤字符文本为可显示的HTML文本类出错！" + e);
			return "";
		}
	}
	
	/**
	 * 过滤字符文本内的HTML文本和特殊字符
	 * 
	 * @param str
	 * @return
	 */
	public static String getReplaceAllHtml(String str) {
		try {
			if (str != null) {
				str = str.replaceAll("&", ""); // &号
				str = str.replaceAll("×", ""); // 乘号
				str = str.replaceAll("÷", ""); // 除号
				str = str.replaceAll("¥", ""); // 日圆
				str = str.replaceAll("\"", ""); // "号
				str = str.replaceAll("'", ""); // "号
				str = str.replaceAll("<", ""); // 正括号
				str = str.replaceAll(">", ""); // 反括号
				str = str.replaceAll("\"", "");
				str = str.replaceAll("\r", ""); // 回车
				str = str.replaceAll("\n", ""); // 回车
				str = str.replaceAll("  ", ""); // 空格
				str = str.replaceAll("/", ""); // 斜杠
				str = str.replaceAll("©", ""); // 版权符
				str = str.replaceAll("®", ""); // 注册符
				str = str.replaceAll("\t", ""); // TAB键
				str = str.replaceAll("amp;", ""); // &号
				str = str.replaceAll("times;", ""); // 乘号
				str = str.replaceAll("divide;", ""); // 除号
				str = str.replaceAll("yen;", ""); // 日圆
				str = str.replaceAll("quot;", ""); // "号
				str = str.replaceAll("acute;", ""); // "号
				str = str.replaceAll("lt;", ""); // 正括号
				str = str.replaceAll("gt;", ""); // 反括号
				str = str.replaceAll("nbsp;", ""); // 空格
				str = str.replaceAll("#47;", ""); // 斜杠
				str = str.replaceAll("copy;", ""); // 版权符
				str = str.replaceAll("reg;", ""); // 注册符
			}
			return str.trim();
		} catch (Exception e) {
			System.out.println("过滤字符文本为可显示的HTML文本类出错！" + e);
			return "";
		}
	}

    /**
     * kindeditor 内容字符转义
     *
     * @param str
     * @return
     */
	public static String htmlspecialchars(String str) {
		str = str.replaceAll("&", "&amp;");
		str = str.replaceAll("<", "&lt;");
		str = str.replaceAll(">", "&gt;");
		str = str.replaceAll("\"", "&quot;");
		return str;
	}

	public static boolean isAlphaUnderline(String msg) {
		Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]+$");
		Matcher matcher = pattern.matcher(msg);
		return matcher.matches();
	}

	/**
	 * 检查用户名是否符合要求
	 * 由字母数字下划线组成且开头必须是字母，不能超过16位
	 *
	 * @param userName
	 * @return
	 */
	public static boolean checkUserName(String userName){
		if (StringUtils.isBlank(userName)) {
			return false;
		}
		return check("[a-zA-Z]{1}[a-zA-Z0-9_]{1,15}$",userName);
	}

	/**
	 * 检查是否是手机号码
	 *
	 * @param PhoneNumber
	 * @return
	 */
	public static boolean checkPhoneNumber(String PhoneNumber){
		if (StringUtils.isBlank(PhoneNumber)) {
			return false;
		}
		return check("^((13[0-9])|(15[^4])|(166)|(18[0,1,2,3,5-9])|(17[0-8])|(147))\\d{8}$",PhoneNumber);
	}
	/**
	 * 获取图片地址的图片后缀
	 *
	 * @param url
	 *        网络图片地址
	 * @return
	 */
	public static String getImageUrlSuffix(String url){
		Pattern p =Pattern.compile("\\/[^\\/]*\\.(jpg|jpeg|gif|png|bmp)\\??[^?]*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(url);
		String regex ="";
		if(matcher.find()){
			regex = matcher.group(1);
		}
		return regex;
	}

	/**
	 * 获取图片本站图片地址根目录路径
	 *
	 * @param url
	 * @return
	 */
	public static String getImageRootUrl(String url){
		Pattern p =Pattern.compile("^[http://|ftp://|https://|www]+[^\\/]*\\/(.*\\.(jpg|jpeg|gif|png|bmp))\\??[^?]*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(url);
		String regex ="";
		if(matcher.find()){
			regex = matcher.group(1);
		}
		return regex;
	}
	
    /** 
     * 验证输入的邮箱格式是否符合 
     * @param email 
     * @return 是否合法 
     */ 

	public static boolean emailFormat(String email) {
        boolean tag = true;
        final String pattern1 = "^([a-zA-Z0-9]+[_|_|.]?)*[a-zA-Z0-9]+@([a-zA-Z0-9]+[_|_|.]?)*[a-zA-Z0-9]+.[a-zA-Z]{2,4}$";
        final Pattern pattern = Pattern.compile(pattern1);
        final Matcher mat = pattern.matcher(email);
        if (!mat.find())
        {
            tag = false;
        }
        return tag;
    }


	/**
	 * 验证输入密码条件(字符与数据同时出现)
	 *
	 * @param str
	 *     待验证的字符串
	 * @return 如果是符合格式的字符串,返回 <b>true </b>,否则为 <b>false </b>
	 */
	public static boolean IsPassword(String str) {
		if (StringUtils.isBlank(str)) {
			return false;
		}
		return check("^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$",str);
	}
	
	public static String getHideMail(String mail){
		if(StringUtils.isEmpty(mail)){
			return "";
		}
		return mail.substring(0,1) + "***"+mail.substring(mail.lastIndexOf("@")-1,mail.length());
	}
	
	/**
	 * 去除字符串中的空格、回车、换行符、制表符
	 * 
	 * @param str
	 * @return
	 */
	public static String replaceBlank(String str) {
		str = str.replaceAll("&nbsp;", ""); // 空格
		if(str!=null && !"".equals(str)) {      
			String regular="\\s*|\t|\r|\n| ";
			Pattern p = Pattern.compile(regular);      
            Matcher m = p.matcher(str);      
            String strNoBlank = m.replaceAll("");      
            return strNoBlank;      
        }else {      
            return str;      
        }
    }
	
    /**
     * 检测ip地址是否正确
     * 
     * @param ip
     * @return
     */
    public static boolean checkIp(String ip){
    	return check("([0-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}",ip);
    }
    
    /**
     * 判断两个ip是否在一个网段内
     * 
     * @param ip
     * @param cidr
     * @return
     */
    public static boolean isInRange(String ip, String cidr) {
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) ;
        String[] cidrs = cidr.split("\\.");
        int ipcidrs = (Integer.parseInt(cidrs[0]) << 24)
                | (Integer.parseInt(cidrs[1]) << 16)
                | (Integer.parseInt(cidrs[2]) << 8);
        return ipAddr == ipcidrs;
    }
	
    /**
     * 检查字符串是否是整数
     * 
     * @param d
     * @return
     */
    public static boolean checkInteger(String d){
    	return check("^[0-9]+$",d);
    }
    
    public static boolean checkPoint(String d){
    	if(d.indexOf(".") >= 0){
    		return false;
		 }
    	return true;
    }
    
    /**
     * 正则表达式整个匹配
     * 
     * @param pattern
     *        正则
     * @param mather
     *        需要匹配的内容
     * @return
     */
    public static boolean check(String pattern, String mather) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(mather);
        return m.matches();
    }
	
	public static String spaceToComma(String keywords) {
        String regEx = "[' ']+"; // 一个或多个空格    
        Pattern p = Pattern.compile(regEx);    
        Matcher m = p.matcher(keywords);  
        return m.replaceAll(",").trim();  
	}
	
	/**
	 * 头像替换操作
	 * 
	 * @param str
	 *        替换头衔图片名：source=原图，avatar1=100*100，avatar2=50*50，avatar1=32*32
	 * @param replace
	 * @return
	 */
	public static String TextReplace(String str, String replace) {
		String newStr = str.replaceAll("avatar1",replace);
		return newStr;
	}
	
	public static boolean isNumeric(String str){ 
		String regular="[0-9]*";
		Pattern pattern = Pattern.compile(regular); 
		Matcher isNum = pattern.matcher(str);
		if( !isNum.matches() ){
		    return false; 
		} 
		return true; 
	}
	
    /**
     * 判断字符是否是中文
     *
     * @param c 字符
     * @return 是否是中文
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    /**
     * 判断字符串是否是乱码
     *
     * @param strName 字符串
     * @return 是否是乱码
     */
    public static boolean isMessyCode(String strName) {
    	String regular="\\s*|t*|r*|n*";
    	Pattern p = Pattern.compile(regular);
        Matcher m = p.matcher(strName);
        String after = m.replaceAll("");
        String temp = after.replaceAll("\\p{P}", "");
        char[] ch = temp.trim().toCharArray();
        float chLength = ch.length;
        float count = 0;
        for (int i = 0; i < ch.length; i++) {
            char c = ch[i];
            if (!Character.isLetterOrDigit(c)) {
                if (!isChinese(c)) {
                    count = count + 1;
                }
            }
        }
        float result = count / chLength;
        if (result > 0.4) {
            return true;
        } else {
            return false;
        }

    }
    
    /**
     * 获取搜索引擎url中的关键词
     * 
     */
    public static String obtainurlkey(String url) {    
        String urlStr = url; // 
        String textStr = "";  
        String regular="(\\?wd|&wd|\\?q|&q|\\?query|&query)=(.*?)(&|$)";
        Pattern p = Pattern.compile(regular);
        Matcher m = p.matcher(urlStr);
        while (m.find()) {
            //System.out.println(m.group(1));
            textStr = m.group(2);
        }
        return textStr;// 返回文本字符串    
    }

	
	public static String getRandom(int length) {
		String val = "";
		Random random = new Random();
		for (int i = 0; i < length; i++) {
			// 输出字母还是数字
			String charOrNum = random.nextInt(2) % 2 == 0 ? "char" : "num";
			// 字符串
			if ("char".equalsIgnoreCase(charOrNum)) {
				// 取得大写字母还是小写字母
				int choice = random.nextInt(2) % 2 == 0 ? 65 : 97;
				val += (char) (choice + random.nextInt(26));
			} else if ("num".equalsIgnoreCase(charOrNum)) { // 数字
				val += String.valueOf(random.nextInt(10));
			}
		}
		return val.toLowerCase();
	}

	/**
	 * 判断一个字符串在数组中存在几个
	 * @param baseStr
	 * @param strings
	 * @return
	 */
	public static int indexOf(String baseStr,String[] strings){
		
		if(null == baseStr || baseStr.length() == 0 || null == strings){
			return 0;
		}
		int i = 0;
		for (String string : strings) {
			boolean result = baseStr.equals(string);
			i = result ? ++i : i;
		}
		return i ;
	}
    
    /**
	 * 转换成Unicode
	 * @param str
	 * @return
	 */
    public static String toUnicode(String str) {
        String as[] = new String[str.length()];
        String s1 = "";
        for (int i = 0; i < str.length(); i++) {
        	int v = str.charAt(i);
        	if(v >=19968 && v <= 171941){
	            as[i] = Integer.toHexString(str.charAt(i) & 0xffff);
	            s1 = s1 + "\\u" + as[i];
        	}else{
        		 s1 = s1 + str.charAt(i);
        	}
        }
        return s1;
     }

    /**
     * 合并数据
     * @param v
     * @return
     */
    public static String merge(Object...v){
    	StringBuffer sb = new StringBuffer();
    	for (int i = 0; i < v.length; i++) {
    		sb.append(v[i]);
		}
    	return sb.toString() ; 
    }
    /**
     * 字符串转urlcode
     * @param value
     * @return
     */
    public static String strToUrlcode(String value){
    	try {
			value = java.net.URLEncoder.encode(value,"utf-8");
			return value ;
		} catch (UnsupportedEncodingException e) {
			logger.error( "字符串转换为URLCode失败,value:" + value,e);
			e.printStackTrace();
			return null;
		}    
    }
    /**
     * urlcode转字符串
     * @param value
     * @return
     */
    public static String urlcodeToStr(String value){
    	try {
			value = java.net.URLDecoder.decode(value,"utf-8");
			return value ;
		} catch (UnsupportedEncodingException e) {
			logger.error("URLCode转换为字符串失败;value:" + value,e);
			e.printStackTrace();
			return null;
		}  
    }
    /**
     * 判断字符串是否包含汉字
     * @param txt
     * @return
     */
    public static Boolean containsCN(String txt){
    	if(StringUtils.isBlank(txt)){
    		return false;
    	}
    	for (int i = 0; i < txt.length(); i++) { 

    		String bb = txt.substring(i, i + 1); 

    		boolean cc = Pattern.matches("[\u4E00-\u9FA5]", bb);
    		if(cc){
	    		return cc ;
    		}
    	}
		return false;
    }
    /**
     * 去掉HTML代码
     * @param news
     * @return
     */
    public static String removeHtml(String news) {
      String s = news.replaceAll("amp;", "").replaceAll("<","<").replaceAll(">", ">");
      String regular="<(span)?\\sstyle.*?style>|(span)?\\sstyle=.*?>";
      Pattern pattern = Pattern.compile(regular, Pattern.DOTALL);
      Matcher matcher = pattern.matcher(s);
      String str = matcher.replaceAll("");
      String regular1="(<[^>]+>)";
      Pattern pattern2 = Pattern.compile(regular1,Pattern.DOTALL);
      Matcher matcher2 = pattern2.matcher(str);
      String strhttp = matcher2.replaceAll(" ");
      
      
      String regEx = "(((http|https|ftp)(\\s)*((\\:)|：))(\\s)*(//|//)(\\s)*)?"
         + "([\\sa-zA-Z0-9(\\.|．)(\\s)*\\-]+((\\:)|(:)[\\sa-zA-Z0-9(\\.|．)&%\\$\\-]+)*@(\\s)*)?"
         + "("
         + "(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])"
         + "(\\.|．)(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)"
         + "(\\.|．)(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)"
         + "(\\.|．)(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])"
         + "|([\\sa-zA-Z0-9\\-]+(\\.|．)(\\s)*)*[\\sa-zA-Z0-9\\-]+(\\.|．)(\\s)*[\\sa-zA-Z]*"
         + ")"
         + "((\\s)*(\\:)|(：)(\\s)*[0-9]+)?"
         + "(/(\\s)*[^/][\\sa-zA-Z0-9\\.\\,\\?\\'\\\\/\\+&%\\$\\=~_\\-@]*)*";
      Pattern p1 = Pattern.compile(regEx,Pattern.DOTALL);
      Matcher matchhttp = p1.matcher(strhttp);
      String strnew = matchhttp.replaceAll("").replaceAll("(if[\\s]*\\(|else|elseif[\\s]*\\().*?;", " ");
      
      String regular2="(&[^;]+;)";
      Pattern patterncomma = Pattern.compile(regular2,Pattern.DOTALL);
      Matcher matchercomma = patterncomma.matcher(strnew);
      String strout = matchercomma.replaceAll(" ");
      String answer = strout.replaceAll("[\\pP‘’“”]", " ")
        .replaceAll("\r", " ").replaceAll("\n", " ")
        .replaceAll("\\s", " ").replaceAll("　", "");

      
      return answer;
    }
    /**
	 * 把数组的空数据去掉
	 * @param array
	 * @return
	 */
	public static List<String> array2Empty(String[] array){
		List<String> list = new ArrayList<String>();
		for (String string : array) {
			if(StringUtils.isNotBlank(string)){
				list.add(string);
			}
		}
		return list;
	}
	/**
	 * 把数组转换成set
	 * @param array
	 * @return
	 */
	public static Set<?> array2Set(Object[] array) {
		Set<Object> set = new TreeSet<Object>();
		for (Object id : array) {
			if(null != id){
				set.add(id);
			}
		}
		return set;
	}
	
	/**
	 * 查找数组内是否包含该字符串
	 * 
	 * @param array
	 *        String类型数组 
	 * @param str
	 *        查找的字符串
	 * @return
	 */
	public static boolean arraySearchStr(String[] array, String str) {
	    for(String s: array){
	        if(s.equals(str)){
	            return true;
	        }
	    }
	    return false;
	}	
	
	/**
	 * @param str
	 *        截取的字符串
	 * @param subSLength
	 *        需要截取的数量
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static String subStr(String str, int subSLength)  
            throws UnsupportedEncodingException{ 
        if (str == null)  
            return "";  
        else{ 
            int tempSubLength = subSLength;//截取字节数
            String subStr = str.substring(0, str.length()<subSLength ? str.length() : subSLength).trim();//截取的子串  
            int subStrByetsL = subStr.getBytes("GBK").length;//截取子串的字节长度 
            //int subStrByetsL = subStr.getBytes().length;//截取子串的字节长度 
            // 说明截取的字符串中包含有汉字  
            while (subStrByetsL > tempSubLength){  
                int subSLengthTemp = --subSLength;
                subStr = str.substring(0, subSLengthTemp>str.length() ? str.length() : subSLengthTemp);  
                subStrByetsL = subStr.getBytes("GBK").length;
                //subStrByetsL = subStr.getBytes().length;
            }  
            return subStr; 
        }
	}
	
	/**
	 * 把骆驼命名法的变量，变为大写字母变小写且之前加下划线
	 * 
	 * @param str
	 * @return
	 */
	public static String toUnderline(String str) {
		str = StringUtils.uncapitalize(str);
		char[] letters = str.toCharArray();
		StringBuilder sb = new StringBuilder();
		for (char letter : letters) {
			if (Character.isUpperCase(letter)) {
				sb.append("_" + letter + "");
			} else {
				sb.append(letter + "");
			}
		}
		return StringUtils.lowerCase(sb.toString());
	}
	  /**
	   * Get cookie value from request
	   *
	   * @param request
	   * @param name
	   * @return
	   */
	  public static String getCookie(HttpServletRequest request, String name) {
	    Cookie[] cookies = request.getCookies();
	    if (cookies != null) {
	      for (Cookie cookie : cookies) {
	        if (cookie.getName().equals(name)) {
	          return cookie.getValue();
	        }
	      }
	    }
	    return null;
	  }

	  /**
	   * Set cookie
	   *
	   * @param response
	   * @param name
	   * @param value
	   * @param params   maxAge, httpOnly, domain, path, secure
	   */
	  public static void setCookie(HttpServletResponse response, String name, String value, Object... params) {
	    Cookie cookie = new Cookie(name, value);
	    if (params.length >= 1) cookie.setMaxAge((Integer) params[0]); //second
	    if (params.length >= 2) cookie.setHttpOnly((Boolean) params[1]);
	    if (params.length >= 3) cookie.setDomain((String) params[2]);
	    if (params.length >= 4) cookie.setPath((String) params[3]);
	    if (params.length >= 5) cookie.setSecure((Boolean) params[4]);
	    response.addCookie(cookie);
	  }
}
