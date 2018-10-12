package com.cai.game.mj;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.mj.handler.hunanchangde.MJTable_HuNan_ChangDe;
import com.cai.game.mj.handler.hunanshaoyan.MJTable_ShaoYang;
import com.cai.game.mj.handler.yiyang.MJTable_YiYang;
import com.cai.game.mj.handler.yyzxz.MJTable_YYZXZ;

public enum MJType {
	DEFAULT(-1) {
		@Override
		public AbstractMJTable createTable() {
			// TODO Auto-generated method stub
			return new MJTable();
		}
	},
	// 岳阳抓虾子
	GAME_TYPE_YYZXZ(GameConstants.GAME_TYPE_YUE_YANG_ZHUA_XIA_ZI) {
		@Override
		public int[] getCards() {
			// TODO Auto-generated method stub
			return MJConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractMJTable createTable() {
			// TODO Auto-generated method stub
			return new MJTable_YYZXZ();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},

	// 邵阳麻将
	GAME_TYPE_HUNAN_SHAOYANG(GameConstants.GAME_TYPE_HUNAN_SHAOYANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_ShaoYang();
		}
	},

	// 益阳麻将
	GAME_TYPE_HUNAN_YIYANG(GameConstants.GAME_TYPE_HUNAN_YIYANG) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_YiYang();
		}

		@Override
		public int[] getCards() {
			return MJConstants.CARD_DATA_WAN_TIAO_TONG;
		}
	},

	GAME_TYPE_HUNAN_CHANGDE(GameConstants.GAME_TYPE_HU_NAN_CHANG_DE) {
		@Override
		public AbstractMJTable createTable() {
			return new MJTable_HuNan_ChangDe();
		}
	};

	private final int value;

	MJType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractMJTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return MJConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static MJType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, MJType> maps = new HashMap<>();

	static {
		MJType[] temp = MJType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
