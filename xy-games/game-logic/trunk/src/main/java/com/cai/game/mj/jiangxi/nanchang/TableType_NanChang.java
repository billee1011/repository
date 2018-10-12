package com.cai.game.mj.jiangxi.nanchang;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_NANCHANG, desc = "南昌麻将")
public final class TableType_NanChang extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_NANCHANG, MJTable_NanChang.class);
	}
}
