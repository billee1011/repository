package com.cai.game.wsk.handler.pcdz.ai;

import org.apache.commons.lang.math.RandomUtils;

import com.cai.ai.AbstractAi;
import com.cai.ai.AiWrap;
import com.cai.ai.IRootAi;
import com.cai.ai.RobotPlayer;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.wsk.handler.pcdz.WSKTable_PCDZ;

import protobuf.clazz.Protocol.RoomResponse;

@IRootAi(gameIds = { GameConstants.GAME_TYPE_WSK_PC_ZD}, desc = "蒲城炸弹", msgIds = {  MsgConstants.RESPONSE_WSK_PC_CALL_BANKER_RESULT ,MsgConstants.RESPONSE_WSK_PC_GAME_START})
public class PcdzCallBankerAi extends AbstractAi<WSKTable_PCDZ> {

	public PcdzCallBankerAi() {
	}

	@Override
	protected boolean isNeedExe(WSKTable_PCDZ table, RobotPlayer player, RoomResponse rsp) {
		
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;
		if ((table._is_call_banker[seat_index] == 1 )&&table._game_status !=  GameConstants.GS_PCWSK_LIANG_PAI) {
			return false;
		}
//		else if(table._game_status ==  GameConstants.GS_PCWSK_LIANG_PAI){
//			return false;
//		}
		return true;
	}

	@Override
	public void onExe(WSKTable_PCDZ table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;

		if (table._is_call_banker[seat_index] == 0&&table._game_status !=  GameConstants.GS_PCWSK_LIANG_PAI) {
			table._handler_call_banker.handler_call_banker(table, seat_index, 0);
		}
		else if(current_player == seat_index &&table._game_status ==  GameConstants.GS_PCWSK_LIANG_PAI){
			int card_data = 0;
			table.deal_liang_pai(seat_index, card_data);
		}
	}

	@Override
	protected AiWrap needDelay(WSKTable_PCDZ table, RobotPlayer player, RoomResponse rsp) {
		if(player.isRobot()){
			return new AiWrap(RandomUtils.nextInt(3000) + 2000);
		}
		//断线后是否加速
		if (player.isAuto()) {
			return new AiWrap(RandomUtils.nextInt(1000) + 2000);
		} 
		
		if (table.istrustee[player.get_seat_index()]) {
			return new AiWrap(2000);
		}
		// 超时出牌
		return new AiWrap(true, table.getDelay_play_card_time());
	}
	
	@Override
	public long getMaxTrusteeTime(WSKTable_PCDZ table) {
		int delay = table.getDelay_play_card_time();
		return delay;
	}

}
