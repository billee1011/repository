package com.cai.game.dtz;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.PK_DTZ, desc = "打筒子")
public class DtzType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_PK_DTZ, Table_DTZ.class);
	}
}
