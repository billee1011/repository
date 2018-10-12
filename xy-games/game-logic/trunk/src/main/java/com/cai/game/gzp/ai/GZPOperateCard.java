package com.cai.game.gzp.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.GZPAIGameLogic;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_GZP,GameConstants.GAME_TYPE_GZP_DDWF},
	desc = "胡子牌操作", msgIds = {MsgConstants.RESPONSE_PLAYER_ACTION })


public class GZPOperateCard extends AbstractAi<GZPTable> {

	public GZPOperateCard() {
	}

	@Override
	protected boolean isNeedExe(GZPTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		if (!(table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OPR_CARD||
				table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OPR_OR_OUT_CARD)||(table._playerStatus[seat_index].has_action() == false)) {
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
	public void onExe(GZPTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		GZPAIGameLogic.AI_Operate_Card(table, seat_index);
	}

	@Override
	protected AiWrap needDelay(GZPTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}
		// 超时出牌
		int delay = 10000;
		SysParamModel sysParamModelAI = SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(table._game_type_index)).get(10);
		if (sysParamModelAI != null) {
			delay = sysParamModelAI.getVal1();
		}
		return new AiWrap(true, table.getDelay_play_card_time());
	}

}
