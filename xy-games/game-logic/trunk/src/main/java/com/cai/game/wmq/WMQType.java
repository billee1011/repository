package com.cai.game.wmq;

import java.util.HashMap;
import java.util.Map;

public enum WMQType {
	DEFAULT(-1) {
		@Override
		public WMQTable createTable() {
			return new WMQTable();
		}
	},;
	
	private final int value;

	WMQType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract WMQTable createTable();


	public static WMQType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, WMQType> maps = new HashMap<>();

	static {
		WMQType[] temp = WMQType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
