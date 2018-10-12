package com.cai.game.mj.yu.dcwdh.handler;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_DCWDH;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.yu.dcwdh.MJTable_DCWDH;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerMaiMa_DCWDH extends AbstractMJHandler<MJTable_DCWDH> {

	@Override
	public void exe(MJTable_DCWDH table) {
		table._game_status = GameConstants_DCWDH.GS_MJ_PIAO; // 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);

		table.load_common_status(roomResponse);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = -1;// 设置为报听状态
			table.pao[i] = -1;
		}

		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.addDouliuzi(0);
			roomResponse.addDouliuzi(1);
			roomResponse.setPaoMin(2);
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_DCWDH table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		table._player_result.pao[seat_index] = pao;
		table.pao[seat_index] = pao;

		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants_KWX.GS_MJ_PIAO) {
			table._game_status = GameConstants_KWX.GS_MJ_PLAY;
			table.on_game_start_real();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_DCWDH table, int seat_index) {
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

	private void player_reconnect(MJTable_DCWDH table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 发送数据
		roomResponse.setTarget(seat_index);
		roomResponse.addDouliuzi(0);
		roomResponse.addDouliuzi(1);
		roomResponse.setPaoMin(2);
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
