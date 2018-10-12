package com.cai.game.mj.yu.trgy;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_MY_GY, desc = "麻友贵阳捉鸡")
public final class TableType_GY extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAMR_TYPE_MJ_GY_TRZJ, Table_GY.class);
	}
}
