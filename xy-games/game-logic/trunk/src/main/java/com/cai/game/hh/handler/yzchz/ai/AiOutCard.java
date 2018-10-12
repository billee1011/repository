package com.cai.game.hh.handler.yzchz.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.AiGameLogic;
import com.cai.game.hh.handler.yzchz.Table_YongZhou;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_PHZ_YONG_ZHOU }, desc = "永州扯胡子Ai出牌", msgIds = { MsgConstants.RESPONSE_PLAYER_STATUS })
public class AiOutCard<T extends Table_YongZhou> extends AbstractAi<T> {
	public AiOutCard() {
	}

	@Override
	protected boolean isNeedExe(T table, RobotPlayer player, RoomResponse rsp) {
		if (table._playerStatus[player.get_seat_index()].get_status() != GameConstants.Player_Status_OUT_CARD)
			return false;

		return true;
	}

	@Override
	public void onExe(T table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		AiGameLogic.aiOutCard(table, seat_index);
	}

	@Override
	protected AiWrap needDelay(T table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}

		return new AiWrap(true, table.getDelay_play_card_time());
	}
}
