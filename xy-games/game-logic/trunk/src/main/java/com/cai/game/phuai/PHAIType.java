package com.cai.game.phuai;

import java.util.HashMap;
import java.util.Map;

public enum PHAIType {
	DEFAULT(-1) {
		@Override
		public PHAITable createTable() {
			return new PHAITable();
		}
	},;
	
	private final int value;

	PHAIType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PHAITable createTable();


	public static PHAIType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PHAIType> maps = new HashMap<>();

	static {
		PHAIType[] temp = PHAIType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
