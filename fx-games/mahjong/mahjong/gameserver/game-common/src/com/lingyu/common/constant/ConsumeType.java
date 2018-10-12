package com.lingyu.common.constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lingyu.common.core.ServiceException;

/**
 * @Description 角色物品消耗类型
 * @author majiayun
 * @date 2014年3月20日 下午4:20:32
 */
public enum ConsumeType {
	NORMAL(1000, "常规，替代"),
	//
	XXXXX(2999, "样式");

	/**********************************************************************/
	private int id;// 编号
	private String description;// 描述

	private ConsumeType(int id, String description) {
		this.id = id;
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public String getIdStr() {
		return id + "";
	}

	public String getDescription() {
		return description;
	}

	public final static List<String[]> getConsumeTypeList() {
		List<String[]> list = new ArrayList<>();
		for (ConsumeType type : ConsumeType.values()) {
			list.add(new String[] { String.valueOf(type.getId()), type.getDescription() });
		}
		Collections.sort(list, new Comparator<String[]>() {
			@Override
			public int compare(String[] o1, String[] o2) {
				if (Integer.parseInt(o1[0]) > Integer.parseInt(o2[0])) {
					return 1;
				} else if (Integer.parseInt(o1[0]) < Integer.parseInt(o2[0])) {
					return -1;
				}
				return 0;
			}

		});
		return list;
	}

	public static String getConsumeTypeName(int id) {
		for (ConsumeType type : ConsumeType.values()) {
			if (type.id == id) {
				return type.description;
			}
		}
		return null;
	}

	/**********************************************************************/
	private static Map<Integer, ConsumeType> result = new HashMap<>();
	static {
		for (ConsumeType type : ConsumeType.values()) {
			if (result.containsKey(type.getId())) {
				throw new ServiceException("道具消耗操作类型存在重复的编号：[{}]", type.getId());
			}
			result.put(type.getId(), type);
		}
	}

	public static void main(String[] args) {
		for (ConsumeType type : ConsumeType.values()) {
			System.out.println(type.getId() + "\t" + type.getDescription());
		}
	}
}
