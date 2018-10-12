package com.lingyu.common.util;

/**
 */
public class StringBuilderUtil {

	public static long convert(String strId) {
		return Long.parseLong(strId);
	}

	public static void deleteLastChars(StringBuilder sb, int length) {
		if (sb.length() >= length) {
			sb.delete(sb.length() - length, sb.length());
		}
	}

	/**
	 * 用逗号连接字符数组
	 */
	public static StringBuilder jointStringWithComma(Object[] args) {
		return jointStringWithSpliter(args, " ,", 1);
	}

	/**
	 * 用下划线连接字符数组
	 */
	public static String jointStringWithUnderline(Object[] args) {
		return jointStringWithSpliter(args, "_", 1).toString();
	}

	/**
	 * 用指定分隔符连接字符数组
	 */
	public static StringBuilder jointStringWithSpliter(Object[] args, String spliter, int toDelete) {
		StringBuilder ret = new StringBuilder();
		for (Object each : args) {
			ret.append(each).append(spliter);
		}
		StringBuilderUtil.deleteLastChars(ret, toDelete);
		return ret;
	}

	/**
	 * @param pattern -"000000"
	 * @param input - the input String
	 */
	public static String format(String pattern, String input) {
		String str = pattern + input;
		return str.substring(str.length() - pattern.length());
	}

	/**
	 * @param pattern -"000000"
	 * @param input - the input String
	 */
	public static String format(String pattern, int input) {
		String str = pattern + input;
		return str.substring(str.length() - pattern.length());
	}
}
