package com.flycms.core.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
/**
 *
 * Open source house, All rights reserved
 * 开发公司：28844.com<br/>
 * 版权：开源中国<br/>
 * <p>
 * 
 * 格式化时间 工具类
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
public class DateUtils {
	private final static SimpleDateFormat SDFYEAR = new SimpleDateFormat("yyyy");

	private final static SimpleDateFormat sdfDay = new SimpleDateFormat("yyyy-MM-dd");
	
	private final static SimpleDateFormat sdfDays = new SimpleDateFormat("yyyyMMdd");

	private final static SimpleDateFormat sdfTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private static final long m = 60*1000L;//分
	private static final long hour = 3600*1000L;//小时
	private static final long day = 24*hour;//天
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	/**
	 * 获取YYYY格式
	 * 
	 * @return
	 */
	public static String getYear() {
		return SDFYEAR.format(new Date());
	}

	/**
	 * 获取YYYY-MM-DD格式
	 * 
	 * @return
	 */
	public static String getDay() {
		return sdfDay.format(new Date());
	}
	
	/**
	 * 获取YYYYMMDD格式
	 * 
	 * @return
	 */
	public static String getDays(){
		return sdfDays.format(new Date());
	}

	/**
	 * 获取YYYY-MM-DD hh:mm:ss格式
	 * 
	 * @return
	 */
	public static String getTime() {
		return sdfTime.format(new Date());
	}

	/**
	* @Title: compareDate
	* @Description: TODO(日期比较，如果s>=e 返回true 否则返回false)
	* @param s
	* @param e
	* @return boolean  
	* @throws
	* @author luguosui
	 */
	public static boolean compareDate(String s, String e) {
		if(fomatDate(s)==null||fomatDate(e)==null){
			return false;
		}
		return fomatDate(s).getTime() >=fomatDate(e).getTime();
	}

	/**
	 * 格式化日期
	 * 
	 * @return
	 */
	public static Date fomatDate(String date) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			return fmt.parse(date);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * 将字符串日期转换为字符串 , 格式yyyy-MM-dd HH:mm:ss
	 * 
	 * @param date
	 *            要转换的日期
	 * 
	 * @return
	 */
	public static String fomatString(String date) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			Date d = fmt.parse(date);
			return fmt.format(d.getTime());
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 将字符串日期转换为字符串 , 格式yyyy-MM-dd HH:mm:ss
	 *
	 * @param date
	 *            要转换的日期
	 *
	 * @return
	 */
	public static String fomatDateToString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(date);
	}
	
	/**
	 * 将字符串日期转换为solr使用时间字符串 , 格式yyyy-MM-dd'T'HH:mm:ss'Z'
	 * 
	 * @param date
	 *            要转换的日期
	 * 
	 * @return
	 */
	public static String fomatSolrDate(String date) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
		try {
			Date d = sdf.parse(date);
			return fmt.format(d);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	/**
	 * 将字符串日期转换为字符串 , 格式yyyy-MM-dd
	 * 
	 * @param date
	 *            要转换的日期
	 * 
	 * @return
	 */
	public static String fomatStringDate(String date) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			Date d = sdf.parse(date);
			return fmt.format(d);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 校验日期是否合法
	 * 
	 * @return
	 */
	public static boolean isValidDate(String s) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			fmt.parse(s);
			return true;
		} catch (Exception e) {
			// 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
			return false;
		}
	}
	
	/**
	 * 校验日期是否合法,格式yyyy-MM-dd HH:mm:ss
	 * 
	 * @return
	 */
	public static boolean isValidDayDate(String s) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			fmt.parse(s);
			return true;
		} catch (Exception e) {
			// 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
			return false;
		}
	}
	
	public static int getDiffYear(String startTime,String endTime) {
		DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
		try {
			int years=(int) (((fmt.parse(endTime).getTime()-fmt.parse(startTime).getTime())/ (1000 * 60 * 60 * 24))/365);
			return years;
		} catch (Exception e) {
			// 如果throw java.text.ParseException或者NullPointerException，就说明格式不对
			return 0;
		}
	}
	  /**
     * <li>功能描述：时间相减得到天数
     * @param beginDateStr
     * @param endDateStr
     * @return
     * long 
     * @author Administrator
     */
    public static long getDaySub(String beginDateStr,String endDateStr){
        long day=0;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate = null;
        Date endDate = null;
        
            try {
				beginDate = format.parse(beginDateStr);
				endDate= format.parse(endDateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
            day=(endDate.getTime()-beginDate.getTime())/(24*60*60*1000);
            //System.out.println("相隔的天数="+day);
      
        return day;
    }
    
    /**
     * 得到n天之后的日期
     * @param days
     * @return
     */
    public static String getAfterDayDate(String days) {
    	int daysInt = Integer.parseInt(days);
    	
        Calendar canlendar = Calendar.getInstance(); // java.util包
        canlendar.add(Calendar.DATE, daysInt); // 日期减 如果不够减会将月变动
        Date date = canlendar.getTime();
        
        SimpleDateFormat sdfd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = sdfd.format(date);
        
        return dateStr;
    }
    
    /**
     * 得到n天之后是周几
     * @param days
     * @return
     */
    public static String getAfterDayWeek(String days) {
    	int daysInt = Integer.parseInt(days);
    	
        Calendar canlendar = Calendar.getInstance(); // java.util包
        canlendar.add(Calendar.DATE, daysInt); // 日期减 如果不够减会将月变动
        Date date = canlendar.getTime();
        
        SimpleDateFormat sdf = new SimpleDateFormat("E");
        String dateStr = sdf.format(date);
        
        return dateStr;
    }
    
	/**
	 * 日期时间转换成文字
	 * @param time
	 * @return
	 * @throws ParseException 
	 */
	public static String getDateTimeString(String time) throws ParseException{
		if(time==null){
			throw new NullPointerException();
		}
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date =sdf.parse(time);
		
		Date currentDate = new Date();
		long cha = Math.abs(date.getTime()-currentDate.getTime());
		long hours = cha/hour;
		if(hours<1){
			if(cha/m<=0){
				return "刚刚";
			}
			return cha/m+"分钟前";
		}
		if(hours<24){
			return cha/hour+"小时前";
		}
		if(hours<=720){
			int nn = Integer.valueOf(cha/day+"");
			if(cha%day>0){
				nn++;
			}
			return nn+"天前";
		}
		if(hours>720){
			int nn = Integer.valueOf(cha/day/30+"");
			if(cha%day%30>0){
				nn++;
			}
			return nn+"个月前";
		}
		return sdf.format(date);
	}
	
	/**
	 * 站内短信显示日期时间转换成文字
	 * @param time
	 * @return
	 * @throws ParseException 
	 */
	public static String getMessageDateTime(String time) throws ParseException{
		if(time==null){
			throw new NullPointerException();
		}
		Date newTime=new Date();   
        //将下面的 理解成  yyyy-MM-dd 00：00：00 更好理解点  
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        //格式化成时间：分：秒
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        
        String todayStr = format.format(newTime);  
        Date today = format.parse(todayStr); 
        Date oldTime = sdfTime.parse(time);
        
        String dateString = formatter.format(oldTime);
        //昨天 86400000=24*60*60*1000 一天  
        if((today.getTime()-oldTime.getTime())>0 && (today.getTime()-oldTime.getTime())<=86400000) {  
            return "昨天 "+dateString;  
        }  
        else if((today.getTime()-oldTime.getTime())<=0){ //至少是今天  
            return "今天 "+dateString;  
        } 
	   return sdfTime.format(oldTime);
	}
    

	// 加天数
	public static String addDays(int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, day);
		StringBuffer sb = new StringBuffer();
		sb.append(calendar.get(Calendar.YEAR)).append("-");
		sb.append(calendar.get(Calendar.MONTH) + 1).append("-");
		sb.append(calendar.get(Calendar.DAY_OF_MONTH));
		return sb.toString();
	}

	// 加年份
	public static String addYears(String now, int year) throws ParseException {
		Calendar fromCal = Calendar.getInstance();

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(now);
		fromCal.setTime(date);
		fromCal.add(Calendar.YEAR, year);

		return dateFormat.format(fromCal.getTime());
	}

	// 加天数(特定时间)
	public static String addDate(String now, int day) throws ParseException {
		Calendar fromCal = Calendar.getInstance();

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = dateFormat.parse(now);
		fromCal.setTime(date);
		fromCal.add(Calendar.DATE, day);

		return dateFormat.format(fromCal.getTime());
	}
	
    /**
     * 将日期转换为字符串 , 格式yyyy-MM-dd
     * @param birthDay 出生日期
     * @return
     * @throws Exception
     */
    public static String getAge(String birthDay) throws Exception {		
		SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM-dd");
		Date date =sdf.parse(birthDay);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

	    Calendar now = Calendar.getInstance();
	    int day = now.get(Calendar.DAY_OF_MONTH) - calendar.get(Calendar.DAY_OF_MONTH);
	    int month = now.get(Calendar.MONTH) - calendar.get(Calendar.MONTH);
	    int year = now.get(Calendar.YEAR) - calendar.get(Calendar.YEAR);
	    //按照减法原理，先day相减，不够向month借；然后month相减，不够向year借；最后year相减。
	    if(day<0){
	    	month -= 1;
	    	now.add(Calendar.MONTH, -1);//得到上一个月，用来得到上个月的天数。
	    	day = day + now.getActualMaximum(Calendar.DAY_OF_MONTH);
	    }
	    if(month<0){
	    	month = (month+12)%12;
	    	year--;
	    }
	    String age="";
	    if(year==0 && month==0){
	    	age += day+"天";
	    }else if(year==0){
	    	age += month+"月"+day+"天";
	    }else if(year>0 && day==0){
	    	age += year +"岁"+ month +"月";
	    }else if(year>0 && day>0){
	    	age += year+"岁"+month+"月"+day+"天";
	    }

	    return age;
	}
    
	public static String fomatCurrentDate(String dateFomat) {
		String date=null;
		if("dd".equals(dateFomat)){
			SimpleDateFormat sDateFormat =new SimpleDateFormat("dd");   
			date = sDateFormat.format(new Date());
		}else if("mm".equals(dateFomat)){
			SimpleDateFormat sDateFormat =new SimpleDateFormat("MM");   
			date = sDateFormat.format(new Date());
		}else if("yyyy".equals(dateFomat)){
			SimpleDateFormat sDateFormat =new SimpleDateFormat("yyyy");   
			date = sDateFormat.format(new Date());
		}else if("yyyymm".equals(dateFomat)){
			SimpleDateFormat sDateFormat =new SimpleDateFormat("yyyy-MM");   
			date = sDateFormat.format(new Date());
		}else if("mmdd".equals(dateFomat)){
			SimpleDateFormat sDateFormat =new SimpleDateFormat("MM.dd");   
			date = sDateFormat.format(new Date());
		}else if("week".equals(dateFomat)){
			Date time=new Date();
			String[] weeks = {"周日","周一","周二","周三","周四","周五","周六"};  
	        Calendar cal = Calendar.getInstance();  
	        cal.setTime(time);  
	        int week_index = cal.get(Calendar.DAY_OF_WEEK) - 1;  
	        if(week_index<0){  
	            week_index = 0;  
	        }   
	        date = weeks[week_index]; 
		}
		return date;
	}

	/**
	 * 将java time格式转成Solr支持的时间
	 * @param time
	 * @return
	 * @throws ParseException
	 */
	public static String getSolrDate(String time) throws ParseException {
		//格式化成 时间成  年-月-日
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
		//格式化成 时间：分：秒
		SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss");
		Date date =sdfTime.parse(time);
		String result = sdf1.format(date) + "T" + sdf2.format(date) + "Z";
		return result;
	}
}
