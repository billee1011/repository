package com.lingyu.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.MapUtils;

import com.alibaba.fastjson.JSON;
import com.lingyu.common.constant.CurrencyConstant.CurrencyType;

public class MapUtil {
	/**
	 * 把attName,attValue加入tmpMap
	 * 
	 * @param tmpMap
	 * @param attName
	 * @param attValue
	 */
	public static <K extends Object> void addMapValue(Map<K, Float> tmpMap, K attName, Float attValue) {
		Float value = tmpMap.get(attName);
		if (value == null) {
			tmpMap.put(attName, attValue);
		} else {
			tmpMap.put(attName, value + attValue);
		}
	}

	public static <K extends Object> void addMapValue(Map<K, Integer> tmpMap, K attName, Integer attValue) {
		Integer value = tmpMap.get(attName);
		if (value == null) {
			tmpMap.put(attName, attValue);
		} else {
			tmpMap.put(attName, value + attValue);
		}
	}

	public static <K extends Object> void addMapValue(Map<K, Long> tmpMap, K attName, Long attValue) {
		Long value = tmpMap.get(attName);
		if (value == null) {
			tmpMap.put(attName, attValue);
		} else {
			tmpMap.put(attName, value + attValue);
		}
	}

	/**
	 * 把Map2的值加入Map1
	 * 
	 * @param tmpMap1
	 * @param tmpMap2
	 */
	public static <K extends Object> void addMapValue(Map<K, Float> tmpMap1, Map<K, Float> tmpMap2) {
		if (tmpMap2 == null || tmpMap2.size() == 0) {
			return;
		}

		for (K attName : tmpMap2.keySet()) {
			Float attValue1 = tmpMap1.get(attName);
			Float attValue2 = tmpMap2.get(attName);

			if (attValue1 == null) {
				tmpMap1.put(attName, attValue2);
			} else {
				tmpMap1.put(attName, attValue1 + attValue2);
			}
		}
	}

	/**
	 * 把Map2的值加入Map1
	 * 
	 * @param <K>
	 * 
	 * @param tmpMap1
	 * @param tmpMap2
	 */
	public static <K extends Object> void addMapIntegerValue(Map<K, Integer> tmpMap1, Map<K, Integer> tmpMap2) {
		if (tmpMap2 == null || tmpMap2.size() == 0) {
			return;
		}

		for (K attName : tmpMap2.keySet()) {
			Integer attValue1 = tmpMap1.get(attName);
			Integer attValue2 = tmpMap2.get(attName);

			if (attValue1 == null) {
				tmpMap1.put(attName, attValue2);
			} else {
				tmpMap1.put(attName, attValue1 + attValue2);
			}
		}
	}

	// ---------------------------------------------------------------------
	/**
	 * 把Map转换为数组[ [key,value], [key, value]... ]
	 * 
	 * @param argMap
	 * @return
	 */
	public static <K, V> Object[] map2Array(Map<K, V> map) {
		if (MapUtils.isEmpty(map)) {
			return new Object[] {};
		}
		Object[] result = new Object[map.size()];
		int i = 0;
		for (Entry<K, V> e : map.entrySet()) {
			if (e.getKey() instanceof CurrencyType) {
				result[i++] = new Object[] { ((CurrencyType) e.getKey()).getId(), e.getValue() };
			} else {
				result[i++] = new Object[] { e.getKey(), e.getValue() };
			}
		}
		return result;

	}

	// /**
	// * 把Map转换为二维数组
	// *
	// * @param paramMap
	// * @return [ [key,value],[key,value]... ]
	// */
	// public static <K, V> Object convertMap2Array(Map<K, V> paramMap) {
	// if (paramMap == null || paramMap.size() == 0) {
	// return null;
	// }
	// Object[] result = new Object[paramMap.size()];
	// int i = 0;
	// for (K key : paramMap.keySet()) {
	// result[i++] = new Object[] { key, paramMap.get(key) };
	// }
	// return result;
	// }
	// ---------------------------------------------------------------------
	/**
	 * 把srcMap中的值扩大odds倍放入targetMap中
	 * 
	 * @param srcMap
	 * @param targetMap
	 */
	public static void multipleAttribute(Map<String, Float> srcMap, float odds, Map<String, Float> targetMap) {
		if (srcMap == null || srcMap.size() == 0 || odds == 0)
			return;

		for (String attributeKey : srcMap.keySet()) {
			float value = srcMap.get(attributeKey);
			addMapValue(targetMap, attributeKey, value * odds);
		}
	}

	/**
	 * 把srcMap中的值扩大odds倍放入targetMap中
	 * 
	 * @param srcMap
	 * @param targetMap
	 */
	public static void multipleAttribute(Map<String, Integer> srcMap, int odds, Map<String, Integer> targetMap) {
		if (srcMap == null || srcMap.size() == 0 || odds == 0)
			return;

		for (String attributeKey : srcMap.keySet()) {
			int value = srcMap.get(attributeKey);
			addMapValue(targetMap, attributeKey, value * odds);
		}
	}

	// /**
	// * 丹药信息转换为Map
	// *
	// * @param jsonObj
	// * @return
	// */
	// public static Map<String, Integer> convertDanyaoInfo(JSONObject jsonObj)
	// {
	// if (jsonObj == null || jsonObj.size() == 0) {
	// return null;
	// }
	// Map<String, Integer> goodsMap = new HashMap<String, Integer>();
	// for (Object goodsIdObj : jsonObj.keySet()) {
	// Object count = jsonObj.get(goodsIdObj);
	//
	// goodsMap.put((String) goodsIdObj, (Integer) count);
	// }
	// return goodsMap;
	// }

	// ---------------------------------------------------------------------
	/**
	 * Map转换成String(key:value;key:value;...)
	 * 
	 * @param paramMap
	 * @return
	 */
	public static <K, V> String map2String(Map<K, V> paramMap) {
		if (paramMap == null || paramMap.size() == 0) {
			return null;
		}

		StringBuilder result = new StringBuilder();
		for (K k : paramMap.keySet()) {
			result.append(SPLIT2).append(k).append(SPLIT1).append(paramMap.get(k));
		}
		return result.substring(1);
	}

	private static final String SPLIT1 = ":";
	private static final String SPLIT2 = ";";

	// ---------------------------------------------------------------------
	/**
	 * 字符串转换为Map<String, Long>
	 * 
	 * @param strs
	 * @return
	 */
	public static Map<String, Long> convertStr2Map(String strs) {
		if (ObjectUtil.strIsEmpty(strs)) {
			return null;
		}

		Map<String, Long> result = new HashMap<String, Long>();
		String[] strArray = strs.split(SPLIT2);
		for (String str : strArray) {
			String[] _str = str.split(SPLIT1);
			result.put(_str[0], Long.valueOf(_str[1]));
		}
		return result;
	}

	/**
	 * 字符串转换为Map<String, Integer>
	 * 
	 * @param strs
	 * @return
	 */
	public static Map<String, Integer> convertStr2MapInteger(String strs) {
		if (ObjectUtil.strIsEmpty(strs)) {
			return null;
		}

		Map<String, Integer> result = new HashMap<String, Integer>();
		String[] strArray = strs.split(SPLIT2);
		for (String str : strArray) {
			String[] _str = str.split(SPLIT1);
			if (_str.length < 2) {
				result.put(_str[0], 1);
			} else {
				result.put(_str[0], Integer.valueOf(_str[1]));
			}
		}
		return result;
	}

	/**
	 * 字符串转换为Map<String, Integer>
	 * 
	 * @param strs
	 * @return
	 */
	public static Map<Integer, Integer> convertStr2MapIntInt(String strs) {
		if (ObjectUtil.strIsEmpty(strs)) {
			return null;
		}

		Map<Integer, Integer> result = new HashMap<Integer, Integer>();
		String[] strArray = strs.split(SPLIT2);
		for (String str : strArray) {
			String[] _str = str.split(SPLIT1);
			result.put(Integer.valueOf(_str[0]), Integer.valueOf(_str[1]));
		}
		return result;
	}

	public static Map<String, Float> addMapValue(Map<String, Float> tmpMap, float rate) {
		Map<String, Float> result = new HashMap<>();
		for (Entry<String, Float> e : tmpMap.entrySet()) {
			result.put(e.getKey(), e.getValue() * rate);
		}
		return result;
	}

	// ---------------------------------------------------------------------
	/**
	 * 对Map<<code>K, Float>进行正序排序
	 * 
	 * @param oldMap
	 * @return
	 */
	public static <K> Map<K, Float> sortMap(Map<K, Float> oldMap) {
		ArrayList<Map.Entry<K, Float>> list = new ArrayList<Map.Entry<K, Float>>(oldMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, Float>>() {

			@Override
			public int compare(Entry<K, Float> arg0, Entry<K, Float> arg1) {
				float dec = arg0.getValue() - arg1.getValue();
				if (dec > 0) {
					return 1;
				} else if (dec < 0) {
					return -1;
				}
				return 0;
			}
		});
		Map<K, Float> newMap = new LinkedHashMap<>();
		for (int i = 0; i < list.size(); i++) {
			newMap.put(list.get(i).getKey(), list.get(i).getValue());
		}
		return newMap;
	}

	/**
	 * 对Map<<code>K, Long>进行排序
	 * 
	 * @param oldMap
	 * @param isAesc true: 升序; false: 降序;
	 * @return
	 */
	public static <K> Map<K, Long> sortMapLong(Map<K, Long> oldMap, final boolean isAesc) {
		ArrayList<Map.Entry<K, Long>> list = new ArrayList<Map.Entry<K, Long>>(oldMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, Long>>() {

			@Override
			public int compare(Entry<K, Long> arg0, Entry<K, Long> arg1) {
				long dec = 0;
				if (isAesc) {
					dec = arg0.getValue() - arg1.getValue();
				} else {
					dec = arg1.getValue() - arg0.getValue();
				}
				if (dec > 0) {
					return 1;
				} else if (dec < 0) {
					return -1;
				}
				return 0;
			}
		});

		Map<K, Long> newMap = new LinkedHashMap<>();
		for (int i = 0; i < list.size(); i++) {
			newMap.put(list.get(i).getKey(), list.get(i).getValue());
		}
		return newMap;
	}

	// ---------------------------------------------------------------------
	/**
	 * 是否满足完成条件
	 * 
	 * @param currentCondition
	 * @param completeCondition
	 * @return
	 */
	public static boolean isSatisfyComplete(Map<String, Integer> currentCondition, Map<String, Integer> completeCondition) {
		if (MapUtils.isEmpty(completeCondition)) {
			return true;
		}
		int curSize = currentCondition != null ? currentCondition.size() : 0;
		if (curSize < completeCondition.size()) {
			return false;
		}
		for (Entry<String, Integer> entry : completeCondition.entrySet()) {
			Integer curValue = currentCondition.get(entry.getKey());
			if (curValue == null || curValue < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	// ---------------------------------------------------------------------
	public static Map<Integer, int[]>[] getChangeAttr(Map<Integer, Integer> oldMap, Map<Integer, Integer> newMap) {
		Map<Integer, int[]>[] ret = new Map[] { new HashMap<>(), new HashMap<>(), new HashMap<>() };
		if (oldMap != null && newMap != null) {
			Iterator<Entry<Integer, Integer>> iterator = oldMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<Integer, Integer> entry = iterator.next();
				int key = entry.getKey();
				int value = entry.getValue();
				Integer newValue = newMap.get(key);
				if (newValue != null) {
					if (newValue != value) {
						// 改变的值
						ret[0].put(key, new int[] { value, newValue });
					}
				} else {
					// 删除的key
					ret[1].put(key, new int[] { value });
				}
			}
			Iterator<Entry<Integer, Integer>> newIterator = newMap.entrySet().iterator();
			while (newIterator.hasNext()) {
				Entry<Integer, Integer> entry = newIterator.next();
				int key = entry.getKey();
				int value = entry.getValue();
				Integer oldValue = oldMap.get(key);
				if (oldValue == null) {
					// 新增的key
					ret[2].put(key, new int[] { value });
				}
			}
		}
		return ret;
	}

	// --------------------------------------------------------------------------------
	public static void main(String[] args) {
		Map<Integer, Integer> resulte = new HashMap<>();
		resulte.put(1, 2);
		resulte.put(2, 3);
		resulte.put(3, 4);
		String info = map2String(resulte);

		System.out.println("--->curent is:" + info);

		Map<Integer, Integer> dat = convertStr2MapIntInt(info);

		System.out.println("->" + dat);
		Map<Integer, Integer> oldMap = new HashMap<>();
		oldMap.put(1001, 48);
		oldMap.put(1014, 89);
		oldMap.put(1015, 101);
		Map<Integer, Integer> newMap = new HashMap<>();
		newMap.put(1001, 80);
		newMap.put(1016, 200);
		newMap.put(1021, 210);
		newMap.put(1015, 101);
		Map<Integer, int[]>[] ret = getChangeAttr(oldMap, newMap);
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < ret.length; i++) {
			switch (i) {
			case 0:
				str.append("改变的:" + JSON.toJSONString(ret[0]));
				break;
			case 1:
				str.append(" 删除的:" + JSON.toJSONString(ret[1]));
				break;
			case 2:
				str.append(" 新增的:" + JSON.toJSONString(ret[2]));
				break;
			}
		}
		System.out.println(str.toString());
	}
}
