package com.cai.game.mj.ai.honzhong;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.AbstractMJTable;
import com.cai.game.mj.MJAIGameLogic;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_HALL_HONG_ZHONG_MJ,GameConstants.GAME_TYPE_HALL_ZHUAN_ZHUAN_MJ   }, desc = "红中麻将操作", msgIds = {
		MsgConstants.RESPONSE_PLAYER_ACTION } ,roomType = 7)
public class MJOperateCard extends AbstractAi<AbstractMJTable> {

	public MJOperateCard() {
	}

	@Override
	protected boolean isNeedExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OPR_CARD){
			return false;
		}
		return true;
	}

	@Override
	public void onExe(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		MJAIGameLogic.AI_Operate_Card_ALL(table, seat_index);
	}

	@Override
	protected AiWrap needDelay(AbstractMJTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(3000);
		}
		// 超时出牌
		return new AiWrap(true, table.getDelay_play_card_time());
	}

	@Override
	public long getMaxTrusteeTime(AbstractMJTable table) {
		long delay = table.getDelay_play_card_time();
		return delay;
	}
}
