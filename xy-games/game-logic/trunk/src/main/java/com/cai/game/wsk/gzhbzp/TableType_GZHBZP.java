package com.cai.game.wsk.gzhbzp;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.GZH_BZP, desc = "贵州板子炮")
public final class TableType_GZHBZP extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_GZH_BZP, Table_GZHBZP.class);
	}
}
