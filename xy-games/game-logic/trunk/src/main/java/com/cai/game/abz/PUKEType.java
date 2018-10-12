package com.cai.game.abz;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.abz.handler.abz_four.PUKETable_ABZ_FOUR;
import com.cai.game.abz.handler.abz_three.PUKETable_ABZ_THREE;
import com.cai.game.zjh.ZJHTable_HB.ZJHTable_HB;
import com.cai.game.zjh.ZJHTable_JD.ZJHTable_JD;


public enum PUKEType {
	DEFAULT(-1) {
		@Override
		public PUKETable createTable() {
			return new PUKETable();
		}
	},

	GAME_TYPE_PUKE_ABZ_FOUR(GameConstants.GAME_TYPE_PUKE_ABZ_FOUR) {
		@Override
		public PUKETable createTable() {
				return new PUKETable_ABZ_FOUR();
			}
	},
	GAME_TYPE_PUKE_ABZ_THREE(GameConstants.GAME_TYPE_PUKE_ABZ_THREE) {
		@Override
		public PUKETable createTable() {
				return new PUKETable_ABZ_THREE();
			}
	},
	;
	
	
	
	private final int value;

	PUKEType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PUKETable createTable();


	public static PUKEType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PUKEType> maps = new HashMap<>();

	static {
		PUKEType[] temp = PUKEType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
