package com.cai.game.mj.handler.cs;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_CS extends MJHandler {

	@Override
	public void exe(MJTable table) {

		table._game_status = GameConstants.GS_MJ_PIAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		if (table.is_mj_type(GameConstants.GAME_TYPE_THJ_CS)) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._player_result.pao[i] = -1;
			}
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._player_result.pao[i] < 0) {
					table._player_result.pao[i] = 0;
				}
			}
		}

		table.operate_player_data();
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_result.pao[i] >= 5) {
				handler_pao_qiang(table, i, 5, 0);
				continue;
			}
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);// table._player_result.pao[i]
			roomResponse.setPaoMin(table._player_result.pao[i]);
			roomResponse.setPaoMax(5);
			roomResponse.setPaoDes("最多飘5个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		int p = table._player_result.pao[seat_index];

		table._player_result.pao[seat_index] = pao;

		if (p != pao) {
			table.operate_player_data();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PIAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;
			// 游戏开始
			table.game_start_cs_real();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		if (table._shang_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._shang_zhuang_player);
		} else if (table._lian_zhuang_player != GameConstants.INVALID_SEAT) {
			tableResponse.setBankerPlayer(table._lian_zhuang_player);
		} else {
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// TODO Auto-generated method stub

		this.player_reconnect(table, seat_index);
		return true;
	}

	private void player_reconnect(MJTable table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);// table._player_result.pao[i]
		roomResponse.setPaoMin(table._player_result.pao[seat_index]);
		roomResponse.setPaoMax(5);
		roomResponse.setPaoDes("做多飘5个");
		table.send_response_to_player(seat_index, roomResponse);

		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}

}
