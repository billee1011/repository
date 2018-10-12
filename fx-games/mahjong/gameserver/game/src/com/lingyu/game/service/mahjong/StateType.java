package com.lingyu.game.service.mahjong;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

/**
 * 状态
 * @author wangning
 * @date 2017年1月4日 下午5:06:21
 */
public enum StateType {



	WAIT(1, "等待界面"),
	STARTGAME(2, "打牌界面")
	;
	
	private static Map<Integer, StateType> stateMap = new HashMap<Integer, StateType>();

	public final Integer val;
	public final String description;// 描述

	private StateType(Integer val, String description) {
		this.val = val;
		this.description = description;
	}

	public Integer getVal() {
		return val;
	}
	
	protected static void initStateType() {
		for (StateType stateValue : StateType.values()) {
			stateMap.put(stateValue.getVal(), stateValue);
		}
	}

	public static StateType getStateType(int type) {
		if (MapUtils.isEmpty(stateMap))
			initStateType();
		return stateMap.get(type);
	}
	
}