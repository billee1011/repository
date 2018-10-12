package com.cai.game.mj.yingjing;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_YING_JING, desc = "荥经麻将")
public final class TableType_YingJing extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_YING_JING, MJTable_YingJing.class);
	}
}
