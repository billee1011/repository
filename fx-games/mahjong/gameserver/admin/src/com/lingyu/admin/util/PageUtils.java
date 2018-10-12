package com.lingyu.admin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.lingyu.common.entity.Server;

/**
 * 分页相关
 * 
 * @author Wang Shuguang
 * 
 */
public class PageUtils {
	/***
	 * 获取分页显示信息
	 * 
	 * @param curPage
	 * @param totalPage
	 * @param displayNum
	 * @return
	 */
	public static List<Integer> calPages(int curPage, int totalPage, int displayNum) {
		List<Integer> ret = new ArrayList<Integer>(displayNum);
		int half = displayNum / 2;
		if (curPage <= half) {
			for (int i = 1; i <= Math.min(displayNum, totalPage); i++) {
				ret.add(i);
			}
		} else if (curPage >= totalPage - half) {
			for (int i = Math.max(0, totalPage - displayNum) + 1; i <= totalPage; i++) {
				ret.add(i);
			}
		} else {
			for (int i = curPage - half; i <= curPage + half; i++) {
				ret.add(i);
			}
		}
		return ret;
	}
	
	public static <E> List<E> filterPageList(List<E> list, int page, int rows, MutableInt totalSize) {
		List<E> ret = Collections.emptyList();
		int start = (page - 1) * rows;
		if(start >= list.size()){
			return ret;
		}
		int end = page * rows;
		if (end > list.size()) {
			end = list.size();
		}

		totalSize.setValue(list.size());
		ret = list.subList(start, end);
		return ret;
	}
}
