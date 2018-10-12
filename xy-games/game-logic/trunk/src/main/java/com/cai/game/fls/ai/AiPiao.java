package com.cai.game.fls.ai;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.fls.FLSTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_FLS_LX, GameConstants.GAME_TYPE_FLS_LX_TWENTY, GameConstants.GAME_TYPE_FLS_LX_THREE,
		GameConstants.GAME_TYPE_FLS_LX_TWO }, desc = "福禄寿飘分", msgIds = { MsgConstants.RESPONSE_PAO_QIANG_ACTION })
public class AiPiao extends AbstractAi<FLSTable> {
	@Override
	protected boolean isNeedExe(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		if (table._game_status != GameConstants.GS_MJ_PIAO && table._game_status != GameConstants.GS_MJ_PAO) {
			return false;
		}
		return true;
	}

	@Override
	public void onExe(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		table.handler_requst_pao_qiang(player, 0, 0);
	}

	@Override
	protected AiWrap needDelay(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		boolean flag = player.isIsTrusteeOver();
		if(table.isClubMatch()){
			flag = false;
		}
		return new AiWrap(flag, 5000);
	}
}
