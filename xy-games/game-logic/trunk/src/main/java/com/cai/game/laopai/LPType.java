package com.cai.game.laopai;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.laopai.handler.xp.LPTable_XP;



public enum LPType {
	DEFAULT(-1) {
		@Override
		public AbstractLPTable createTable() {
			return new LPTable();
		}
	},
	// 溆浦老牌
	GAME_TYPE_LAOPAI_XUPU(GameConstants.GAME_TYPE_LAOPAI_XUPU) {
		@Override
		public int[] getCards() {
			return LPConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractLPTable createTable() {
			return new LPTable_XP();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},

	;

	private final int value;
	
	LPType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractLPTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return LPConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static LPType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, LPType> maps = new HashMap<>();

	static {
		LPType[] temp = LPType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
