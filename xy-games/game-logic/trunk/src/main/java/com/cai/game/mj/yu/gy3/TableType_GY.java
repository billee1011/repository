package com.cai.game.mj.yu.gy3;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.mj.yu.gy3.three.Table_GY_THREE;
import com.cai.game.mj.yu.gy3.two.Table_GY_TWO;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_GY_3, desc = "新贵阳捉鸡")
public final class TableType_GY extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_GY_3, Table_GY.class);
		maping(GameConstants.GAMR_TYPE_GY_TWO_3, Table_GY_TWO.class);
		maping(GameConstants.GAMR_TYPE_GY_THREE_3, Table_GY_THREE.class);
	}
}
