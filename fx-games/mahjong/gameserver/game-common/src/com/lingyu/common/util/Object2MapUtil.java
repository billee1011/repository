package com.lingyu.common.util;

import java.util.HashMap;
import java.util.Map;

public class Object2MapUtil {

	/**
	 * 解析（物品ID|数量;物品ID|数量）为Map
	 * 
	 * @param str
	 * @return
	 */
	public static Map<String, Integer> convert2Map(String str) {
		if (ObjectUtil.strIsEmpty(str)) {
			return null;
		}
		Map<String, Integer> map = new HashMap<String, Integer>();
		String[] _str = str.split(";");
		for (String str1 : _str) {
			if (ObjectUtil.strIsEmpty(str1))
				continue;

			String[] str2 = str1.split("\\|");
			if (str2.length == 1) {
				map.put(str2[0], 1);
			} else {
				map.put(str2[0], Integer.parseInt(str2[1]));
			}
		}
		return map;
	}

	/**
	 * 解析（str:int;str:int）为Map
	 * 
	 * @param str
	 * @return
	 */
	public static Map<String, Integer> convert2Map1(String str) {
		if (ObjectUtil.strIsEmpty(str)) {
			return null;
		}
		Map<String, Integer> map = new HashMap<String, Integer>();
		String[] _str = str.split(";");
		for (String str1 : _str) {
			if (ObjectUtil.strIsEmpty(str1))
				continue;

			String[] str2 = str1.split(":");
			if (str2.length == 1) {
				map.put(str2[0], 1);
			} else {
				map.put(str2[0], Integer.parseInt(str2[1]));
			}
		}
		return map;
	}

	/**
	 * 解析(int:float;int:float）为Map
	 * 
	 * @param str
	 * @return
	 */
	public static Map<Integer, Float> convert2Map2(String str) {
		if (ObjectUtil.strIsEmpty(str)) {
			return null;
		}
		Map<Integer, Float> map = new HashMap<Integer, Float>();
		String[] _str = str.split(";");
		for (String str1 : _str) {
			if (ObjectUtil.strIsEmpty(str1))
				continue;

			String[] str2 = str1.split("\\|");
			map.put(Integer.parseInt(str2[0]), Float.parseFloat(str2[1]));
		}
		return map;
	}

	/**
	 * 解析(str:float;str:float）为Map
	 * 
	 * @param str
	 * @return
	 */
	public static Map<String, Float> convert2Map3(String str) {
		if (ObjectUtil.strIsEmpty(str)) {
			return null;
		}
		Map<String, Float> map = new HashMap<String, Float>();
		String[] _str = str.split(";");
		for (String str1 : _str) {
			if (ObjectUtil.strIsEmpty(str1))
				continue;

			String[] str2 = str1.split("\\|");
			float odds = ConvertObjectUtil.obj2float(str2[1]);
			if (odds <= 0)
				continue;
			map.put(str2[0], odds);
		}
		return map;
	}

}
