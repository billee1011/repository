package com.cai.game.shengji;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.shengji.handler.lldq.SJTable_LLDQ;
import com.cai.game.shengji.handler.nceqw.SJTable_NCEQW;
import com.cai.game.shengji.handler.wzdd.SJTable_WZDD;
import com.cai.game.shengji.handler.xfgd_four.SJTable_XFGD_Four;
import com.cai.game.shengji.handler.xfgd_three.SJTable_XFGD_Three;
import com.cai.game.shengji.handler.xp240.SJTable_XP_240;
import com.cai.game.shengji.handler.yz240.SJTable_YZ_240;
import com.cai.game.shengji.handler.yzbp.SJTable_YZBP;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.SJ_XF_GD, desc = "升级")
public final class SJType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {

		/**
		 * 影射关系，maping(小游戏id，房间类对应的Class)
		 */
		maping(GameConstants.GAME_TYPE_XF_GD_FOUR, SJTable_XFGD_Four.class);
		maping(GameConstants.GAME_TYPE_XF_GD_THREE, SJTable_XFGD_Three.class);
		maping(GameConstants.GAME_TYPE_LLDAQI, SJTable_LLDQ.class);
		maping(GameConstants.GAME_TYPE_XP_240, SJTable_XP_240.class);
		maping(GameConstants.GAME_TYPE_WZ_DD, SJTable_WZDD.class);
		maping(GameConstants.GAME_TYPE_YZBP, SJTable_YZBP.class);
		maping(GameConstants.GAME_TYPE_SJ_YZ240, SJTable_YZ_240.class);
		maping(GameConstants.GAME_TYPE_NC_EQW, SJTable_NCEQW.class);
	}
}
