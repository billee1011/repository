package com.cai.game.gxzp;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;


public enum GXZPType {

	;

	private final int value;

	GXZPType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子


	public static GXZPType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, GXZPType> maps = new HashMap<>();

	static {
		GXZPType[] temp = GXZPType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
