package com.cai.game.wsk.tongcheng;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.PK_TONG_CHENG, desc = "通城打滚")
public final class TableType_TongCheng extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_PK_TONG_CHENG, Table_TongCheng.class);
	}
}
