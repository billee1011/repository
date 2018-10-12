package com.cai.game.hh.handler.ldfpf.ai;

import org.apache.commons.lang.math.RandomUtils;

import protobuf.clazz.Protocol.RoomResponse;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.hh.HHAIGameLogic;
import com.cai.game.hh.HHTable;

@IRootAi(gameIds = {GameConstants.GAME_TYPE_PHZ_LD_FANG_PAO_FA},
	desc = "胡子牌操作", msgIds = {MsgConstants.RESPONSE_PLAYER_ACTION })


public class HHOperateCard extends AbstractAi<HHTable> {

	public HHOperateCard() {
	}

	@Override
	protected boolean isNeedExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OPR_CARD) {
			// System.out.println("ai操作失败:" +
			// table._playerStatus[seat_index].get_status());
			return false;
		}
		// if (player.future != null) {
		// player.future.cancel(false);
		// player.future = null;
		// }
		return true;
	}

	@Override
	public void onExe(HHTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		HHAIGameLogic.AI_Operate_Card(table, seat_index);
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
