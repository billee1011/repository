package com.cai.game.schcpdss;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.schcpdss.handler.dssms.SCHCPDSSTable_MS;


public enum SCHCPDSSType {

	DEFAULT(-1) {
		@Override
		public SCHCPDSSTable createTable() {
			return new SCHCPDSSTable();
		}
	},
	// 
	GAME_TYPE_DSS_MS(GameConstants.GAME_TYPE_DSS_MS) {
		@Override
		public SCHCPDSSTable_MS createTable() {
			return new SCHCPDSSTable_MS();
		}
	},
	;

	private final int value;

	SCHCPDSSType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	
	// 创建麻将桌子
	public abstract SCHCPDSSTable createTable();


	public static SCHCPDSSType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SCHCPDSSType> maps = new HashMap<>();

	static {
		SCHCPDSSType[] temp = SCHCPDSSType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
