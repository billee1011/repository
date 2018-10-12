package com.cai.game.mj.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.fls.FLSTable;
import com.cai.game.mj.MJTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class MJHandler {
	
	public abstract void  exe(MJTable table);
	
	//自动托管操作
	protected boolean handler_check_auto_behaviour(MJTable table, int seat_index, int card_data){
		if(!table.is_sys() || !table.isTrutess(seat_index))//非金币场 无托管
			return false;
		
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		if(curPlayerStatus.has_action()){//有动作
			table.operate_player_action(seat_index, true);
			if (curPlayerStatus.has_zi_mo() && card_data != GameConstants.INVALID_VALUE) {
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, card_data);
			}else if( curPlayerStatus.has_chi_hu() && card_data != GameConstants.INVALID_VALUE ){
					table.exe_jian_pao_hu(seat_index, GameConstants.WIK_CHI_HU, card_data);
			}else{
				table.exe_jian_pao_hu(seat_index, GameConstants.WIK_NULL, card_data);
			}
			return true;
		}else if(curPlayerStatus.get_status() == GameConstants.Player_Status_OUT_CARD){	//出牌状态
			
			int out_card = GameConstants.INVALID_VALUE;			
			int card_index = table._logic.switch_to_card_index(card_data);
			if(card_index != GameConstants.MAX_INDEX && table.GRR._cards_index[seat_index][card_index] > 0){//手牌有
				out_card = card_data;
			}else{
				for (int i = 0; i < GameConstants.MAX_FLS_INDEX; i++) {
					if(table.GRR._cards_index[seat_index][i] > 0){//托管 随意出一张牌
						out_card = table._logic.switch_to_card_data(i);
					}
				}			
			}
		
			if(out_card != GameConstants.INVALID_VALUE){
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, out_card),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);	
				return true;
			}
		}
		
		return false;
	}
	
	
	public boolean handler_be_set_trustee(MJTable table, int seat_index){
		handler_check_auto_behaviour(table,seat_index,GameConstants.INVALID_VALUE);
		return false;
	}
	//准备
	public boolean handler_player_ready(MJTable table,int seat_index) {
		return table.handler_player_ready(seat_index);
	}
	
	
	public boolean handler_player_be_in_room(MJTable table,int seat_index) {
		
		

		return true;

	}
	
	public boolean handler_player_out_card(MJTable table,int seat_index, int card){
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
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		return false;
	}
	
	public boolean handler_release_room(MJTable table,Player player, int opr_code) {
		 return table.handler_release_room(player, opr_code);
	}
	
	public boolean handler_audio_chat(MJTable table,Player player, com.google.protobuf.ByteString chat, int l,float audio_len) {
		return false;
	}
	
	
	public boolean handler_requst_audio_chat(int room_id,long account_id, RoomRequest room_rq){
//		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
//		
//		if(table==null){
//			return false;
//		}
//		
//		Player player = table.get_player(account_id);
//		if(player==null){
//			return false;
//		}
//		
//		//逻辑处理
//		boolean r =  table.handler_audio_chat(player, room_rq.getAudioChat(),room_rq.getAudioSize(),room_rq.getAudioLen());
		
		
		return true;
	}
	
	public boolean handler_requst_emjoy_chat(int room_id,long account_id, RoomRequest room_rq){
//		MJTable table = (MJTable)PlayerServiceImpl.getInstance().getRoomMap().get(room_id);
//		
//		if(table==null){
//			return false;
//		}
//		
//		Player player = table.get_player(account_id);
//		if(player==null){
//			return false;
//		}
//		
//		//逻辑处理
//		boolean r =  table.handler_emjoy_chat(player, room_rq.getEmjoyId());
		
		
		return true;
	}
}
