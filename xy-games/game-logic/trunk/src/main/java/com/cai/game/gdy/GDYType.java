package com.cai.game.gdy;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.gdy.handler.gdy_hb.GDYTable_HB;
import com.cai.game.gdy.handler.gdy_jd.GDYTable_JD;
import com.cai.game.gdy.handler.gdy_rar.GDYTable_RAR;

public enum GDYType {
	DEFAULT(-1) {
		@Override
		public AbstractGDYTable createTable() {
			return new GDYTable();
		}
	},
	// 干瞪眼
	GAME_TYPE_GDY(GameConstants.GAME_TYPE_GDY) {
		@Override
		public int[] getCards() {
			return GDYConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractGDYTable createTable() {
			return new GDYTable_JD();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}
	},
	// 干瞪眼
	GAME_TYPE_GDY_HB(GameConstants.GAME_TYPE_GDY_HB) {

		@Override
		public AbstractGDYTable createTable() {
			return new GDYTable_HB();
		}
	},
	// 南平干瞪眼
	GAME_TYPE_GDY_NP(GameConstants.GAME_TYPE_GDY_NP) {

		@Override
		public AbstractGDYTable createTable() {
			return new GDYTable_HB();
		}
	},
	// 欢乐干瞪眼
	GAME_TYPE_GDY_HL(GameConstants.GAME_TYPE_GDY_RAR) {

		@Override
		public AbstractGDYTable createTable() {
			return new GDYTable_RAR();
		}
	},;

	private final int value;

	GDYType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractGDYTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return GDYConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static GDYType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, GDYType> maps = new HashMap<>();

	static {
		GDYType[] temp = GDYType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
