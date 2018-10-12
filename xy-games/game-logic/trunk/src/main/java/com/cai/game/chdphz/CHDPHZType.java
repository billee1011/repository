package com.cai.game.chdphz;

import java.util.HashMap;
import java.util.Map;

public enum CHDPHZType {
	DEFAULT(-1) {
		@Override
		public CHDPHZTable createTable() {
			return new CHDPHZTable();
		}
	},;

	private final int value;

	CHDPHZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract CHDPHZTable createTable();

	public static CHDPHZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, CHDPHZType> maps = new HashMap<>();

	static {
		CHDPHZType[] temp = CHDPHZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
