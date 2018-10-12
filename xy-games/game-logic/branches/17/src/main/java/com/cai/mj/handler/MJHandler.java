package com.cai.mj.handler;

import com.cai.common.domain.Player;
import com.cai.mj.MJTable;

import protobuf.clazz.Protocol.RoomRequest;

public abstract class MJHandler {
	
	public abstract void  exe(MJTable table);
	
	

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
