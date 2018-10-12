package com.cai.game.mj.jiangxi.pxzz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_PXZZ extends AbstractMJHandler<MJTable_PXZZ> {
	@Override
	public void exe(MJTable_PXZZ table) {
		table._game_status = GameConstants.GS_MJ_PIAO;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
/*		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_result.pao[i] < 0) {
				table._player_result.pao[i] = 0;
			}
		}*/
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.addDouliuzi(-1);
		}
		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_result.pao[i] >= 3) {
				handler_pao_qiang(table, i, 3, 0);
				continue;
			}
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(table._player_result.pao[i]);
			roomResponse.setPaoMax(3);
			roomResponse.setPaoDes("最多买3分");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_PXZZ table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;
		
		
		table._playerStatus[seat_index]._is_pao_qiang = true;

		table._player_result.pao[seat_index] = pao;
		
		table.operate_player_data();
		//给客户端发送买子已完成的状态 例：[-1,1,-1,1]
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		table.load_room_info_data(roomResponse);
		for (int j = 0; j < table.getTablePlayerNumber(); j++) {
			if(table._player_result.pao[j]>=0) {
				roomResponse.addDouliuzi(1);
			}else{
				roomResponse.addDouliuzi(-1);
			}
		}
		//roomResponse.setType(MsgConstants.RESPONSE_BIAO_YAN_ACTION);
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.send_response_to_player(i, roomResponse);
		}

		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PIAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;
			table.on_game_start_hz_real();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_PXZZ table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table._cur_banker);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(MJTable_PXZZ table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		//给重连玩家发送其他玩家的买子状态 为1表示已经买了 -1表示正在买
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(table._player_result.pao[i]>=0) {
				roomResponse.addDouliuzi(1);
			}else{
				roomResponse.addDouliuzi(-1);
			}
		}
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setPaoMin(table._player_result.pao[seat_index]);
		roomResponse.setPaoMax(3);
		roomResponse.setPaoDes("最多买3分");
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
