package com.cai.game.laopai.handler.xp;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.laopai.handler.AbstractLPHandler;

import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class LPHandlerPao_XP extends AbstractLPHandler<LPTable_XP> {

	@Override
	public void exe(LPTable_XP table) {
		
		table._game_status = GameConstants.GS_MJ_PAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		roomResponse.setGameStatus(table._game_status);
		//有上庄
		table.operate_player_data();
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);//table._player_result.pao[i]
			roomResponse.setPaoMin(table._player_result.pao[i]);
			roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_LP_XP);
			roomResponse.setPaoDes("当前可以撤跑,最多下跑4个");
			table.send_response_to_player(i, roomResponse);
		}
	}
	
	public boolean handler_pao_qiang(LPTable_XP table,int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang || table._game_status!=GameConstants.GS_MJ_PAO)
			return false;
		if (pao<table._player_result.pao[seat_index])
			return false;
		table._playerStatus[seat_index]._is_pao_qiang = true;
		
		int p = table._player_result.pao[seat_index];
		
		table._player_result.pao[seat_index] = pao;
		
		if(p!=pao){
			table.operate_player_data();
		}
		
		for(int i = 0; i < table.getTablePlayerNumber(); i++){
			if(table._playerStatus[i]._is_pao_qiang==false){
				return true;
			}
		}
		table.on_game_start();
		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(LPTable_XP table,int seat_index) {
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);
		
		// TODO Auto-generated method stub
		
		this.player_reconnect(table, seat_index);
		return true;
	}
	
	private void player_reconnect(LPTable_XP table,int seat_index){
		if(table._playerStatus[seat_index]._is_pao_qiang == true){
			return;
		}
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据
		
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);//table._player_result.pao[i]
		roomResponse.setPaoMin(table._player_result.pao[seat_index]);
		roomResponse.setPaoMax(GameConstants.PAO_MAX_COUNT_LP_XP);
		roomResponse.setPaoDes("当前可以撤跑,最多下跑3个");
		table.send_response_to_player(seat_index, roomResponse);
		
		//table.load_room_info_data(roomResponse);
		//table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);	
		table.send_response_to_player(seat_index, roomResponse);
	}

}
