package com.cai.game.ddz.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EGameType;
import com.cai.dictionary.SysParamServerDict;
import com.cai.game.ddz.DDZAIGameLogic;
import com.cai.game.ddz.DDZTable;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_DDZ_JD, GameConstants.GAME_TYPE_DDZ_HENAN, }, desc = "斗地主", msgIds = {
		MsgConstants.RESPONSE_DDZ_CALL_BANKER })
public class DDZCallBanker extends AbstractAi<DDZTable> {

	public DDZCallBanker() {
	}

	@Override
	protected boolean isNeedExe(DDZTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;
		return current_player == seat_index;
	}

	@Override
	public void onExe(DDZTable table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();

		if(table.has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU) || table.has_rule(GameConstants.GAME_RULE_JIAO_FEN)){
			DDZAIGameLogic.AI_Land_Score(table, seat_index);
		}else{
			DDZAIGameLogic.AI_QIANG_DI_ZHU(table, seat_index);
		}
		
	}

	@Override
	protected AiWrap needDelay(DDZTable table, RobotPlayer player, RoomResponse rsp) {
		if (player.isRobot()) {
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}

		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}
		// 超时出牌
		return new AiWrap(false, SysParamServerDict.getInstance().getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(9).getVal3());
	}

}
