package com.cai.game.mj.handler.yifeng;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerPao_YiFeng extends AbstractMJHandler<Table_YiFeng> {
	@Override
	public void exe(Table_YiFeng table) {
		table._game_status = GameConstants.GS_MJ_PIAO;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = 0;// 瓢
		}

		boolean bduihuo = false;
		bduihuo = table._cur_round % 5 == 0 ? true : false;
		if (!bduihuo)
			bduihuo = table._cur_round == 1 ? true : false;
		if (bduihuo) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._player_result.qiang[i] = 0;// 对火
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(3);
			roomResponse.setPaoDes("瓢0，瓢1，瓢2");
			// 对火
			if (bduihuo) {
				roomResponse.setQiang(table._player_result.qiang[i]);
				roomResponse.setQiangMin(0);
				roomResponse.setQiangMax(2);
				roomResponse.setQiangDes("上火、不上火");
			}
			table.send_response_to_player(i, roomResponse);
		}
		table.operate_player_data();
	}

	public boolean handler_pao_qiang(Table_YiFeng table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		table._player_result.pao[seat_index] = pao;

		if (table._cur_round == 1 || table._cur_round % 5 == 0) {
			table._player_result.qiang[seat_index] = qiang;
		}

		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PIAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;
			table.on_game_start_real();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_YiFeng table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		tableResponse.setBankerPlayer(table.GRR._banker_player);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(Table_YiFeng table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setPaoMin(0);
		roomResponse.setPaoMax(3);
		roomResponse.setPaoDes("瓢0，瓢1，瓢2");
		if (table._cur_round % 5 == 0 || table._cur_round == 1) {
			roomResponse.setQiang(table._player_result.qiang[seat_index]);
			roomResponse.setQiangMin(0);
			roomResponse.setQiangMax(2);
			roomResponse.setQiangDes("上火、不上火");
		}
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
