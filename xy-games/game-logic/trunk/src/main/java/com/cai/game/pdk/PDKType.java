package com.cai.game.pdk;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.pdk.handler.fifteenpdk.PDK_FIFTEEN_Table;
import com.cai.game.pdk.handler.fifteenpdk_DT.PDK_FIFTEENDT_Table;
import com.cai.game.pdk.handler.fifteenpdk_NP.PDK_FIFTEEN_Table_NP;
import com.cai.game.pdk.handler.fifteenpdk_ah.PDK_FIFTEENAH_Table;
import com.cai.game.pdk.handler.fifteenpdk_cz.PDK_FIFTEEN_Table_CZ;
import com.cai.game.pdk.handler.fifteenpdk_dl.PDK_FIFTEENDL_Table;
import com.cai.game.pdk.handler.fifteenpdk_hn.PDK_FIFTEEN_Table_HN;
import com.cai.game.pdk.handler.fifteenpdk_ld.PDK_FIFTEEN_Table_LD;
import com.cai.game.pdk.handler.fifteenpdk_xiangyang.PDK_FIFTEEN_Table_XiangYang;
import com.cai.game.pdk.handler.fifteenpdk_yuey.PDK_FIFTEENYUEY_Table;
import com.cai.game.pdk.handler.fifteenpdk_yy.PDK_FIFTEENYY_Table;
import com.cai.game.pdk.handler.jdpd_cz.PDK_JD_Table_CZ;
import com.cai.game.pdk.handler.jdpdk.PDK_JD_Table;
import com.cai.game.pdk.handler.jdpdk_DL.PDK_JDDL_Table;
import com.cai.game.pdk.handler.jdpdk_DT.PDK_JDDT_Table;
import com.cai.game.pdk.handler.jdpdk_NP.PDK_JD_Table_NP;
import com.cai.game.pdk.handler.jdpdk_ah.PDK_JDAH_Table;
import com.cai.game.pdk.handler.jdpdk_hn.PDK_JD_Table_HN;
import com.cai.game.pdk.handler.jdpdk_ld.PDK_JD_Table_LD;
import com.cai.game.pdk.handler.jdpdk_xiangyang.PDK_JD_Table_XiangYang;
import com.cai.game.pdk.handler.jdpdk_yueyang.PDK_JDYUEY_Table;
import com.cai.game.pdk.handler.jdpdk_yy.PDK_JDYY_Table;
import com.cai.game.pdk.handler.ll_fifteenpdk.LL_PDK_FIFTEEN_Table;
import com.cai.game.pdk.handler.ll_jdpdk.LL_PDK_JD_Table;
import com.cai.game.pdk.handler.srpdk_sc.PDK_SR_Table_SC;
import com.cai.game.pdk.handler.sxsdpdk.PDK_SXSD_Table;
import com.cai.game.pdk.handler.znpdk.PDK_ZN_Table;
import com.cai.game.pdk.handler.znpdk_cz.PDK_ZN_Table_CZ;

public enum PDKType {
	DEFAULT(-1) {
		@Override
		public PDKTable createTable() {
			return new PDKTable();
		}
	},
	// 经典跑得快
	GAME_TYPE_JDPDK(GameConstants.GAME_TYPE_PDK_JD) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table();
		}
	},
	// 十五张跑得快
	GAME_TYPE_SWPDK(GameConstants.GAME_TYPE_PDK_SW) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table();
		}
	},
	// 十五张跑得快
	GAME_TYPE_SWPDKLL(GameConstants.GAME_TYPE_PDK_SW_LL) {
		@Override
		public PDKTable createTable() {
			return new LL_PDK_FIFTEEN_Table();
		}
	},
	// 经典跑得快
	GAME_TYPE_JDPDKLL(GameConstants.GAME_TYPE_PDK_JD_LL) {
		@Override
		public PDKTable createTable() {
			return new LL_PDK_JD_Table();
		}
	},
	// 四川跑得快
	GAME_TYPE_SCPDK(GameConstants.GAME_TYPE_PDK_SC) {
		@Override
		public PDKTable createTable() {
			return new PDK_SR_Table_SC();
		}
	},
	// 扎鸟跑得快
	GAME_TYPE_ZNPDK(GameConstants.GAME_TYPE_PDK_ZN) {
		@Override
		public PDKTable createTable() {
			return new PDK_ZN_Table();
		}
	},
	// 益阳跑得快
	GAME_TYPE_JDYYPDK(GameConstants.GAME_TYPE_PDK_JD_YY) {
		@Override
		public PDKTable createTable() {
			return new PDK_JDYY_Table();
		}
	}, // 益阳跑得快
	GAME_TYPE_SWYYPDK(GameConstants.GAME_TYPE_PDK_SW_YY) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEENYY_Table();
		}
	},
	// 益阳跑得快
	GAME_TYPE_JDAHPDK(GameConstants.GAME_TYPE_PDK_JD_AH) {
		@Override
		public PDKTable createTable() {
			return new PDK_JDAH_Table();
		}
	}, // 益阳跑得快
	GAME_TYPE_SWAHPDK(GameConstants.GAME_TYPE_PDK_SW_AH) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEENAH_Table();
		}
	},
	// 益阳跑得快
	GAME_TYPE_JDYUEYPDK(GameConstants.GAME_TYPE_PDK_JD_YUEY) {
		@Override
		public PDKTable createTable() {
			return new PDK_JDYUEY_Table();
		}
	}, // 益阳跑得快
	GAME_TYPE_SWYUEYPDK(GameConstants.GAME_TYPE_PDK_SW_YUEY) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEENYUEY_Table();
		}
	},
	// 经典跑得快
	GAME_TYPE_JDPDK_CZ(GameConstants.GAME_TYPE_PDK_JD_CZ) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table_CZ();
		}
	},
	// 十五张跑得快
	GAME_TYPE_SWPDK_CZ(GameConstants.GAME_TYPE_PDK_SW_CZ) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table_CZ();
		}
	}, // 益阳跑得快
	GAME_TYPE_JDPDK_DT(GameConstants.GAME_TYPE_PDK_JD_DT) {
		@Override
		public PDKTable createTable() {
			return new PDK_JDDT_Table();
		}
	}, // 益阳跑得快
	GAME_TYPE_SWPDK_DT(GameConstants.GAME_TYPE_PDK_SW_DT) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEENDT_Table();
		}
	},
	// 扎鸟跑得快
	GAME_TYPE_ZNPDK_CZ(GameConstants.GAME_TYPE_PDK_ZN_CZ) {
		@Override
		public PDKTable createTable() {
			return new PDK_ZN_Table_CZ();
		}
	},
	// 快乐跑得快
	GAME_TYPE_KLPDK(GameConstants.GAME_TYPE_PDK_KL) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table();
		}
	},
	// 快乐跑得快
	GAME_TYPE_KLPDK_SW(GameConstants.GAME_TYPE_PDK_SW_KL) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table();
		}
	},
	// 快乐跑得快
	GAME_TYPE_KLPDK_ZN(GameConstants.GAME_TYPE_PDK_ZN_KL) {
		@Override
		public PDKTable createTable() {
			return new PDK_ZN_Table();
		}
	},
	// 陕西三代
	GAME_TYPE_SXSD(GameConstants.GAME_TYPE_PDK_SXSD) {
		@Override
		public PDKTable createTable() {
			return new PDK_SXSD_Table();
		}
	},
	// 襄阳跑得快
	GAME_TYPE_JDPDK_XY(GameConstants.GAME_TYPE_PDK_JD_XY) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table_XiangYang();
		}
	}, // 襄阳跑得快
	GAME_TYPE_SWPDK_XY(GameConstants.GAME_TYPE_PDK_SW_XY) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table_XiangYang();
		}
	},
	// 南平跑得快
	GAME_TYPE_JDPDK_NP(GameConstants.GAME_TYPE_PDK_JD_NP) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table_NP();
		}
	}, // 南平跑得快
	GAME_TYPE_SWPDK_NP(GameConstants.GAME_TYPE_PDK_SW_NP) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table_NP();
		}
	},
	// 娄底跑得快
	GAME_TYPE_JDPDK_LD(GameConstants.GAME_TYPE_PDK_JD_LD) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table_LD();
		}
	},
	// 娄底跑得快
	GAME_TYPE_SWPDK_LD(GameConstants.GAME_TYPE_PDK_SW_LD) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table_LD();
		}
	},
	// 娄底跑得快
	GAME_TYPE_PDK_JD_DL(GameConstants.GAME_TYPE_PDK_JD_DL) {
		@Override
		public PDKTable createTable() {
			return new PDK_JDDL_Table();
		}
	},
	// 娄底跑得快
	GAME_TYPE_PDK_SW_DL(GameConstants.GAME_TYPE_PDK_SW_DL) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEENDL_Table();
		}
	},
	// 河南跑得快
	GAME_TYPE_PDK_JD_HN(GameConstants.GAME_TYPE_PDK_JD_HN) {
		@Override
		public PDKTable createTable() {
			return new PDK_JD_Table_HN();
		}
	},
	// 河南跑得快
	GAME_TYPE_PDK_SW_HN(GameConstants.GAME_TYPE_PDK_SW_HN) {
		@Override
		public PDKTable createTable() {
			return new PDK_FIFTEEN_Table_HN();
		}
	},;

	private final int value;

	PDKType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PDKTable createTable();

	public static PDKType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PDKType> maps = new HashMap<>();

	static {
		PDKType[] temp = PDKType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
