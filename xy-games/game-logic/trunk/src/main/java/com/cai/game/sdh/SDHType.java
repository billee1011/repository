package com.cai.game.sdh;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.sdh.handler.xtsdh.SDHTable_XT;
import com.cai.game.sdh.handler.yybs.SDHTable_YYBS;

public enum SDHType {

	DEFAULT(-1) {
		@Override
		public SDHTable createTable() {
			return new SDHTable();
		}
	},
	SANDAHA(GameConstants.GAME_TYPE_SANDAHA) {
		@Override
		public SDHTable createTable() {
			return new SDHTable();
		}
	},
	// 湘潭三打哈
	SANDAHA_XT(GameConstants.GAME_TYPE_SANDAHA_XT) {
		@Override
		public SDHTable createTable() {
			return new SDHTable_XT();
		}
	},
	
	// 三打哈 -- 益阳巴十
	SANDAHA_BS(GameConstants.GAME_TYPE_YI_YANG_BA_SHI) {
		@Override
		public SDHTable createTable() {
			return new SDHTable_YYBS();
		}
	},
	;

	private final int value;

	SDHType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建三打哈桌子
	public abstract SDHTable createTable();

	public static SDHType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SDHType> maps = new HashMap<>();

	static {
		SDHType[] temp = SDHType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
