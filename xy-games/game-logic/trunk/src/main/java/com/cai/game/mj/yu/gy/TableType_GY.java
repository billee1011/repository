package com.cai.game.mj.yu.gy;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.mj.yu.gy.three.Table_GY_THREE;
import com.cai.game.mj.yu.gy.two.Table_GY_TWO;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_GY_ZJ_NEW, desc = "新贵阳捉鸡")
public final class TableType_GY extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_GY_ZJ_NEW, Table_GY.class);
		maping(GameConstants.GAME_TYPE_GY_EDG, Table_GY_TWO.class);
		maping(GameConstants.GAME_TYPE_GT_SDG, Table_GY_THREE.class);
	}
}
