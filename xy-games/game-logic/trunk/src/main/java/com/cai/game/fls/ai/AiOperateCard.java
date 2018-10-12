package com.cai.game.fls.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.fls.AiGameLogic;
import com.cai.game.fls.FLSTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_FLS_LX, GameConstants.GAME_TYPE_FLS_LX_TWENTY, GameConstants.GAME_TYPE_FLS_LX_THREE,
		GameConstants.GAME_TYPE_FLS_LX_TWO }, desc = "福禄寿操作牌", msgIds = { MsgConstants.RESPONSE_PLAYER_ACTION })
public class AiOperateCard extends AbstractAi<FLSTable> {
	@Override
	protected boolean isNeedExe(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int status = table._playerStatus[seat_index].get_status();
		if (status != GameConstants.Player_Status_OPR_CARD) {
			return false;
		}
		return true;
	}

	@Override
	public void onExe(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		AiGameLogic.ai_operate_card(table, seat_index);
	}

	@Override
	protected AiWrap needDelay(FLSTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(3000);
		}

		return new AiWrap(true, table.getDelay_play_card_time());
	}

	@Override
	public long getMaxTrusteeTime(FLSTable table) {
		long delay = table.getDelay_play_card_time();
		return delay;
	}
}
