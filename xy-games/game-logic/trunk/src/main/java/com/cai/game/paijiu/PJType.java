package com.cai.game.paijiu;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.paijiu.pj_deh_jd.PJTable_DEH_JD;
import com.cai.game.paijiu.pj_deh_mg.PJTable_DEH_MG;
import com.cai.game.paijiu.pj_deh_mq.PJTable_DEH_MQ;


public enum PJType {
	DEFAULT(-1) {
		@Override
		public PJTable createTable() {
			return new PJTable(this);
		}
	},
	// 溆浦十三张
	GAME_TYPE_PJ_DEH_JD(GameConstants.GAME_TYPE_DEH_JD) {
		@Override
		public PJTable createTable() {
				return new PJTable_DEH_JD();
			}
	},
	GAME_TYPE_PJ_DEH_MQ(GameConstants.GAME_TYPE_DEH_MQ) {
		@Override
		public PJTable createTable() {
				return new PJTable_DEH_MQ();
			}
	},
	GAME_TYPE_PJ_DEH_MG(GameConstants.GAME_TYPE_DEH_MG) {
		@Override
		public PJTable createTable() {
				return new PJTable_DEH_MG();
			}
	},
	;
	
	
	
	private final int value;

	PJType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PJTable createTable();


	public static PJType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PJType> maps = new HashMap<>();

	static {
		PJType[] temp = PJType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
