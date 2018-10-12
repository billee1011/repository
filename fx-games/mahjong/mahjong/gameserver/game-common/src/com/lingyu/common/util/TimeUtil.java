package com.lingyu.common.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lingyu.common.constant.TimeConstant;

/**
 * 处理时间的类
 */
public class TimeUtil {
	private static final Logger logger = LogManager.getLogger(TimeUtil.class);
	public static long SECOND = 1000L;
	public static long MINUTE = 60 * SECOND;
	public static long HOUR = 60 * MINUTE;
	public static long DAY = 24 * HOUR;
	public static long MONTH = 30 * DAY;

	/** 自然一月值 */
	public static final int NATURE_JANUARY = 1;
	/** 自然十二月值 */
	public static final int NATURE_DECEMBER = 12;

	// public static DateFormat df = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	// public static final String DATE_PATTERN_DATE_TIME2 = "yyyyMMddHHmmss";
	// public static final String DATE_PATTERN_DATE_TIME =
	// "yyyy-MM-dd HH:mm:ss";
	public static final String PATTERN_yyyy_MM_dd = "yyyy-MM-dd";
	public static final String PATTERN_yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
	public static final String PATTERN_yyyyMMddHHmmss = "yyyyMMddHHmmss";
	public static final String PATTERN_yyyyMMdd = "yyyyMMdd";
	public static final String PATTERN_FORMAT5 = "yyyy/MM/dd/HH:mm:ss";
	public static final String PATTERN_yyyyMM = "yyyyMM";

	public static int getToday() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int ret = year * 10000 + (month + 1) * 100 + day;
		return ret;
	}

	/**
	 * 当天还剩多少秒
	 */
	public static int spaceTime() {
		Calendar c = Calendar.getInstance();
		long now = c.getTimeInMillis();
		c.add(Calendar.DAY_OF_MONTH, 1);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis() - now;
		return (int) (millis / 1000);
	}

	/**
	 * 时区修正值<br>
	 * <li>东时区为正值</li>
	 * <li>西时区是负值</li>
	 * 
	 * @return 时区偏移值(毫秒)
	 */
	private static long getTimeZoneRawOffset() {
		return TimeZone.getDefault().getRawOffset();
	}

	/**
	 * @param lasttime
	 * @return true: 对比lastTime超过24小时 false 对比lastTime不到24小时
	 */
	public static boolean checkTime(Date lastTime) {
		return DateUtils.addHours(lastTime, 24).before(new Date());
	}

	public static String format(Date date, String pattern) {
		return DateFormatUtils.format(date, pattern);
		// return (new SimpleDateFormat(pattern)).format(date);
	}

	public static Date parse(String str, String pattern) {
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		Date date = null;
		try {
			date = format.parse(str);
		} catch (ParseException e) {
			logger.error(e.getMessage(), e);
		}
		return date;
	}

	public static int subDateToMonth(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return (int) ((time) / MONTH);
	}

	public static int subDateToMonth(Date minDate, Date maxDate) {
		return subDateToMonth(minDate.getTime(), maxDate.getTime());
	}

	public static int subDateToDay(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return (int) ((time) / DAY);
	}

	/***
	 * 相差的天数。包含跨年的
	 * 
	 * @param minDate
	 * @param maxDate
	 * @return
	 */
	public static int subDateToDay(Date minDate, Date maxDate) {
		return subDateToDay(minDate.getTime(), maxDate.getTime());
	}

	/** 获取两个时间的时间差，精度为天 */
	public static int getIntervalDay(Date minDate, Date maxDate) {
		// 需要时区修正
		return (int) ((maxDate.getTime() + getTimeZoneRawOffset()) / DAY
		        - (minDate.getTime() + getTimeZoneRawOffset()) / DAY);
	}

	public static int SubDateToHour(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return (int) (time / HOUR);
	}

	public static int subDateToMin(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return (int) (time / MINUTE);
	}

	public static int subDateToMin(Date minDate, Date maxDate) {
		return subDateToMin(minDate.getTime(), maxDate.getTime());
	}

	public static int subDateToSecond(Date minDate, Date maxDate) {
		return subDateToSecond(minDate.getTime(), maxDate.getTime());
	}

	public static int subDateToSecond(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return (int) (time / SECOND);
	}

	public static long subDateToMilliSecond(Date minDate, Date maxDate) {
		return subDateToMilliSecond(minDate.getTime(), maxDate.getTime());
	}

	public static long subDateToMilliSecond(long minDate, long maxDate) {
		long time = maxDate - minDate;
		time = time > 0 ? time : 0;
		return time;
	}

	public static int getYear() {
		Calendar c = Calendar.getInstance();
		return c.get(Calendar.YEAR);
	}

	/**
	 * 获取当前系统时间点(0-24小时)
	 * 
	 * @return
	 */
	public static int getHour() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	// ===============================================================
	// 更新刷新时间为21点
	// ===============================================================
	public static int getHourOfDay(Calendar calendar) {
		return calendar.get(Calendar.HOUR_OF_DAY);
	}

	public static int getCurHourOfDay() {
		return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
	}

	// public static int getFormatDate(long date) {
	// return Integer.parseInt(formatDateByPattern(new Date(date), "yyyyMMdd"));
	// }

	/**
	 * 检测date是不是long ago的事件 误差在1s以内的都算long ago的时间
	 * 
	 * @param date
	 * @return
	 */
	public static boolean isLongAgo(Date date) {
		long time = date.getTime();
		return Math.abs(time - TimeConstant.LONG_AGO) <= SECOND;
	}

	/**
	 * 判断两个日期是不是同一个月份
	 * 
	 * @param cal1
	 * @param cal2
	 * @return
	 */
	public static boolean isSameMonth(Calendar cal1, Calendar cal2) {
		int year1 = cal1.get(Calendar.YEAR);
		int year2 = cal2.get(Calendar.YEAR);
		int month1 = cal1.get(Calendar.MONTH);
		int month2 = cal2.get(Calendar.MONTH);
		return (year1 == year2) && (month1 == month2);
	}

	public static boolean isSameMonthByDate(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date1);
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date2);
		return isSameMonth(cal1, cal2);
	}

	public static boolean isSameMonth(Date date1, Date date2) {
		return DateUtils.truncatedEquals(date1, date2, Calendar.MONTH);
	}

	/**
	 * 是否是同一个星期
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isSameWeek(Date date1, Date date2) {
		Date monday1 = getMondayOfThisWeek(date1);
		Date monday2 = getMondayOfThisWeek(date2);
		return DateUtils.isSameDay(monday1, monday2);
	}

	/**
	 * 返回本周周一的时间<br>
	 * 按[周一为一周的第一天]计算
	 */
	public static Date getMondayOfThisWeek(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		int weekday1 = cal.get(Calendar.DAY_OF_WEEK);
		if (weekday1 == Calendar.SUNDAY) {
			weekday1 = Calendar.SATURDAY + 1;
		}
		return DateUtils.addDays(cal.getTime(), -weekday1 + Calendar.MONDAY);
	}

	/**
	 * 返回带验证日期是否与当前日期为同一个星期
	 * 
	 * @return true.同一个星期
	 */
	public static boolean isSameWeek(Date date) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(new Date());
		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date);

		int week1 = cal1.get(Calendar.WEEK_OF_YEAR);
		int week2 = cal2.get(Calendar.WEEK_OF_YEAR);
		return week1 == week2;
	}

	/**
	 * 根据传过来的时,分 获取当前时间戳
	 * 
	 * @param time
	 *            格式 HH:mm
	 * @return
	 */
	public static long getCurTime(String time) {
		String strs[] = time.split(":");
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(strs[0]));
		cal.set(Calendar.MINUTE, Integer.valueOf(strs[1]));
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	/**
	 * 根据传过来的时间算出到当月末相差几天 不包含传过来的那个时间，例如：2016-01-07 到 2016-01-31 = 24
	 * 
	 * @param date
	 * @return
	 */
	public static int getMonthEnd(Date date) {
		Calendar cal1 = Calendar.getInstance();
		cal1.setTime(date);

		Calendar cal2 = Calendar.getInstance();
		cal2.setTime(date);
		cal2.set(Calendar.DAY_OF_MONTH, cal2.getActualMaximum(Calendar.DAY_OF_MONTH));

		return cal2.get(Calendar.DAY_OF_MONTH) - cal1.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 根据参数获取相应的时间
	 * 
	 * @param date
	 * @param addDay
	 * @param hour
	 * @param minute
	 * @return
	 */
	public static long getTimeByPara(Date date, int addDay, int hour, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, addDay);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	/**
	 * 获取一天的开始时间，比如2016-02-24 00:00:00
	 * 
	 * @param date
	 * @param addDay
	 *            添加的天数不算date当天 比如2016-02-24 00:00:00 +2天，得到的时间为2016-02-26
	 *            00:00:00
	 * @return
	 */
	public static long getTimeStart(Date date, int addDay) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, addDay);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	/**
	 * 获取一天的开始时间，比如2016-02-24 00:00:00
	 * 
	 * @param date
	 * @param addDay
	 *            添加的天数不算date当天 比如2016-02-24 00:00:00 +2天，得到的时间为2016-02-26
	 *            00:00:00
	 * @return
	 */
	public static long getTimeEnd(Date date, int addDay) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.DAY_OF_MONTH, addDay);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTimeInMillis();
	}

	public static void main(String[] args) {
		Date now = new Date();
		int n = TimeUtil.getMonthEnd(now);
		System.out.println(n);

		// Date date2 = DateUtils.addDays(now, -1);
		// System.out.println(isSameWeek(now, date2));

		// System.out.println(isSameMonth(now, date2));
		// System.out.println(now.getTime() / DAY);
		// System.out.println((DateUtils.addDays(now, 2)).getTime() / DAY);

		// System.err.println(getMondayOfThisWeek(date2).toString());

		// Date date_1 = DateUtils.addDays(now, -1);
		// Date date_2 = DateUtils.addDays(now, -2);
		// Date date_3 = DateUtils.addDays(now, -3);
		// Date date_4 = DateUtils.addDays(now, -4);
		// Date date_5 = DateUtils.addDays(now, -5);
		// Date date_6 = DateUtils.addDays(now, -6);
		// Date date_7 = DateUtils.addDays(now, -7);
		// Date date0 = new Date();
		// Date date1 = DateUtils.addDays(now, 1);
		// Date date2 = DateUtils.addDays(now, 2);
		// Date date3 = DateUtils.addDays(now, 3);
		// Date date4 = DateUtils.addDays(now, 4);
		// Date date5 = DateUtils.addDays(now, 5);
		// Date date6 = DateUtils.addDays(now, 6);
		// Date date7 = DateUtils.addDays(now, 7);
		//
		// System.err.println("date_1--->"+isSameWeek(date_1)+"\t");
		// System.err.println("date_2--->"+isSameWeek(date_2)+"\t");
		// System.err.println("date_3--->"+isSameWeek(date_3)+"\t");
		// System.err.println("date_4--->"+isSameWeek(date_4)+"\t");
		// System.err.println("date_5--->"+isSameWeek(date_5)+"\t");
		// System.err.println("date_6--->"+isSameWeek(date_6)+"\t");
		// System.err.println("date_7--->"+isSameWeek(date_7)+"\t");
		//
		// System.out.println("==================分割线========================");
		// System.err.println("date0--->"+isSameWeek(date0)+"\t");
		// System.err.println("date1--->"+isSameWeek(date1)+"\t");
		// System.err.println("date2--->"+isSameWeek(date2)+"\t");
		// System.err.println("date3--->"+isSameWeek(date3)+"\t");
		// System.err.println("date4--->"+isSameWeek(date4)+"\t");
		// System.err.println("date5--->"+isSameWeek(date5)+"\t");
		// System.err.println("date6--->"+isSameWeek(date6)+"\t");
		// System.err.println("date7--->"+isSameWeek(date7)+"\t");
		//
	}
}
