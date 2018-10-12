package com.cai.game.mj.yu.dcwdh;

import com.cai.common.constant.GameConstants;
import com.cai.common.define.EGameType;
import com.cai.game.AbstractGameTypeTable;
import com.cai.util.IRoom;

@IRoom(gameType = EGameType.MJ_DCWDH, desc = "都昌无档胡")
public class DCWDHType extends AbstractGameTypeTable {

	@Override
	public void doMaping() {
		maping(GameConstants.GAME_TYPE_MJ_DCWDH, MJTable_DCWDH.class);
	}

}
