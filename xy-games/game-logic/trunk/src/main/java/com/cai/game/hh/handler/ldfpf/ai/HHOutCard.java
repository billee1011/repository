package com.cai.game.hh.handler.ldfpf.ai;

import org.apache.commons.lang.math.RandomUtils;

import protobuf.clazz.Protocol.RoomResponse;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.handler.ldfpf.LouDiFangPaoFaUtils;


@IRootAi(gameIds = {GameConstants.GAME_TYPE_PHZ_LD_FANG_PAO_FA}, desc = "胡子牌出牌", msgIds = {
		MsgConstants.RESPONSE_PLAYER_STATUS })

public class HHOutCard extends AbstractAi<HHTable> {

	public HHOutCard() {
	}

	@Override
	protected boolean isNeedExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		// if(player.future != null){
		// player.future.cancel(false);
		// player.future = null;
		// }

		if (table._playerStatus[player.get_seat_index()].get_status() != GameConstants.Player_Status_OUT_CARD)
			return false;
		return true;
	}

	@Override
	public void onExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return ;
		int card = LouDiFangPaoFaUtils.get_ai_out_card(table, seat_index);
		table.handler_player_out_card(seat_index, card);
	}

	@Override
	protected AiWrap needDelay(HHTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}
		return new AiWrap(true, table.getDelay_play_card_time());
	}

}
