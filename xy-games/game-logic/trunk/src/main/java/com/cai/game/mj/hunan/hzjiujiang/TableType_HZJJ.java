package com.cai.game.mj.hunan.hzjiujiang;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_JIUJIANG_HZ, desc = "九江红中")
public final class TableType_HZJJ extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_JIUJIANG_HZ, MJTable_HZJJ.class);
	}
}
