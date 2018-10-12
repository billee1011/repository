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

@IRootAi(gameIds = { GameConstants.GAME_TYPE_WSK_PC_ZD}, desc = "蒲城炸弹", msgIds = {  MsgConstants.RESPONSE_WSK_PC_OUT_CARD })
public class PcdzGameStartAi extends AbstractAi<WSKTable_PCDZ> {

	public PcdzGameStartAi() {
	}

	@Override
	protected boolean isNeedExe(WSKTable_PCDZ table, RobotPlayer player, RoomResponse rsp) {
		int seat_index = player.get_seat_index();
		int current_player = table._current_player;
		return current_player == seat_index;
	}

	@Override
	public void onExe(WSKTable_PCDZ table, RobotPlayer player, RoomResponse rsp) {
 		int seat_index = player.get_seat_index();
		int current_player = table._current_player;

		if (current_player == seat_index) {
			int card_data[] = new int[27];
			int out_card_count = table._logic.Ai_Out_Card(table.GRR._cards_data[current_player], table.GRR._card_count[current_player],
					table._turn_out_card_data, table._turn_out_card_count,table._turn_three_link_num ,card_data);
			if (out_card_count != 0) {
				table._handler = table._handler_out_card_operate;
				table._handler_out_card_operate.reset_status(current_player, card_data, out_card_count, 1);
				table._handler_out_card_operate.exe(table);
			}
			else{
				table._handler = table._handler_out_card_operate;
				table._handler_out_card_operate.reset_status(current_player, card_data, out_card_count, 0);
				table._handler_out_card_operate.exe(table);
			}
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
