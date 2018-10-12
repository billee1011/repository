package com.cai.game.sdh;

import java.util.List;

public abstract class SDHUtil {

	/**
	 * 判断一个数组中是否全部是相同元素
	 * 
	 * @param array
	 * @param begin
	 *            起始位置
	 * @param end
	 *            结束位置
	 * @return
	 */
	public static boolean isSameElement(int[] array, int begin, int end) {
		boolean result = true;

		for (int i = begin + 1; i < end; i++) {
			if (array[begin] != array[i]) {
				result = false;
				break;
			}
		}

		return result;
	}

	/**
	 * 统计数组中的相同元素
	 * 
	 * @param array
	 * @param begin
	 * @param end
	 * @return
	 */
	public static int countSameElement(int[] array, int begin, int end) {
		int result = 0;

		for (int i = begin + 1; i < end; i++) {
			if (array[begin] != array[i]) {
				result++;
				break;
			}
		}

		return result;
	}

	/**
	 * 拷贝一个数组
	 * 
	 * @param array
	 *            目标数组
	 * @param begin
	 *            起始位置
	 * @param end
	 *            结束位置
	 * @return
	 */
	public static int[] copyArray(int[] array, int begin, int end) {
		int[] result = new int[end - begin];
		for (int i = begin; i < end; i++) {
			result[i] = array[i];
		}
		return result;
	}

	/**
	 * 拷贝一个数组
	 * 
	 * @param source
	 *            资源数组
	 * @param target
	 *            目标数组
	 * @param begin
	 *            起始位置
	 * @param len
	 *            长度
	 */
	public static void copyArray(int[] source, int target[], int begin, int len) {
		for (int i = 0; i < len; i++) {
			target[i] = source[i + begin];
		}
	}

	public static void swap(int a, int b) {
		int t = a;
		a = b;
		b = t;
	}

	public static long[] listToArray(List<Integer> list) {
		long[] operate = new long[list.size()];

		for (int i = 0; i < list.size(); i++) {
			operate[i] = list.get(i);
		}

		return operate;
	}
}
