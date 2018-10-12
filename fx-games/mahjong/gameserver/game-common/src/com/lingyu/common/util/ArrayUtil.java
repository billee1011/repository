package com.lingyu.common.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;

public class ArrayUtil {
	/**
	 * Object[]转换为String[]
	 * 
	 * @param objs
	 * @return
	 */
	public static String[] objArray2StrArray(Object[] objs) {
		if (objs == null || objs.length == 0)
			return null;

		String[] result = new String[objs.length];
		for (int i = 0; i < objs.length; i++) {
			result[i] = (String) objs[i];
		}
		return result;
	}

	/**
	 * Object[]转换为int[]
	 * 
	 * @param objs
	 * @return
	 */
	public static int[] objArray2IntArray(Object[] objs) {
		if (objs == null || objs.length == 0)
			return null;

		int[] result = new int[objs.length];
		for (int i = 0; i < objs.length; i++) {
			result[i] = ConvertObjectUtil.object2Integer(objs[i]);
		}
		return result;
	}

	/**
	 * Object[]转换为int[]
	 * 
	 * @param objs
	 * @return
	 */
	public static long[] objArray2LongArray(Object[] objs) {
		if (objs == null || objs.length == 0)
			return null;

		long[] result = new long[objs.length];
		for (int i = 0; i < objs.length; i++) {
			result[i] = (long) objs[i];
		}
		return result;
	}

	/** 用于处理客户端传来的long 数组 */
	public static long[] objArray2LongArray2(Object[] objs) {

		if (objs == null || objs.length == 0)
			return null;
		int length = objs.length;
		long[] result = new long[objs.length];
		for (int i = 0; i < length; i++) {
			result[i] = ((Double) objs[i]).longValue();
		}
		return result;
	}

	public static long[] list2LongArray(Collection<Long> list) {
		long[] ret = new long[list.size()];
		int i = 0;
		for (long e : list) {
			ret[i++] = e;
		}
		return ret;
	}

	public static <T> T[] createArray(List<T> list, Class<T> clazz) {
		@SuppressWarnings("unchecked")
		T[] array = (T[]) Array.newInstance(clazz, list.size());
		for (int i = 0; i < array.length; i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static int average(int[] array) {
		if (array == null) {
			return 0;
		}
		long sum = 0;
		for (int i : array) {
			sum += i;
		}
		return (int) (sum / array.length);
	}
}