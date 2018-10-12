package com.cai.game.mj.yu.sx;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_SX, desc = "松溪麻将")
public class SXType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_SX, Table_SX.class);
	}

}
