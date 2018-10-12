package com.cai.game.mj.henan.newhuazhou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_HuaZhou;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerPao_HuaZhou extends AbstractMJHandler<Table_HuaZhou> {
	@Override
	public void exe(Table_HuaZhou table) {
		table._game_status = GameConstants.GS_MJ_PAO;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			//给客户端区分初始值(-1)还是选择不跑(0)
			table._player_result.pao[i] = -1;
		}
		table.operate_player_data();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.setTarget(i);
			roomResponse.setPao(table._player_result.pao[i]);
			roomResponse.setPaoMin(0);
			if(table.has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_THREE)){
				roomResponse.setPaoMax(3);
				roomResponse.setPaoDes("当前可以下跑,最多下跑3个");
			}else if(table.has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_FIVE)){
				roomResponse.setPaoMax(5);
				roomResponse.setPaoDes("当前可以下跑,最多下跑5个");
			}
			
			table.send_response_to_player(i, roomResponse);
		}
	}

	public boolean handler_pao_qiang(Table_HuaZhou table, int seat_index, int pao, int qiang) {
		if (table._playerStatus[seat_index]._is_pao_qiang)
			return false;

		table._playerStatus[seat_index]._is_pao_qiang = true;

		table._player_result.pao[seat_index] = pao;

		//只刷新本人的数据
		table.operate_player_data_to_player(seat_index);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._is_pao_qiang == false) {
				return true;
			}
		}

		if (table._game_status == GameConstants.GS_MJ_PAO) {
			table._game_status = GameConstants.GS_MJ_PLAY;
			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					table.on_game_start_real();
				}
			}, 500, TimeUnit.MILLISECONDS);
			
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_HuaZhou table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(Table_HuaZhou table, int seat_index) {
		if (table._playerStatus[seat_index]._is_pao_qiang == true) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PAO_QIANG_ACTION);
		table.load_room_info_data(roomResponse);

		roomResponse.setTarget(seat_index);
		roomResponse.setPao(table._player_result.pao[seat_index]);
		roomResponse.setPaoMin(0);
		if(table.has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_THREE)){
			roomResponse.setPaoMax(3);
			roomResponse.setPaoDes("当前可以下跑,最多下跑3个");
		}else if(table.has_rule(Constants_HuaZhou.GAME_RULE_XIA_PAO_FIVE)){
			roomResponse.setPaoMax(5);
			roomResponse.setPaoDes("当前可以下跑,最多下跑5个");
		}
		
		table.send_response_to_player(seat_index, roomResponse);

		table.load_common_status(roomResponse);
		table.send_response_to_player(seat_index, roomResponse);
	}
}
