package com.cai.game.mj.guangdong.heyuan;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_HEYUAN_JD, desc = "河源鸡搭")
public final class TableType_HYJD extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_HEYUAN_JD, Table_HYJD.class);
	}
}
