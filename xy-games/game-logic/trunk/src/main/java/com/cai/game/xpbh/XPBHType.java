package com.cai.game.xpbh;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.ddz.handler.jxddz.DDZ_JX_Table;
import com.cai.util.IRoom;

/**
 * 
 * @IRoom:房间影射关系注解 
 * gameType:游戏大类型，{@link com.cai.common.define.EGameType}
 * desc:游戏描述
 * 
 * 
 */
@IRoom(gameType = EGameType.XPBH, desc = "溆浦博胡")
public final class XPBHType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {
		
		/**
		 * 影射关系，maping(小游戏id，房间类对应的Class)
		 */
		maping(GameConstants.GAME_TYPE_XP_BOHU, XPBHTable.class);
	}
}
