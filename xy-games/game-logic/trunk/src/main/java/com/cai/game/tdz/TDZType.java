package com.cai.game.tdz;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;

public enum TDZType {

	DEFAULT(-1) {
		@Override
		public TDZTable createTable() {
			return new TDZTable();
		}
	},
	TDZ_SX(GameConstants.GAME_TYPE_TDZ_SHAN_XI) {
		@Override
		public TDZTable createTable() {
			return new TDZTable();
		}
	};

	private final int value;

	TDZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建推对子桌子
	public abstract TDZTable createTable();

	public static TDZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, TDZType> maps = new HashMap<>();

	static {
		TDZType[] temp = TDZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
