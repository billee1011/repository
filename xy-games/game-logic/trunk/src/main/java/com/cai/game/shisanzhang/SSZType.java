package com.cai.game.shisanzhang;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJConstants;
import com.cai.game.mj.handler.yyzxz.MJTable_YYZXZ;
import com.cai.game.phz.handler.nxghz.NXGHZTable;
import com.cai.game.phz.handler.yywhz.YYWHZTable;
import com.cai.game.shisanzhang.baguai_cz.BAGUAITable_CZ;
import com.cai.game.shisanzhang.ssz_cz.SSZTable_CZ;
import com.cai.game.shisanzhang.ssz_jd.SSZTable_JD;
import com.cai.game.shisanzhang.ssz_zz.SSZTable_ZZ;
import com.cai.game.shisanzhang.xp.SSZTable_XP;

public enum SSZType {
	DEFAULT(-1) {
		@Override
		public SSZTable createTable() {
			return new SSZTable(this);
		}
	},
	// 溆浦十三张
	GAME_TYPE_SSZ_XUPU(GameConstants.GAME_TYPE_SSZ_XUPU) {
		@Override
		public SSZTable createTable() {
				return new SSZTable_XP();
			}
	},
	// 经典十三张
	GAME_TYPE_SSZ_JD(GameConstants.GAME_TYPE_SSZ_JD) {
		@Override
		public SSZTable createTable() {
				return new SSZTable_JD();
			}
	},
	// 镇江十三张
	GAME_TYPE_SSZ_ZZ(GameConstants.GAME_TYPE_SSZ_ZZ) {
		@Override
		public SSZTable createTable() {
				return new SSZTable_ZZ();
			}
	},
	// 郴州十三张
	GAME_TYPE_SSZ_CZ(GameConstants.GAME_TYPE_SSZ_CZ) {
		@Override
		public SSZTable createTable() {
				return new SSZTable_CZ();
			}
	},
	// 郴州八怪
	GAME_TYPE_BAGUAI_CZ(GameConstants.GAME_TYPE_BAGUAI_CZ) {
		@Override
		public SSZTable createTable() {
				return new BAGUAITable_CZ();
			}
	},
	;
	
	
	
	private final int value;

	SSZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract SSZTable createTable();


	public static SSZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, SSZType> maps = new HashMap<>();

	static {
		SSZType[] temp = SSZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
