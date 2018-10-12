package com.cai.game.zjh;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.zjh.ZJHTable_HB.ZJHTable_HB;
import com.cai.game.zjh.ZJHTable_JD.ZJHTable_JD;


public enum ZJHType {
	DEFAULT(-1) {
		@Override
		public ZJHTable createTable() {
			return new ZJHTable();
		}
	},

	GAME_TYPE_PJ_ZJH_JD(GameConstants.GAME_TYPE_ZJH_JD) {
		@Override
		public ZJHTable createTable() {
				return new ZJHTable_JD();
			}
	},
	GAME_TYPE_PJ_ZJH_HB(GameConstants.GAME_TYPE_ZJH_HB) {
		@Override
		public ZJHTable createTable() {
				return new ZJHTable_HB();
			}
	},
	;
	
	
	
	private final int value;

	ZJHType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract ZJHTable createTable();


	public static ZJHType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, ZJHType> maps = new HashMap<>();

	static {
		ZJHType[] temp = ZJHType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
