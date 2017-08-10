package com.javastar920905.spider.util;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Minutes;
import org.joda.time.Months;
import org.joda.time.Seconds;
import org.joda.time.Years;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 时间处理通用类
 * 需要 org.joda.time 依赖
 */
public class DateUtil extends DateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    /**
     * date转字符串
     *
     * @param date
     * @param pattern
     * @return 格式化后的日期字符串
     */
    public static String dateFormat(Date date, String pattern) {
        String dateStr = "";
        if (date != null) {
            if (StringUtil.isBlank(pattern)) {
                pattern = "yyyy-MM-dd HH:mm:ss";
            }
            dateStr = new DateTime(date).toString(pattern);
        }
        return dateStr;
    }

    /**
     * 根据指定格式获取时间
     *
     * @param time 时间字符串
     * @return Date
     * @author chenjun
     */
    public static Date transStringToDate(String time) {
        return new DateTime(time).toDate();
    }

    /**
     * 根据指定格式获取时间
     *
     * @param format 指定格式
     * @param time   时间字符串
     * @return
     */
    public static Date transStringToDate(String format, String time) {
        try {
            if (StringUtil.isNotBlank(format) && StringUtil.isNotBlank(time)) {
                SimpleDateFormat sim = new SimpleDateFormat(format);
                return sim.parse(time);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 计算两个时间间隔
     *
     * @param startDate
     * @param endDate
     * @return 返回值单位是分钟
     */
    public static long intervalDate(Date startDate, Date endDate) {
        long startMs = startDate.getTime();
        long endMs = endDate.getTime();
        long intervalMs = endMs - startMs;
        return intervalMs / (1000 * 60);
    }

    /**
     * 计算两个日期相差的月数,不计大小，结果取绝对值
     *
     * @param startDate
     * @param endDate
     * @return
     * @author liuwb
     */
    public static int intervalDateByMonth(Date startDate, Date endDate) {
        return Math
            .abs(Months.monthsBetween(new DateTime(startDate), new DateTime(endDate)).getMonths());
    }

    /**
     * 计算指定时间与当前时间相隔的天数
     *
     * @param date 指定时间
     * @return
     * @author liuwb
     * @date 2016/12/30
     */
    public static int intervalDateByDay(Date date) {
        return Math.abs(Days.daysBetween(new DateTime(date), new DateTime(new Date())).getDays());
    }

    /**
     * 计算两个日期相差的分钟数,不计大小，结果取绝对值
     *
     * @param startDate
     * @param endDate
     * @return
     * @author liuwb
     */
    public static long intervalDateByMinute(Date startDate, Date endDate) {
        return Math.abs(
            Minutes.minutesBetween(new DateTime(startDate), new DateTime(endDate)).getMinutes());
    }

    /**
     * 计算两个日期相差的小时数,不计大小，结果取绝对值
     *
     * @param startDate
     * @param endDate
     * @return
     * @author liuwb
     */
    public static long intervalDateByHour(Date startDate, Date endDate) {
        return Math
            .abs(Hours.hoursBetween(new DateTime(startDate), new DateTime(endDate)).getHours());
    }


    /**
     * 计算两个日期相差的秒数,不计大小，结果取绝对值
     *
     * @param startDate
     * @param endDate
     * @return
     * @author liuwb
     */
    public static long intervalDateBySeconds(Date startDate, Date endDate) {
        return Math.abs(
            Seconds.secondsBetween(new DateTime(startDate), new DateTime(endDate)).getSeconds());
    }

    /**
     * 计算两个日期相差的年数,不计大小，结果取绝对值
     *
     * @param startDate
     * @param endDate
     * @return
     * @author liuwb
     */
    public static int intervalDateByYears(Date startDate, Date endDate) {
        return Math
            .abs(Years.yearsBetween(new DateTime(startDate), new DateTime(endDate)).getYears());
    }

    /**
     * 判断两个日期是不是同一年
     *
     * @author liuwb
     * @date 2017/2/7
     */
    public static boolean judgeSameYear(Date startDate, Date endDate) {
        DateTime startDateTime = new DateTime(startDate);
        DateTime endDateTime = new DateTime(endDate);
        Integer startYear = startDateTime.getYear();
        Integer endYear = endDateTime.getYear();
        return startYear.equals(endYear);
    }

    /**
     * 根据日期生成cron表达式
     *
     * @param date
     * @return cron表达式
     */
    public static String generateCronExpressionByDate(Date date) {
        String cron = StringUtil.EMPTY;
        if (date != null) {
            DateTime dateTime = new DateTime(date);
            int second = dateTime.getSecondOfMinute();
            int minute = dateTime.getMinuteOfHour();
            int hour = dateTime.getHourOfDay();
            int day = dateTime.getDayOfMonth();
            int month = dateTime.getMonthOfYear();

            cron = second + " " + minute + " " + hour + " " + day + " " + month + " ?";

        }
        return cron;
    }

    /**
     * 获取当年的第n周
     *
     * @param date
     * @return 第n周
     */
    public static int getWeekOfYearByDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(date);
        return calendar.get(Calendar.WEEK_OF_YEAR);
    }


    /**
     * 获取当年的第n月
     *
     * @param date
     * @return 第n月
     */
    public static int getMonthOfYearByDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH); // 注意:月是从0开始的
        return month + 1;
    }

    /**
     * 获取当前时间的前rang分钟
     *
     * @param date
     * @param range
     * @return
     * @author huangzhk 2016/11/24
     */
    public static Date getLastFewMinutes(Date date, int range) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, -range);
        return calendar.getTime();
    }

    /**
     * 获取当前时间的前rang小时
     *
     * @param date
     * @param range
     * @return
     * @author 邱颐
     */
    public static Date getLastFewHours(Date date, int range) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, -range);
        return calendar.getTime();
    }


    /**
     * 获取当前时间的前rang天
     *
     * @param date  当前时间
     * @param range
     * @return
     * @author huangzhk
     */
    public static Date getLastFewDays(Date date, int range) {
        Date newDate = date == null ? new Date() : date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);
        calendar.add(Calendar.DAY_OF_MONTH, -range);
        return calendar.getTime();
    }

    /**
     * 获取指定日期的对应前n周日期
     *
     * @param date
     * @param range
     * @return
     * @author huangzhk
     */
    public static Date getLastFewWeeks(Date date, int range) {
        Date newDate = date == null ? new Date() : date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);
        calendar.add(Calendar.WEEK_OF_YEAR, -range);
        return calendar.getTime();
    }

    /**
     * 获取指定日期的前n年的日期
     *
     * @param date
     * @param range
     * @return
     * @author liuwb
     */
    public static Date getLastFewYear(Date date, int range) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.YEAR, -range);
        return calendar.getTime();
    }

    /**
     * 获取指定日期的前n月的日期
     *
     * @param date
     * @param range
     * @return
     * @author huangzhk
     */
    public static Date getLastFewMonths(Date date, int range) {
        if (range < 0) {
            return null;
        }
        Date newDate = date == null ? new Date() : date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);
        calendar.add(Calendar.MONTH, -range);
        return calendar.getTime();
    }

    /**
     * 获取某一天的开始时间和结束时间
     *
     * @param today
     * @return
     */
    public static Map<String, DateTime> getOneDayTime(DateTime today) {
        DateTime newDate = today == null ? new DateTime() : today;

        DateTime startTime = newDate.hourOfDay().withMinimumValue();
        startTime = startTime.minuteOfHour().withMinimumValue();
        startTime = startTime.secondOfMinute().withMinimumValue();

        DateTime endTime = newDate.hourOfDay().withMaximumValue();
        endTime = endTime.minuteOfHour().withMaximumValue();
        endTime = endTime.secondOfMinute().withMaximumValue();

        Map<String, DateTime> dateMap = new HashMap<>();

        dateMap.put("start", startTime);
        dateMap.put("end", endTime);

        return dateMap;
    }

    /**
     * 获取指定日期连续前n天内,每天的开始时间和结束时间
     *
     * @param startDate
     * @param range
     * @return Map中key为之前的第几天, 0表示当天, 1表示前一天, 以此类推
     */
    public static Map<Integer, Map<String, DateTime>> getPerDayStartTimeAndEndTimeBefore(
        Date startDate, int range) {
        Date newDate = startDate == null ? new Date() : startDate;

        Map<Integer, Map<String, DateTime>> map = new HashMap<>();
        for (int i = 0; i < range; i++) {
            if (i == 0) {
                map.put(i, getOneDayTime(new DateTime(newDate)));
            } else {
                Date lastFewDays = getLastFewDays(newDate, i);
                map.put(i, getOneDayTime(new DateTime(lastFewDays)));
            }
        }
        return map;
    }

    /**
     * 获取指定日期连续前n周内,每周周一的开始时间和每周周日的结束时间
     *
     * @param startDate
     * @param range
     * @return Map中key为之前的第几周, 0表示当周, 1表示前一周, 以此类推
     */
    public static Map<Integer, Map<String, DateTime>> getPerWeekStartTimeAndEndTimeBefore(
        Date startDate, int range) {
        Date newDate = startDate == null ? new Date() : startDate;

        Map<Integer, Map<String, DateTime>> map = new HashMap<>();
        for (int i = 0; i < range; i++) {
            if (i == 0) {
                map.put(i, getOneWeekStartAndEndTime(new DateTime(newDate)));
            } else {
                Date lastFewDays = getLastFewWeeks(newDate, i);
                map.put(i, getOneWeekStartAndEndTime(new DateTime(lastFewDays)));
            }
        }
        return map;
    }

    /**
     * 获取某一周的开始时间和结束时间
     *
     * @param today
     * @return
     */
    public static Map<String, DateTime> getOneWeekStartAndEndTime(DateTime today) {
        DateTime newDate = today == null ? new DateTime() : today;

        DateTime monday = newDate.withDayOfWeek(DateTimeConstants.MONDAY);
        DateTime sunday = newDate.withDayOfWeek(DateTimeConstants.SUNDAY);

        DateTime startTime = monday.hourOfDay().withMinimumValue();
        startTime = startTime.minuteOfHour().withMinimumValue();
        startTime = startTime.secondOfMinute().withMinimumValue();

        DateTime endTime = sunday.hourOfDay().withMaximumValue();
        endTime = endTime.minuteOfHour().withMaximumValue();
        endTime = endTime.secondOfMinute().withMaximumValue();

        Map<String, DateTime> dateMap = new HashMap<>();

        dateMap.put("start", startTime);
        dateMap.put("end", endTime);

        return dateMap;
    }

    /**
     * 获取某一月的开始时间和结束时间
     *
     * @param date
     * @return
     */
    public static Map<String, DateTime> getOneMonthStartAndEndTime(Date date) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(date);
        startCalendar.set(Calendar.DAY_OF_MONTH, 1);
        Date monthStart = startCalendar.getTime();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(date);
        endCalendar.set(Calendar.DAY_OF_MONTH, endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date monthEnd = endCalendar.getTime();

        DateTime startTime = new DateTime(monthStart).hourOfDay().withMinimumValue().minuteOfHour()
            .withMinimumValue().secondOfMinute().withMinimumValue();
        DateTime endTime =
            new DateTime(monthEnd).hourOfDay().withMaximumValue().minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue();

        Map<String, DateTime> dateMap = new HashMap<>();
        dateMap.put("start", startTime);
        dateMap.put("end", endTime);
        return dateMap;
    }

    /**
     * 获取某个日期在一年的初始时间，如2017.05.03对应的初始日期为2017.01.01
     *
     * @author liuwb
     * @date 2017/3/16
     */
    public static Date getStartDateForYear(Date date) {
        DateTime startTime =
            new DateTime(date).monthOfYear().withMinimumValue().dayOfMonth().withMinimumValue();
        return startTime.toDate();
    }

    /**
     * 获取某个日期在一天中的开始时间和结束时间
     *
     * @param
     * @return
     * @author liuwb
     * @date 2017/1/13
     */
    public static Map<String, Date> getOneDayStartAndEndTime(Date date) {
        DateTime startTime =
            new DateTime(date).hourOfDay().withMinimumValue().minuteOfHour().withMinimumValue()
                .secondOfMinute().withMinimumValue();
        DateTime endTime =
            new DateTime(date).hourOfDay().withMaximumValue().minuteOfHour().withMaximumValue()
                .secondOfMinute().withMaximumValue();

        Map<String, Date> dateMap = new HashMap<>();
        dateMap.put("start", startTime.toDate());
        dateMap.put("end", endTime.toDate());
        return dateMap;
    }



    /**
     * 获取当前格式化的时间
     *
     * @return 以"yyyy-MM-dd HH:mm:ss"字符串格式返回当前时间
     * @author chenjun
     */
    public static String getNowString() {
        return dateFormat(new Date(), null);
    }
}
