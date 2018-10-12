package com.cai.game.hh.ai;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;

import com.cai.game.hh.HHTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU }, desc = "跑胡子飘分、跑分", msgIds = { MsgConstants.RESPONSE_PAO_QIANG_ACTION })
public class HHPiao extends AbstractAi<HHTable> {
	@Override
	protected boolean isNeedExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		if (table._game_status != GameConstants.GS_MJ_PIAO && table._game_status != GameConstants.GS_MJ_PAO) {
			return false;
		}
		return true;
	}

	@Override
	public void onExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		table.handler_requst_pao_qiang(player, 0, 0);
	}

	@Override
	protected AiWrap needDelay(HHTable table, RobotPlayer player, RoomResponse rsp) {
		boolean flag = player.isIsTrusteeOver();

		if (table.isClubMatch()) {
			flag = false;
		}

		return new AiWrap(flag, 5000);
	}
}
