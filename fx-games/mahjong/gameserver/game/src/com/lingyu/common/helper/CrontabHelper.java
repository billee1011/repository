package com.lingyu.common.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * 用于设置周期性被执行的指令的帮助工具类。
 * 
 * @author 小流氓<zhoumingkai@lingyuwangluo.com>
 */
public class CrontabHelper {

	public static List<String> convertTimeQuartzList(String args) {
		List<String> result = new ArrayList<>();
		for (String arg : StringUtils.split(args, ";")) {
			String[] times = StringUtils.split(arg, ":");
			result.add(new StringBuilder(64).append("0 ").append(times[1]).append(" ").append(times[0]).append(" * * ?").toString());
		}
		return result;
	}

	/** 时间cron表达式 -Map<<code>"10:30", cron表达式> */
	public static Map<String, String> convertTimeQuartzMap(String args) {
		Map<String, String> result = new HashMap<>();
		for (String arg : StringUtils.split(args, ";")) {
			String[] times = StringUtils.split(arg, ":");
			result.put(arg, new StringBuilder(64).append("0 ").append(times[1]).append(" ").append(times[0]).append(" * * ?").toString());
		}
		return result;
	}
}