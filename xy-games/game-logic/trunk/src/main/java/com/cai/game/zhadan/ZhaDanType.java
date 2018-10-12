package com.cai.game.zhadan;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.zhadan.handler.ncgz.ZhaDanTable_NCGZ;
import com.cai.game.zhadan.handler.ncst.ZhaDanTable_NCST;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.WSK_NCST, desc = "南昌四团")
public final class ZhaDanType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {

		/**
		 * 影射关系，maping(小游戏id，房间类对应的Class)
		 */
		maping(GameConstants.GAME_TYPE_WSK_NCST, ZhaDanTable_NCST.class);
		maping(GameConstants.GAMR_TYPE_ZD_NCGZ, ZhaDanTable_NCGZ.class);
	}
}
