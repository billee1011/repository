package com.cai.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 时间工具类
 * 
 * @author run
 *
 */
public class MyDateUtil {

	/**
	 * 相关分钟数
	 * @param smdate
	 * @param bdate
	 * @return
	 * @throws ParseException
	 */
	public static int minuteBetween(Date smdate, Date bdate) throws ParseException {
		long between_days = (bdate.getTime() - smdate.getTime()) / (1000*60L);
		return (int)between_days;
	}
	
	
	/**
	 * 相关小时数
	 * @param smdate
	 * @param bdate
	 * @return
	 * @throws ParseException
	 */
	public static int hourBetween(Date smdate, Date bdate) throws ParseException {
		long between_days = (bdate.getTime() - smdate.getTime()) / (1000*60*60L);
		return (int)between_days;
	}
	
	/**
	 * 计算两个日期之间相差的天数
	 * 
	 * @param smdate
	 *            较小的时间
	 * @param bdate
	 *            较大的时间
	 * @return 相差天数
	 * @throws ParseException
	 */
	public static int daysBetween(Date smdate, Date bdate) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		smdate = sdf.parse(sdf.format(smdate));
		bdate = sdf.parse(sdf.format(bdate));
		Calendar cal = Calendar.getInstance();
		cal.setTime(smdate);
		long time1 = cal.getTimeInMillis();
		cal.setTime(bdate);
		long time2 = cal.getTimeInMillis();
		long between_days = (time2 - time1) / (1000 * 3600 * 24);

		return Integer.parseInt(String.valueOf(between_days));
	}

	/**
	 * 零点的时间
	 * @param date
	 * @return
	 */
	public static Date getZeroDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY , 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
		
	}

	/**
	 * 明天的零点的时间
	 * @param date
	 * @return
	 */
	public static Date getTomorrowZeroDate(Date date){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR_OF_DAY , 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, 1);
		return cal.getTime();
	}
	
	public static Date getNow(){
		return new Date();
	}
	static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	public static Date parse(String datestr){
		try {
			return dateFormat.parse(datestr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return new Date();
	}
	/**
	* 获取日期年份
	* @param date
	* @return
	* @throws ParseException
	*/
	public static int getYear(String date) throws ParseException{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateFormat.parse(date));
		return calendar.get(Calendar.YEAR);
	}
	/**
	* 获取日期月份
	* @param date
	* @return
	* @throws ParseException
	*/
	public static int getMonth(String date) throws ParseException{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateFormat.parse(date));
		return (calendar.get(Calendar.MONTH) + 1);
	}
	/**
	* 获取月份起始日期
	* @param date
	* @return
	* @throws ParseException
	*/
	public static Date getMinMonthDate(String date) throws ParseException{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateFormat.parse(date));
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
		return calendar.getTime();
	}
	/**
	* 获取月份最后日期
	* @param date
	* @return
	* @throws ParseException
	*/
	public static Date getMaxMonthDate(String date) throws ParseException{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateFormat.parse(date));
		calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
		return calendar.getTime();
	}
	
	/**
	* 获取20171010  0点时间
	* @param date
	* @return
	* @throws ParseException
	*/
	public static Date getZeroDate(String date) throws ParseException{
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(dateFormat.parse(date));
		return calendar.getTime();
	}
	
	
	public static void main(String[] args) throws ParseException{
//		System.out.println(getMaxMonthDate("201505"));
		
		System.out.println(getZeroDate("20171010"));
	}

}
