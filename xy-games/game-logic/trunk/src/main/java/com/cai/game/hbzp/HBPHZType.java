package com.cai.game.hbzp;

import java.util.HashMap;
import java.util.Map;

public enum HBPHZType {
	DEFAULT(-1) {
		@Override
		public HBPHZTable createTable() {
			return new HBPHZTable();
		}
	},;

	private final int value;

	HBPHZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract HBPHZTable createTable();

	public static HBPHZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, HBPHZType> maps = new HashMap<>();

	static {
		HBPHZType[] temp = HBPHZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
