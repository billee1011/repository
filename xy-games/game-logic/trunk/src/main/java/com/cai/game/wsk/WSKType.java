package com.cai.game.wsk;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.wsk.handler.damazi.WSKTable_DMZ;
import com.cai.game.wsk.handler.dy.WSKTable_DY;
import com.cai.game.wsk.handler.gf.WSKTable_GF;
import com.cai.game.wsk.handler.hs.WSKTable_HS;
import com.cai.game.wsk.handler.hts.WSKTable_HTS;
import com.cai.game.wsk.handler.nsb.WSKTable_NSB;
import com.cai.game.wsk.handler.pcdz.WSKTable_PCDZ;
import com.cai.game.wsk.handler.shangraodazha.WSKTable_ShangRaoDaZha;
import com.cai.game.wsk.handler.sxth.WSKTable_SXTH;
import com.cai.game.wsk.handler.xndg.WSKTable_XNDG;
import com.cai.game.wsk.handler.ysdz.WSKTable_YSDZ;

public enum WSKType {
	DEFAULT(-1) {
		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable();
		}
	},
	// 打码子
	GAME_TYPE_WSK_DMZ(GameConstants.GAME_TYPE_WSK_DMZ) {
		@Override
		public int[] getCards() {
			return WSKConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_DMZ();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},

	GAME_TYPE_WSK_DMZ_YY(GameConstants.GAME_TYPE_WSK_DMZ_YY) {
		@Override
		public int[] getCards() {
			return WSKConstants.CARD_DATA_BU_DAI_FENG_YYZXZ;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_DMZ();
		}

		// 3人麻将
		@Override
		public int getPlayerCount() {
			return GameConstants.GAME_PLAYER_HH;
		}

	},

	GAME_TYPE_WSK_GF(GameConstants.GAME_TYPE_WSK_GF) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_GF();
		}
	},

	GAME_TYPE_SHANG_RAN_DA_ZHA(GameConstants.GAME_TYPE_SHANG_RAO_DA_ZHA) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_ShangRaoDaZha();
		}
	},

	// 牛十别
	GAME_TYPE_WSK_NSB(GameConstants.GAME_TYPE_WSK_NSB) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_NSB();
		}
	},
	// 咸宁打拱
	GAME_TYPE_WSK_XNDG(GameConstants.GAME_TYPE_WSK_XNDG) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_XNDG();
		}
	},
	// 浦城炸弹
	GAME_TYPE_WSK_PC_ZD(GameConstants.GAME_TYPE_WSK_PC_ZD) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_PCDZ();
		}
	},
	// 黄石510K
	GAME_TYPE_WSK_HS(GameConstants.GAME_TYPE_WSK_HS_510K) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_HS();
		}
	},
	// 黄石510K
	GAME_TYPE_WSK_DY(GameConstants.GAME_TYPE_WSK_DY_510K) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_DY();
		}
	},

	// 松溪同花
	GAME_TYPE_SXTH(GameConstants.GAME_TYPE_WSK_XQ_TH) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_SXTH();
		}
	},
	// 玉山打炸
	GAME_TYPE_YSDZ(GameConstants.GAME_TYPE_WSK_YSDZ) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_YSDZ();
		}
	},
	// 黑桃3
	GAME_TYPE_HTS(GameConstants.GAME_TYPE_WSK_HTS) {
		@Override
		public int[] getCards() {
			return GameConstants.CARD_DATA_WSK;
		}

		@Override
		public AbstractWSKTable createTable() {
			return new WSKTable_HTS();
		}
	},;

	private final int value;

	WSKType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract AbstractWSKTable createTable();

	// 获取 该牌桌麻将数据 需要的自己继承
	public int[] getCards() {
		return WSKConstants.DEFAULT;
	}

	public int getCardLength() {
		return this.getCards().length;
	}

	public int getPlayerCount() {
		return GameConstants.GAME_PLAYER;
	}

	public static WSKType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, WSKType> maps = new HashMap<>();

	static {
		WSKType[] temp = WSKType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
