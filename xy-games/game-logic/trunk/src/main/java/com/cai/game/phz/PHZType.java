package com.cai.game.phz;

import java.util.HashMap;
import java.util.Map;

import com.cai.common.constant.GameConstants;
import com.cai.game.phz.handler.nxghz.NXGHZTable;
import com.cai.game.phz.handler.yiyangwhz.YiYangWHZTable;
import com.cai.game.phz.handler.yjghz.YuanJiangGHZTable;
import com.cai.game.phz.handler.yywhz.YYWHZTable;

public enum PHZType {
	DEFAULT(-1) {
		@Override
		public PHZTable createTable() {
			return new PHZTable();
		}
	},
	// 岳阳歪胡子
	GAME_TYPE_YYWHZ(GameConstants.GAME_TYPE_WHZ_YY) {
		@Override
		public PHZTable createTable() {
			return new YYWHZTable();
		}
	},
	GAME_TYPE_NXGHZ(GameConstants.GAME_TYPE_GHZ_NX) {
		@Override
		public NXGHZTable createTable() {
			return new NXGHZTable();
		}
	},
	// 益阳歪胡子
	GAME_TYPE_YIYANGWHZ(GameConstants.GAME_TYPE_WHZ_YIYANG) {
		@Override
		public PHZTable createTable() {
			return new YiYangWHZTable();
		}
	},

	// 沅江鬼胡子
	GAME_TYPE_GHZ_YJ(GameConstants.GAME_TYPE_GHZ_YJ) {
		@Override
		public PHZTable createTable() {
			return new YuanJiangGHZTable();
		}
	},;

	private final int value;

	PHZType(int value) {
		this.value = value;
	}

	public int getValue() {
		return this.value;
	}

	// 创建麻将桌子
	public abstract PHZTable createTable();

	public static PHZType getType(int typeValue) {
		return maps.get(typeValue);
	}

	private static final Map<Integer, PHZType> maps = new HashMap<>();

	static {
		PHZType[] temp = PHZType.values();
		for (int i = 0; i < temp.length; i++) {
			maps.put(temp[i].value, temp[i]);
		}
	}
}
