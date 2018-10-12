package com.cai.game.schcp;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.schcp.handler.yanzhong.SCHCPTable_YanZhong;
import com.cai.game.schcpdz.SCHCPDZTable;



public enum SCHCPType {

	DEFAULT(-1) {
		@Override
		public SCHCPTable createTable() {
			return new SCHCPTable();
		}
	},
	// 
	GAME_TYPE_CP_YANZH(GameConstants.GAME_TYPE_CP_YANZH) {
		@Override
		public SCHCPTable_YanZhong createTable() {
			return new SCHCPTable_YanZhong();
		}

	},
	;;

	private final int value;

	SCHCPType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract SCHCPTable createTable();


	public static SCHCPType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SCHCPType> maps = new HashMap<>();

	static {
		SCHCPType[] temp = SCHCPType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
