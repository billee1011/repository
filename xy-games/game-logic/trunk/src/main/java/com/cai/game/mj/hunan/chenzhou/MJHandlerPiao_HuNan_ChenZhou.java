package com.cai.game.mj.hunan.chenzhou;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.GameDescUtil;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class MJHandlerPiao_HuNan_ChenZhou extends AbstractMJHandler<MJTable_HuNan_ChenZhou> {

	@Override
	public void exe(MJTable_HuNan_ChenZhou table) {
		table._game_status = GameConstants.GS_MJ_PIAO; // 设置状态

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);

		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.pao[i] = -1;
		}

		table.operate_player_data();

		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._player_result.pao[i] >= 5) {
				handler_pao_qiang(table, i, 5, 0);
				continue;
			}
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(4);
			roomResponse.setPaoMax(5);
			roomResponse.setPaoDes("最多飘5个");

			roomResponse.addDouliuzi(0);
			if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_CS_PIAO_3)) {
				roomResponse.addDouliuzi(1);
				roomResponse.addDouliuzi(2);
				roomResponse.addDouliuzi(3);
				roomResponse.setPaoMin(4);
			}
			if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_CS_PIAO_5)) {
				roomResponse.addDouliuzi(2);
				roomResponse.addDouliuzi(3);
				roomResponse.addDouliuzi(5);
				roomResponse.setPaoMin(4);
			}
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(MJTable_HuNan_ChenZhou table, int seat_index, int pao, int qiang) {
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
			table.on_game_start_real();
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_HuNan_ChenZhou table, int seat_index) {
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

	private void player_reconnect(MJTable_HuNan_ChenZhou table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		// 发送数据
		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setPaoMin(table._player_result.pao[seat_index]);
		roomResponse.setPaoMax(5);
		roomResponse.setPaoDes("最多飘5个");
		roomResponse.addDouliuzi(0);
		if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_CS_PIAO_3)) {
			roomResponse.addDouliuzi(1);
			roomResponse.addDouliuzi(2);
			roomResponse.addDouliuzi(3);
			roomResponse.setPaoMin(4);
		}
		if (GameDescUtil.has_rule(table.gameRuleIndexEx, GameConstants.GAME_RULE_HUNAN_CS_PIAO_5)) {
			roomResponse.addDouliuzi(2);
			roomResponse.addDouliuzi(3);
			roomResponse.addDouliuzi(5);
			roomResponse.setPaoMin(4);
		}
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
