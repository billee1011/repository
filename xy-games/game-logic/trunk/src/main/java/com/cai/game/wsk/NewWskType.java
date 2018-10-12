package com.cai.game.wsk;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.wsk.handler.dcts.WSKTable_Dcts;
import com.cai.game.wsk.handler.gf_bawang.WSKTable_GF_BaWang;
import com.cai.game.wsk.handler.pingxiang.six.WSKTable_PXGT_SIX;
import com.cai.game.wsk.handler.pingxiang.three.WSKTable_PXGT_THREE;
import com.cai.game.wsk.handler.pingxiang.two.WSKTable_PXGT_TWO;
import com.cai.game.wsk.handler.yxzd.WSKTable_YXZD;
import com.cai.game.wsk.handler.zzshA.WSKTable_ZzshA;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.WSK_PX, desc = "升级")
public final class NewWskType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {

		/**
		 * 影射关系，maping(小游戏id，房间类对应的Class)
		 */
		maping(GameConstants.GAME_TYPE_WSK_PING_XIANG_2, WSKTable_PXGT_TWO.class);
		maping(GameConstants.GAME_TYPE_WSK_PING_XIANG_3, WSKTable_PXGT_THREE.class);
		maping(GameConstants.GAME_TYPE_WSK_PING_XIANG_6, WSKTable_PXGT_SIX.class);
		maping(GameConstants.GAME_TYPE_WSK_YXZD, WSKTable_YXZD.class);
		maping(GameConstants.GAME_TYPE_WSK_ZZSHA, WSKTable_ZzshA.class);
		maping(GameConstants.GAME_TYPE_WSK_GF_BAWANG, WSKTable_GF_BaWang.class);
		maping(GameConstants.GAME_TYPE_WSK_DCTS, WSKTable_Dcts.class);
	}
}
