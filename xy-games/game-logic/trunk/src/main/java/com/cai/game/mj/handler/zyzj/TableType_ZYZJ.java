package com.cai.game.mj.handler.zyzj;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.mj.yu.gy.three.Table_GY_THREE;
import com.cai.game.mj.yu.gy.two.Table_GY_TWO;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_ZYZJ, desc = "遵义捉鸡")
public final class TableType_ZYZJ extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_ZHZJ, Table_ZYZJ.class);
	}
}
