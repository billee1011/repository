package com.cai.game.mj.guangdong.heyuanhd;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

//河源惠搭与河源鸡搭共用一个APPID
@IRoom(gameType = EGameType.MJ_HEYUAN_JD, desc = "河源惠搭")
public final class TableType_HYHD extends AbstractGameTypeTable {
	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_HEYUAN_HD, Table_HYHD.class);
	}
}
