package com.cai.game.hh.ai;

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
import com.cai.game.hh.HHAIGameLogic;
import com.cai.game.hh.HHTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_PHZ_HS, GameConstants.GAME_TYPE_HH_YX, GameConstants.GAME_TYPE_PHZ_YX,
		GameConstants.GAME_TYPE_468_HONG_GUAI_WAN, GameConstants.GAME_TYPE_HGW_HH, GameConstants.GAME_TYPE_LHQ_HD, GameConstants.GAME_TYPE_LHQ_HY,
		GameConstants.GAME_TYPE_LHQ_QD, GameConstants.GAME_TYPE_LHQ_QD_SAPP, GameConstants.GAME_TYPE_PHZ_XT,
		GameConstants.GAME_TYPE_NEW_PHZ_CHEN_ZHOU }, desc = "胡子牌操作", msgIds = { MsgConstants.RESPONSE_PLAYER_ACTION })

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
