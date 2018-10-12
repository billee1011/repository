package com.cai.game.scphz;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.schcpdz.SCHCPDZTable;

public enum SCPHZType {
	DEFAULT(-1) {
		@Override
		public SCPHZTable createTable() {
			return new SCPHZTable();
		}
	},
	GAME_TYPE_YJ_EQS(GameConstants.GAME_TYPE_YJ_EQS) {
		@Override
		public SCPHZTable createTable() {
			return new SCPHZTable();
		}

	},
	GAME_TYPE_MS_EQS(GameConstants.GAME_TYPE_MS_EQS) {
		@Override
		public SCPHZTable createTable() {
			return new SCPHZTable();
		}

	},
	GAME_TYPE_PHZ_SY_DE(GameConstants.GAME_TYPE_PHZ_SY_DE) {
		@Override
		public SCPHZTable createTable() {
			return new SCPHZTable();
		}

	},
	;

	private final int value;

	SCPHZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract SCPHZTable createTable();

	public static SCPHZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SCPHZType> maps = new HashMap<>();

	static {
		SCPHZType[] temp = SCPHZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
