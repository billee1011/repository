package com.cai.game.wsk.xiangtanxiaozao;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.WSK_XTXZ, desc = "湘潭消造")
public final class TableType_XTXZ extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_WSK_XTXZ, Table_XTXZ.class);
	}
}
