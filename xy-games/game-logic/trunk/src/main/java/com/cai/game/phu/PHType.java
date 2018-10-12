package com.cai.game.phu;

import java.util.HashMap;
import java.util.Map;

public enum PHType {
	DEFAULT(-1) {
		@Override
		public PHTable createTable() {
			return new PHTable();
		}
	},;
	
	private final int value;

	PHType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PHTable createTable();


	public static PHType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PHType> maps = new HashMap<>();

	static {
		PHType[] temp = PHType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
