package com.lingyu.common.util;

import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

public class JsonUtil {
	public static Object toJavaObject(Object obj) {
		if (obj != null) {
			if (obj instanceof JSONArray) {
				Object[] array = ((JSONArray) obj).toArray();
				int size = array.length;
				for (int i = 0; i < size; i++) {
					array[i] = toJavaObject(array[i]);
				}
				obj = array;
			} else if (obj instanceof Map<?, ?>) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> map = (Map<Object, Object>) obj;
				for (Entry<Object, Object> e : map.entrySet()) {
					map.put(e.getKey(), toJavaObject(e.getValue()));
				}
			}
		}
		return obj;
	}

	public static Object[] toJavaArray(Object[] array) {
		if (array != null) {
			int length = array.length;
			for (int i = 0; i < length; i++) {
				array[i] = toJavaObject(array[i]);
			}
		}
		return array;
	}

	public static void main(String[] args) {

		Object[] a = new Object[] { "d", 1 };
		Object[] b = new Object[] { a, null };
		Object[] c = new Object[] { b, 3 };
		String content = JSON.toJSONString(c);
//		JSONArray obj = (JSONArray) JSON.parse(content);

//		Object ret = JsonUtil.toJavaArray(obj.toArray());
		Object xxx = JSON.parse(content);
		if(xxx instanceof Object[]){
			System.out.println();
		}
		System.out.println(JSON.parse(content));
	}
}
