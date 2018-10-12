package com.cai.game.hjk.handler.hjk;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerCallBanker;

import protobuf.clazz.Protocol.CallBankerInfo_HJK;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_HJK;
import protobuf.clazz.Protocol.TableResponse_HJK;

public class HJKHandlerCallBanker_hjk extends HJKHandlerCallBanker{

	
	@Override
	public void exe(HJKTable table) {
		
	}
	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
 public boolean handler_call_banker(HJKTable table, int seat_index,int call_banker) {
		if(_game_status != GameConstants.GS_HJK_CALL_BANKER){
			table.log_error("游戏状态不对 "+ _game_status + "用户开牌 :" + GameConstants.GS_HJK_CALL_BANKER);
			return false;
		}
		if(table._call_banker[seat_index] != -1){
			table.log_error("你已经叫庄操作了 ");
			return false;
		}
		if(call_banker<0||call_banker>4)
		{
			table.log_error("您下注已经越界了");
		}
		table._call_banker[seat_index] = table._call_banker_info[call_banker];
		table.add_call_banker(seat_index);
		boolean flag = true;
		for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
			if(table._player_status[i] == true)
			{
				if(table._call_banker[i] == -1)
					flag = false;
			}
		}
		//游戏结束
		if(flag == true){
			table.game_start_hjk();

		}
		
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_HJK.Builder tableResponse = TableResponse_HJK.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for(int i =0; i<table.getTablePlayerNumber();i++){
			
			if((i == seat_index)&&(table._call_banker[i] == -1)&&(table._player_status[i] == true))
			{
				CallBankerInfo_HJK.Builder  call_banker_info = CallBankerInfo_HJK.newBuilder();
				for(int j = 0; j <= table._banker_max_times; j++){
					call_banker_info.addCallBankerInfo(table._call_banker_info[j]);
				}
				roomResponse_hjk.setCallBankerInfo(call_banker_info);
			}
			tableResponse.addCallBankerInfo(table._call_banker[i]);
			if(table.get_players()[i] != null&&table._player_status[i] == false)
			{
				tableResponse.addWaitSeatIndex(1);
			}
			else
				tableResponse.addWaitSeatIndex(0);
		}
	
		// 游戏变量
		tableResponse.setSceneInfo(table._game_status);
		
		roomResponse_hjk.setTableResponse(tableResponse);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		table.send_response_to_player(seat_index, roomResponse);
		if(table.get_players()[seat_index] != null && table._player_status[seat_index] == false){
			RoomResponse.Builder roomResponsewait = RoomResponse.newBuilder();
			roomResponsewait.setType(MsgConstants.RESPONSE_WAIT_RESPONSE);
			RoomResponse_HJK.Builder roomResponsewait_hjk = RoomResponse_HJK.newBuilder();
			roomResponsewait_hjk.setWaitSeatIndex(seat_index);
			roomResponsewait.setRoomResponseHjk(roomResponsewait_hjk);
			table.send_response_to_other(seat_index, roomResponsewait);
		}
		return true;
	}
		
}
