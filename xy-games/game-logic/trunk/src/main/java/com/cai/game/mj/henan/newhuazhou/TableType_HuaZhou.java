package com.cai.game.mj.henan.newhuazhou;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_HN_HUAZHOU, desc = "河南滑州")
public final class TableType_HuaZhou extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_HN_HUAZHOU, Table_HuaZhou.class);
	}
}
