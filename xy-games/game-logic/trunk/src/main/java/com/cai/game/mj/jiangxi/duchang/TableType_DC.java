package com.cai.game.mj.jiangxi.duchang;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_JX_DUCHANG_ZB, desc = "都昌栽宝")
public final class TableType_DC extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_JX_DUCHANG_ZB, MJTable_DC.class);
	}
}
