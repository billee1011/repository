package com.cai.game.mj.shanximj.sxhs;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_ShanXi_HuaShui extends AbstractMJHandler<MJTable_ShanXi_HuaShui> {

	@Override
	public void exe(MJTable_ShanXi_HuaShui table) {
		table._game_status = GameConstants.GS_MJ_PAO;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 有上庄
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = -1;
		}
		table.operate_player_data();

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(0);
			roomResponse.setPaoMax(4);
			roomResponse.setPaoDes("最多下炮4个");
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_ShanXi_HuaShui table, int seat_index, int pao, int qiang) {
		if (table._game_status == GameConstants.GS_MJ_PAO) {
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

			if (table._game_status == GameConstants.GS_MJ_PAO) {
				table._game_status = GameConstants.GS_MJ_PLAY;
				table.on_game_start_real();
			}
		}

		return true;

	}

	@Override
	public boolean handler_player_be_in_room(MJTable_ShanXi_HuaShui table, int seat_index) {
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

	private void player_reconnect(MJTable_ShanXi_HuaShui table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setPaoMin(0);
		roomResponse.setPaoMax(4);
		roomResponse.setPaoDes("当前最多下炮4个");
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
