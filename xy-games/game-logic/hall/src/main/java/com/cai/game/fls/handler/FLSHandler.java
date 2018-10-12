package com.cai.game.fls.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.fls.FLSTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class FLSHandler {

	public abstract void exe(FLSTable table);

	// 准备
	public boolean handler_player_ready(FLSTable table, int seat_index) {
		return table.handler_player_ready(seat_index);
	}

	public boolean handler_player_be_in_room(FLSTable table, int seat_index) {
		return true;
	}

	public boolean handler_player_out_card(FLSTable table, int seat_index, int card) {
		return false;
	}
	
	public boolean handler_be_set_trustee(FLSTable table, int seat_index){
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		if(curPlayerStatus.has_action()){
			table.operate_player_action(seat_index, true);
			table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, 0);
		}else if(curPlayerStatus.get_status() == GameConstants.Player_Status_OUT_CARD){	
			
			int out_card = GameConstants.INVALID_VALUE;			
			for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
				if(table.GRR._cards_index[seat_index][i] > 0){//托管 随意出一张牌
					out_card = table._logic.switch_to_card_data(i);
				}
			}			
			if(out_card != GameConstants.INVALID_VALUE){
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, out_card),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);		
			}
		}
		return false;
	}
	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	public boolean handler_operate_card(FLSTable table, int seat_index, int operate_code, int operate_card) {
		return false;
	}

	public boolean handler_release_room(FLSTable table, Player player, int opr_code) {
		return table.handler_release_room(player, opr_code);
	}

	public boolean handler_audio_chat(FLSTable table, Player player, com.google.protobuf.ByteString chat, int l,
			float audio_len) {
		return false;
	}

	public boolean handler_requst_audio_chat(int room_id, long account_id, RoomRequest room_rq) {
		return true;
	}

	public boolean handler_requst_emjoy_chat(int room_id, long account_id, RoomRequest room_rq) {

		return true;
	}
}
