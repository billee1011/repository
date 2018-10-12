package com.lingyu.noark.data.kit;

import java.lang.reflect.Field;

public class StringKit {
	public final static String NULL = "";

	/**
	 * 如果此字符串为 null 或者为空串（""），则返回 true
	 * 
	 * @param cs 字符串
	 * @return 如果此字符串为 null 或者为空，则返回 true
	 */
	public static boolean isEmpty(CharSequence cs) {
		return null == cs || cs.length() == 0;
	}

	/**
	 * 将一个字符串由驼峰式命名变成分割符分隔单词
	 * 
	 * <pre>
	 *  lowerWord("helloWorld", '_') => "hello_world"
	 * </pre>
	 * 
	 * @param cs 字符串
	 * @param c 分隔符
	 * 
	 * @return 转换后字符串
	 */
	public static String lowerWord(CharSequence cs, char c) {
		int len = cs.length();
		StringBuilder sb = new StringBuilder(len + 5);
		for (int i = 0; i < len; i++) {
			char ch = cs.charAt(i);
			if (Character.isUpperCase(ch)) {
				if (i > 0)
					sb.append(c);
				sb.append(Character.toLowerCase(ch));
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	/**
	 * 生成Get方法名.
	 * <p>
	 * 
	 * @param field 属性
	 * @return Get方法名
	 */
	public static String genGetMethodName(Field field) {
		int len = field.getName().length();
		if (field.getType() == boolean.class) {
			StringBuilder sb = new StringBuilder(len + 1);
			sb.append("is").append(field.getName());
			if (Character.isLowerCase(sb.charAt(2))) {
				sb.setCharAt(2, Character.toUpperCase(sb.charAt(2)));
			}
			return sb.toString();
		} else {
			StringBuilder sb = new StringBuilder(len + 3);
			sb.append("get").append(field.getName());
			if (Character.isLowerCase(sb.charAt(3))) {
				sb.setCharAt(3, Character.toUpperCase(sb.charAt(3)));
			}
			return sb.toString();
		}
	}

	public static String genSetMethodName(Field field) {
		int len = field.getName().length();
		StringBuilder sb = new StringBuilder(len + 3);
		sb.append("set").append(field.getName());
		if (Character.isLowerCase(sb.charAt(3))) {
			sb.setCharAt(3, Character.toUpperCase(sb.charAt(3)));
		}
		return sb.toString();
	}
}
