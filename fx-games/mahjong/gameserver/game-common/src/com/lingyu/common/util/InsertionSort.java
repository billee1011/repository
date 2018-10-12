package com.lingyu.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 插入排序
 * 
 * @author zl 2013-7-14 下午4:21:51
 */
public class InsertionSort {
	/**
	 * 用插入排序对list进行从大到小的排序
	 * 
	 * @param idList
	 */
	public static void insertOrder(List<Integer> idList) {
		int size = idList.size();

		int j, iElement;
		for (int i = 1; i < size; i++) {
			iElement = idList.get(i);

			for (int k = 0; k < i; k++) {
				if (idList.get(k) < iElement) {
					j = i;
					while (j > k) {
						idList.set(j, idList.get(--j));
					}
					idList.set(k, iElement);
					break;
				}
			}
		}
	}

	/**
	 * 把一个元素插入到一个已经从大到小排序好的list中
	 * 
	 * @param list
	 * @param element
	 */
	public static void insert2InsertOrderList(List<Integer> list, int element) {
		if (list == null) {
			list = new ArrayList<Integer>();
		}
		if (list.size() == 0)
			list.add(element);
		else {
			for (int i = 0; i < list.size(); i++) {
				if (element > list.get(i)) {
					list.add(i, element);
					break;
				}
			}
		}

	}
}
