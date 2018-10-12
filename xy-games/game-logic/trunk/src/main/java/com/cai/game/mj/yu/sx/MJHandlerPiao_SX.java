package com.cai.game.mj.yu.sx;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_SX extends AbstractMJHandler<Table_SX> {

	@Override
	public void exe(Table_SX table) {
		table._game_status = GameConstants_KWX.GS_MJ_PIAO; // 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);

		table.load_common_status(roomResponse);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = -1;
		}

		table.operate_player_data();

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);

			for (int j = 0; j <= table.get_ma_num(); j++) {
				roomResponse.addDouliuzi(j);
			}
			roomResponse.setPaoMin(table.get_ma_num() + 1);
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(Table_SX table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;
		table._player_result.pao[seat_index] = pao;
		table.player_mai_ma_count[seat_index] = pao;

		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants_KWX.GS_MJ_PIAO) {
			table._game_status = GameConstants_KWX.GS_MJ_PLAY;

			for (int p = 0; p < table.getTablePlayerNumber(); p++) {
				for (int mc = 0; mc < table.player_mai_ma_count[p]; mc++) {
					table._send_card_count++;
					table.player_mai_ma_data[p][mc] = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
					--table.GRR._left_card_count;
				}
			}

			table.operate_mai_ma_card(GameConstants_SX.INVALID_SEAT, false);
			table.on_game_start();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_SX table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		this.player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(Table_SX table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 发送数据
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		for (int j = 0; j <= table.get_ma_num(); j++) {
			roomResponse.addDouliuzi(j);
		}
		roomResponse.setPaoMin(table.get_ma_num() + 1);
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
