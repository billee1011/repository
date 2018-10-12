package com.cai.game.mj.hunan.zhuzhouwang;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;


@IRoom(gameType = EGameType.MJ_ZHUZHOU_WANG, desc = "株洲王麻将")
public final class TableType_ZhuZhouWang extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_ZHUZHOU_WANG, Table_ZhuZhouWang.class);
	}
}
