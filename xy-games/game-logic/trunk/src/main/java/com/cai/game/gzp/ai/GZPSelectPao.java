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
desc = "胡子牌出牌", msgIds = {
		MsgConstants.RESPONSE_PAO_QIANG_ACTION })

public class GZPSelectPao extends AbstractAi<GZPTable> {

	public GZPSelectPao() {
	}

	@Override
	protected boolean isNeedExe(GZPTable table, RobotPlayer player, RoomResponse rsp) {
		// if(player.future != null){
		// player.future.cancel(false);
		// player.future = null;
		// }
		if(player == null)
			return false;
		if (table._game_status != GameConstants.GS_MJ_PIAO)
			return false;
		if(table._playerStatus[player.get_seat_index()]._is_pao_qiang == true)
			return false;
		return true;
	}

	@Override
	public void onExe(GZPTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		GZPAIGameLogic.Set_Pao(table, seat_index);

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
