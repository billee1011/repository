package com.cai.game.phu.ai;

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
import com.cai.game.phu.PHAIGameLogic;
import com.cai.game.phu.PHTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_PHU_YX,  }, desc = "碰胡操作", msgIds = {
		MsgConstants.RESPONSE_PLAYER_ACTION })
public class PHOperateCard extends AbstractAi<PHTable> {

	public PHOperateCard() {
	}

	@Override
	protected boolean isNeedExe(PHTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OPR_CARD){
			//System.out.println("ai操作失败:" + table._playerStatus[seat_index].get_status());
			return false;
		}
//		if (player.future != null) {
//			player.future.cancel(false);
//			player.future = null;
//		}
		return true;
	}

	@Override
	public void onExe(PHTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		PHAIGameLogic.AI_Operate_Card(table, seat_index);
	}

	@Override
	protected AiWrap needDelay(PHTable table, RobotPlayer player, RoomResponse rsp) {
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
		if(sysParamModelAI != null)
		{
			delay = sysParamModelAI.getVal1();
		}
		return new AiWrap(true, delay);
	}

}
