package com.lingyu.common.constant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lingyu.common.core.ServiceException;

/**
 * @Description 系统物品产出类型
 * @author majiayun
 * @date 2014年3月20日 下午4:20:32
 */
public enum RewardType {

	/********** 小于100都为系统cmd ***************/
	CMD_SYSTEM_ADD_ADD_ITEM(1, "系统cmd添加物品指令产出"), SYSTEM_ZUBAO_TEST_ITEM(2, "系统组包测试掉落")
	;
	/******************************************************/

	private int id;// 编号
	private String description;// 描述

	private RewardType(int id, String description) {
		this.id = id;
		this.description = description;
	}

	public int getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	/**
	 * 获取rewardType
	 * 
	 * @param id
	 * @return
	 */
	public static RewardType getRewardType(int id) {
		return result.get(id);
	}

	public final static List<String[]> getRewardTypeList() {
		List<String[]> list = new ArrayList<>();
		for (RewardType type : RewardType.values()) {
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

	public static String getRewardTypeName(int id) {
		for (RewardType type : RewardType.values()) {
			if (type.id == id) {
				return type.description;
			}
		}
		return null;
	}

	/**********************************************************************/
	private static Map<Integer, RewardType> result = new HashMap<>();
	static {
		for (RewardType type : RewardType.values()) {
			if (result.containsKey(type.getId())) {
				throw new ServiceException("道具获得操作类型存在重复的编号：[{}]", type.getId());
			}
			result.put(type.getId(), type);
		}
	}

	public static void main(String[] args) {
		for (RewardType type : RewardType.values()) {
			System.out.println(type.getId() + "\t" + type.getDescription());
		}
	}
}
