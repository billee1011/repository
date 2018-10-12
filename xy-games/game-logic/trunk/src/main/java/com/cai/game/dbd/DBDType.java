package com.cai.game.dbd;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.dbd.handler.dbd_jd.DBDTable_JD;




public enum DBDType {
	DEFAULT(-1) {
		@Override
		public AbstractDBDTable createTable() {
			return new DBDTable();
		}
	},
	// 斗板凳
	GAME_TYPE_DBD(GameConstants.GAME_TYPE_DBD_JD) {
		@Override
		public int[] getCards() {
			return DBDConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractDBDTable createTable() {
			return new DBDTable_JD();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}
	},

	;

	private final int value;
	
	DBDType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractDBDTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return DBDConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static DBDType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, DBDType> maps = new HashMap<>();

	static {
		DBDType[] temp = DBDType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
