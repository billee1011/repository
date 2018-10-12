package com.cai.game.hh;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.hh.handler.ahphz.AnHuaHHTable;
import com.cai.game.hh.handler.csphz.HHTable_CS;
import com.cai.game.hh.handler.czphz.Table_ChenZhou;
import com.cai.game.hh.handler.czsrphz.Table_CZ_SR;
import com.cai.game.hh.handler.erphz.HHTable_ER;
import com.cai.game.hh.handler.hcphz.HeChiHHTable;
import com.cai.game.hh.handler.hsphz.HanShouHHTable;
import com.cai.game.hh.handler.ldfpf.LouDiFangPaoFaHHTable;
import com.cai.game.hh.handler.leiyangphz.Table_LeiYang;
import com.cai.game.hh.handler.new_czphz.Table_New_ChenZhou;
import com.cai.game.hh.handler.nxphz.NingXiangHHTable;
import com.cai.game.hh.handler.sybp.ShaoYangBoPiHHTable;
import com.cai.game.hh.handler.syzp.HHTable_SYZP;
import com.cai.game.hh.handler.wcphz.WangChengHHTable;
import com.cai.game.hh.handler.wugangphz.Table_WuGang;
import com.cai.game.hh.handler.xpphz.HHTable_XP;
import com.cai.game.hh.handler.xxphz.XiangXiangHHTable;
import com.cai.game.hh.handler.yyzhz.HHTable_YYZHZ;
import com.cai.game.hh.handler.yzchz.Table_YongZhou;

public enum HHType {
	DEFAULT(-1) {
		@Override
		public HHTable createTable() {
			return new HHTable();
		}
	},

	// 安化跑胡子
	ANHUAPHZ(GameConstants.GAME_TYPE_PHZ_AH) {
		@Override
		public HHTable createTable() {
			return new AnHuaHHTable();
		}
	},

	// 汉寿跑胡子
	HANSHOUPHZ(GameConstants.GAME_TYPE_PHZ_HS) {
		@Override
		public HHTable createTable() {
			return new HanShouHHTable();
		}
	},

	GAME_TYPE_PHZ_YONG_ZHOU(GameConstants.GAME_TYPE_PHZ_YONG_ZHOU) {
		@Override
		public HHTable createTable() {
			return new Table_YongZhou();
		}
	},

	// 湘乡跑胡子
	XIANGXIANGPHZ(GameConstants.GAME_TYPE_PHZ_XX) {
		@Override
		public HHTable createTable() {
			return new XiangXiangHHTable();
		}

	},

	// 河池字牌
	HECHIPHZ(GameConstants.GAME_TYPE_PHZ_HC) {

		@Override
		public HHTable createTable() {
			return new HeChiHHTable();
		}

	},

	// 宁乡跑胡子
	NINGXIANGPHZ(GameConstants.GAME_TYPE_PHZ_NX) {

		@Override
		public HHTable createTable() {
			return new NingXiangHHTable();
		}

	},

	// 邵阳剥皮
	SHAOYANGBOPI(GameConstants.GAME_TYPE_PHZ_SYBP) {

		@Override
		public HHTable createTable() {
			return new ShaoYangBoPiHHTable();
		}

	},

	// 岳阳捉红字
	YUANYANGZHUOHONZIPHZ(GameConstants.GAME_TYPE_HH_YYZHZ) {

		@Override
		public HHTable createTable() {
			return new HHTable_YYZHZ();
		}

	},
	// 祁东六胡抢
	GAME_TYPE_LHQ_QD_SAPP(GameConstants.GAME_TYPE_LHQ_QD_SAPP) {

		@Override
		public HHTable createTable() {
			return new HHTable();
		}

	},
	// 沅陵跑胡子
	GAME_TYPE_YL_PHZ(GameConstants.GAME_TYPE_YL_PHZ) {

		@Override
		public HHTable createTable() {
			return new HHTable();
		}

	},

	// 邵阳字牌
	SHAOYANGZIPAI(GameConstants.GAME_TYPE_PHZ_SY) {

		@Override
		public HHTable createTable() {
			return new HHTable_SYZP();
		}

	},

	// 长沙跑胡子
	CHANGSHAPHZ(GameConstants.GAME_TYPE_PHZ_CS) {

		@Override
		public HHTable createTable() {
			return new HHTable_CS();
		}

	},
	// 二人跑胡子
	ERRENPHZ(GameConstants.GAME_TYPE_PHZ_ER_REN) {

		@Override
		public HHTable createTable() {
			return new HHTable_ER();
		}

	},

	// 郴州字牌
	GAME_TYPE_PHZ_CHEN_ZHOU(GameConstants.GAME_TYPE_PHZ_CHEN_ZHOU) {

		@Override
		public HHTable createTable() {
			return new Table_ChenZhou();
		}

	},

	// 新郴州字牌
	GAME_TYPE_NEW_PHZ_CHEN_ZHOU(GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU) {

		@Override
		public HHTable createTable() {
			return new Table_New_ChenZhou();
		}

	},

	// 耒阳字牌
	GAME_TYPE_PHZ_LEI_YANG(GameConstants.GAME_TYPE_PHZ_LEI_YANG) {

		@Override
		public HHTable createTable() {
			return new Table_LeiYang();
		}

	},
	// 武冈字牌
	GAME_TYPE_PHZ_WU_GANG(GameConstants.GAME_TYPE_PHZ_WU_GANG) {

		@Override
		public HHTable createTable() {
			return new Table_WuGang();
		}

	},
	// 湖南郴州三人字牌
	GAME_TYPE_PHZ_CZ_SR(GameConstants.GAME_TYPE_PHZ_CZ_SR) {

		@Override
		public HHTable createTable() {
			return new Table_CZ_SR();
		}

	},
	// 娄底放炮罚
	GAME_TYPE_PHZ_LD_FANG_PAO_FA(GameConstants.GAME_TYPE_PHZ_LD_FANG_PAO_FA) {

		@Override
		public HHTable createTable() {
			return new LouDiFangPaoFaHHTable();
		}

	},

	// 望城跑胡子
	GAME_TYPE_PHZ_WANG_CHENG(GameConstants.GAME_TYPE_PHZ_WANG_CHENG) {

		@Override
		public HHTable createTable() {
			return new WangChengHHTable();
		}

	},

	// 溆浦跑胡子
	GAME_TYPE_PHZ_XU_PU(GameConstants.GAME_TYPE_PHZ_XP) {

		@Override
		public HHTable createTable() {
			return new HHTable_XP();
		}
	},;

	private final int value;

	HHType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract HHTable createTable();

	public static HHType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, HHType> maps = new HashMap<>();

	static {
		HHType[] temp = HHType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
