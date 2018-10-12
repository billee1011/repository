package com.lingyu.common.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ObjectUtil {

    /**
     * object 转换为long<br>
     * 若为空的时候返回0
     */
    public static long obj2long(Object obj) {
        return obj2long(obj, 0l);
    }

    /**
     * object 转换为long<br>
     *
     * @param defaultVal
     *            若为空的时候返回值
     * @return
     */
    public static long obj2long(Object obj, long defaultVal) {

        if (obj == null) {
            return 0l;
        }

        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        } else {
            if (strIsEmpty(obj.toString())) {
                return defaultVal;
            }
            try {
                return Long.parseLong(obj.toString().trim());
            } catch (Exception e) {
                return defaultVal;
            }
        }
    }

    public static int obj2int(Object o) {
        return obj2int(o, 0);
    }

    /**
     * 对象转换为int
     *
     * @param defaultVal
     *            转换失败的默认值
     */
    public static int obj2int(Object val, int defaultVal) {
        if (val == null) {
            return defaultVal;
        }

        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val instanceof Boolean) {
            if ((Boolean) val) {
                return 1;
            }
            return 0;
        } else {
            String temp = val.toString().trim();
            if (strIsEmpty(temp)) {
                return defaultVal;
            }
            try {
                return Integer.parseInt(temp);
            } catch (Exception e) {
                throw new RuntimeException("转换int值失败--" + val);
                // return defaultVal;
            }
        }
    }

    /**
     * 把客户端发过来的Double的值转换成 long
     *
     * @param vo
     *            double
     * @return long
     */
    public static long transformVo(Object vo) {
        return ConvertObjectUtil.arg2long(vo);
    }

    /**
     * 把某个值向上四舍五入
     *
     * @param vo
     *            转化值
     * @return 取整浮点型值
     */
    public static float exactValue(float vo) {
        BigDecimal bDec = new BigDecimal(vo);
        bDec = bDec.setScale(0, BigDecimal.ROUND_HALF_UP);
        return bDec.floatValue();
    }

    /**
     * 判断字符串是否为空<br>
     * null, "", "  "均返回true
     *
     * @param obj
     * @return
     */
    public static boolean strIsEmpty(String str) {
        if (str == null || str.trim().length() == 0) {
            return true;
        }
        return false;
    }

    public static String double2Str(Object obj) {
        if (obj instanceof Double) {
            return String.valueOf(((Double) obj).longValue());
        } else if (obj instanceof Float) {
            return String.valueOf(((Float) obj).longValue());
        } else if (obj instanceof Long) {
            return ((Long) obj).toString();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).toString();
        }
        return obj.toString();
    }

    /**
     * 判断时间是否为当天
     */
    public static boolean dayIsToday(Long time) {
        if (time == null || time.equals(0L)) {
            return false;
        }
        return dayIsToday(new Timestamp(time));
    }

    /**
     * 判断时间是否为当天
     */
    public static boolean dayIsToday(Timestamp time) {
        if (time == null)
            return false;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String day = sdf.format(new Date(time.getTime()));

        String today = sdf.format(new Date(System.currentTimeMillis()));

        return today.equals(day);
    }

    private static final long MILLDAY = 24 * 60 * 60 * 1000l;

    /**
     * 计算两个日期的相差天数
     *
     * @param c1
     * @param c2
     * @return
     */
    public static int twoDaysDiffence(Calendar c1, Calendar c2) {
        c1.add(Calendar.HOUR_OF_DAY, 8);// 由于当前时区与格林威治时间所处时区相差8小时，所以需加上8小时
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

        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

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
     * 把字符串List转换为逗号分隔的字符串
     *
     * @param strList
     * @return
     */
    public static String convertList2Str(List<String> strList) {
        if (strList == null || strList.size() == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (String str : strList) {
            builder.append(",").append(str);
        }
        return builder.substring(1);
    }

}