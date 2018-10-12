package com.cai.game.ddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.game.ddz.handler.dlddz.DDZ_DL_Table;
import com.cai.game.ddz.handler.jxddz.DDZ_JX_Table;
import com.cai.game.ddz.handler.klddz.DDZ_KL_Table;
import com.cai.game.ddz.handler.lfddz.DDZ_LF_Table;
import com.cai.game.ddz.handler.lps2ddz.DDZ_LPS2_Table;
import com.cai.game.ddz.handler.lps3ddz.DDZ_LPS3_Table;
import com.cai.game.ddz.handler.txw.TXW_Table;
import com.cai.util.IRoom;

/**
 * 
 * @IRoom:房间影射关系注解 gameType:游戏大类型，{@link com.cai.common.define.EGameType}
 *                 desc:游戏描述
 * 
 * 
 */
@IRoom(gameType = EGameType.DDZ, desc = "斗地主")
public final class DDZType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {

		/**
		 * 影射关系，maping(小游戏id，房间类对应的Class)
		 */
		maping(GameConstants.GAME_TYPE_DDZ_JX, DDZ_JX_Table.class);
		maping(GameConstants.GAME_TYPE_DDZ_KL, DDZ_KL_Table.class);
		maping(GameConstants.GAME_TYPE_DDZ_LPS2, DDZ_LPS2_Table.class);
		maping(GameConstants.GAME_TYPE_DDZ_LPS3, DDZ_LPS3_Table.class);
		maping(GameConstants.GAME_TYPE_DDZ_LF, DDZ_LF_Table.class);
		maping(GameConstants.GAME_TYPE_TXW, TXW_Table.class);
		maping(GameConstants.GAME_TYPE_DDZ_DL, DDZ_DL_Table.class);
	}
}
