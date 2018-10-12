package com.cai.game.schcpdz;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;




public enum SCHCPDZType {

	DEFAULT(-1) {
		@Override
		public SCHCPDZTable createTable() {
			return new SCHCPDZTable();
		}
	},
	// 
	GAME_TYPE_CHQ_YDR(GameConstants.GAME_TYPE_CHQ_YDR) {
		@Override
		public SCHCPDZTable createTable() {
			return new SCHCPDZTable();
		}

	},
	;

	private final int value;

	SCHCPDZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	// 创建麻将桌子
	public abstract SCHCPDZTable createTable();

	public static SCHCPDZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SCHCPDZType> maps = new HashMap<>();

	static {
		SCHCPDZType[] temp = SCHCPDZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
