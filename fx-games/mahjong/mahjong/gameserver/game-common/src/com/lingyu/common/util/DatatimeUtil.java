package com.lingyu.common.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatatimeUtil {

	private static final long MILLDAY = 24 * 60 * 60 * 1000l;

	/**
	 * 计算两个日期的相差天数
	 * 
	 * @param c1
	 * @param c2
	 * @return
	 */
	public static int twoDaysDiffence(Calendar c1, Calendar c2) {
		c1.add(Calendar.HOUR_OF_DAY, 8);// 由于当前时区与格林威治时间所处时区不一致，所以需加上8小时
		c2.add(Calendar.HOUR_OF_DAY, 8);

		long time1 = c1.getTimeInMillis() / MILLDAY;
		long time2 = c2.getTimeInMillis() / MILLDAY;

		return new Long(Math.abs(time1 - time2)).intValue();
	}

	/**
	 * 计算日期1距离今天的天数
	 * 
	 * @param c1
	 * @return
	 */
	public static int twoDaysDiffence(Calendar c1) {
		c1.add(Calendar.HOUR_OF_DAY, 8);

		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(System.currentTimeMillis());

		return twoDaysDiffence(c1, c2);
	}

	/**
	 * 计算日期1距离今天的天数(返回的始终是正数)
	 * 
	 * @param c1
	 * @return
	 */
	public static int twoDaysDiffence(Date date1) {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(date1);

		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis(System.currentTimeMillis());

		return twoDaysDiffence(c1, c2);
	}

	/**
	 * 获取当日指定时间的毫秒数
	 * 
	 * @param hour
	 * @param minute
	 * @return
	 */
	public static long getTheTime(int hour, int minute) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}

	/**
	 * 获取当周指定时间的毫秒数<br>
	 * PS: [星期日]为每周的[第一天]
	 * 
	 * @param week
	 *            1~7依次为星期一到星期天
	 * @param hour
	 * @param minute
	 * @return
	 */
	public static long getTheWeekTime(int week, int hour, int minute) {
		// java 1~7依次为星期天到星期六
		int[] javaWeek = new int[] { 2, 3, 4, 5, 6, 7, 1 };
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, javaWeek[week - 1]);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, minute);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTimeInMillis();
	}

	/**
	 * 获取diff天后的日期
	 * 
	 * @param date
	 * @param diff
	 * @return
	 */
	public static Date getDiffDay(Date date, int diff) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		cal.add(Calendar.DAY_OF_YEAR, diff);

		return cal.getTime();
	}

	/**
	 * 返回date1,date2相隔天数
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static int twoDaysDiffence(Date date1, Date date2) {
		long time1 = date1.getTime();
		long time2 = date2.getTime();

		long l1 = time1 / (24 * 60 * 60 * 1000l);
		long l2 = time2 / (24 * 60 * 60 * 1000l);

		// System.out.println("time1:"+time1 + "\t" + l1);
		// System.out.println("time2:"+time2 + "\t" + l2);
		return new Long(Math.abs(l1 - l2)).intValue();
	}

	/**
	 * 获取明日凌晨的毫秒数
	 * 
	 * @return
	 */
	public static long getTomorrow00Time() {
		Calendar cal = Calendar.getInstance();
		// System.out.println(cal.toString());
		cal.add(Calendar.DAY_OF_YEAR, 1);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		// System.out.println( cal.toString() );

		return cal.getTimeInMillis();
	}

	/**
	 * 获取date凌晨0:0:0的毫秒数
	 * 
	 * @return
	 */
	public static long getDate00Time(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		// System.out.println( cal.toString() );

		return cal.getTimeInMillis();
	}

	/**
	 * 获取跟当前时间的时间差(s)
	 * 
	 * @param str
	 *            09:50
	 * @return
	 */
	public static int getSpaceDate(String str) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());

		Calendar cal2 = Calendar.getInstance();
		String[] strs = str.split(":");
		cal2.set(Calendar.HOUR_OF_DAY, Integer.parseInt(strs[0]));
		cal2.set(Calendar.MINUTE, Integer.parseInt(strs[1]));

		return (int) ((cal.getTimeInMillis() - cal2.getTimeInMillis()) / 1000);
	}

	/**
	 * 获取指定格式当日的时间(毫秒)
	 * 
	 * @param timeStr
	 *            格式[19:50]
	 * @return
	 */
	public static Date getParaTodayTime(String timeStr) {
		Calendar calendar = Calendar.getInstance();
		String[] timeStrArray = timeStr.split(":");
		calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeStrArray[0]));
		calendar.set(Calendar.MINUTE, Integer.parseInt(timeStrArray[1]));

		return calendar.getTime();
	}

	/**
	 * 格式yyyy-MM-dd HH:mm:ss转换为毫秒数
	 * 
	 * @param timeStr
	 * @return
	 */
	public static long parseDateMillTime(String timeStr) {
		if (timeStr == null || timeStr.trim().isEmpty()) {
			return 0;
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			Date date = dateFormat.parse(timeStr);
			return date.getTime();
		} catch (Exception e) {
		}
		return 0;
	}

	public static String parseDateToString(Date time) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateFormat.format(time);
	}

	/**
	 * 格式yyyy/MM/dd/HH:mm:ss
	 * 
	 * @param timeStr
	 * @return
	 */
	public static long parseDate4(String timeStr) {
		if (timeStr == null || timeStr.trim().isEmpty()) {
			return 0;
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH:mm:ss");

		try {
			Date date = dateFormat.parse(timeStr);
			return date.getTime();
		} catch (Exception e) {
		}
		return 0;
	}

	/**
	 * 在date时间上增加addSeconds秒
	 * 
	 * @param date
	 * @param addSeconds
	 * @return
	 */
	public static long addSeconds(Date date, long addSeconds) {
		long result = date.getTime();

		return result + addSeconds * 1000;
	}

	/**
	 * 在date时间上增加addMinutes秒
	 * 
	 * @param date
	 * @param addSeconds
	 * @return
	 */
	public static long addMinutes(Date date, long addMinutes) {
		long result = date.getTime();

		return result + addMinutes * 1000 * 60;
	}

	/**
	 * 在date时间上增加adDdays天
	 * 
	 * @param date
	 * @param addSeconds
	 * @return
	 */
	public static Date addDays(Date date, Long adDdays) {
		long result = date.getTime();
		result = result + adDdays * 24 * 60 * 60 * 1000;
		return new Date(result);
	}

	/**
	 * 比较2个时间相差的分钟<br>
	 * 格式 time2(21:30)-time1(20:30) = 60
	 * 
	 * @param time1
	 * @param time2
	 * @return
	 */
	public static int compare2TimesDiffMinutes(String time1, String time2) {
		if (ObjectUtil.strIsEmpty(time1) || ObjectUtil.strIsEmpty(time2)) {
			throw new IllegalArgumentException();
		}
		String[] str1s = time1.split(":");
		String[] str2s = time2.split(":");

		int hour1 = Integer.valueOf(str1s[0]);
		int minute1 = Integer.valueOf(str1s[1]);
		int hour2 = Integer.valueOf(str2s[0]);
		int minute2 = Integer.valueOf(str2s[1]);

		return (hour2 - hour1) * 60 + (minute2 - minute1);
	}

	public static void main(String[] args) {

		// long l = getTomorrow00Time();
		// System.out.println( new Date(l) );
		//
		// long curMills = System.currentTimeMillis();
		// System.out.println("time0:"+curMills);
		// Calendar c1 = Calendar.getInstance();
		// c1.set(2012, 4, 22, 23, 58, 59);
		//
		// Calendar c2 = Calendar.getInstance();
		// c2.set(2012, 4, 23, 0, 0, 0);
		//
		// System.out.println( twoDaysDiffence(c1, c2) );
		//
		// Date date1 = c1.getTime();
		// Date date2 = c2.getTime();
		//
		// int i = twoDaysDiffence(date1, date2);
		// System.out.println(i);
		//
		// DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		// System.out.println( "date1:" + df.format(date1) + "\tdate2:" +
		// df.format(date2) );
		//
		// c1.add(Calendar.DAY_OF_MONTH, i);
		// System.out.println( df.format(c1.getTime()) );
		//
		//
		// System.out.println(addDays(new Date(), 2l));

		// System.out.println(formatTime(System.currentTimeMillis(),
		// "yyyy-MM-dd HH:mm:ss"));
		// System.out.println(formatTime(System.currentTimeMillis(),
		// "ss mm HH dd MM ? yyyy"));
		// System.out.println(getInterval(new Date(1356050885598l), new Date(),
		// Calendar.MONTH));

		System.out.println(getSpaceDate("14:53"));

	}

	/**
	 * 比较时间间隔(根据具体时间单位)
	 */
	public static int getInterval(Date date1, Date date2, int calanderUnit) {

		Calendar c1 = Calendar.getInstance();
		c1.setTime(date1);

		Calendar c2 = Calendar.getInstance();
		c2.setTime(date2);

		return Math.abs(c2.get(calanderUnit) - c1.get(calanderUnit));
	}

	public static String formatTime(long time1, String pattern) {
		DateFormat df = new SimpleDateFormat(pattern);

		return df.format(time1);
	}

	/**
	 * 判断date1和date2是否在同一周<br>
	 * java 星期日每周的第一天, 1~7依次为星期天到星期六
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static boolean isSameWeek(Date date1, Date date2) {
		Calendar cal1 = Calendar.getInstance();
		Calendar cal2 = Calendar.getInstance();
		cal1.setTime(date1);
		cal2.setTime(date2);

		// subYear==0, 说明是同一年
		int subYear = cal1.get(Calendar.YEAR) - cal2.get(Calendar.YEAR);
		if (subYear == 0) {
			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)) {
				return true;
			}
		} else if (subYear == 1 && cal2.get(Calendar.MONTH) == 11) {
			// 例子:cal1是"2005-1-1"，cal2是"2004-12-25"
			// java对"2004-12-25"处理成第52周
			// "2004-12-26"它处理成了第1周，和"2005-1-1"相同了
			// 说明:java的一月用"0"标识，那么12月用"11"

			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)) {
				return true;
			}
		} else if (subYear == -1 && cal1.get(Calendar.MONTH) == 11) {
			// 例子:cal1是"2004-12-31"，cal2是"2005-1-1"
			if (cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)) {
				return true;
			}
		}
		return false;
	}
}
