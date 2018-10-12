package com.cai.game.nn.handler.szox;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.Player;
import com.cai.game.nn.NNTable;
import com.cai.game.nn.handler.NNHandlerAddJetton;

import protobuf.clazz.Protocol.GameStart;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.Timer_OX;

public class NNHandlerAddJetton_szOX extends NNHandlerAddJetton {

	@Override
	public void exe(NNTable table) {

	}

	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	public boolean handler_add_jetton(NNTable table, int seat_index, int sub_jetton) {
		if (_game_status != GameConstants.GS_OX_ADD_JETTON) {
			table.log_error("游戏状态不对 " + _game_status + "用户开牌 :" + GameConstants.GS_OX_ADD_JETTON);
			return false;
		}
		if (table._add_Jetton[seat_index] != 0) {
			table.log_error("你已经下注操作了 ");
			return false;
		}
		if (sub_jetton < 0 || sub_jetton > 4) {
			table.log_error("您下注已经越界了");
			return false;
		}
		if (table._jetton_info_cur[seat_index][sub_jetton] == 0) {
			table.log_error("您下注为0 了");
			return false;
		}
		if (seat_index == table._cur_banker) {
			table.log_error("庄家不用下注");
			return false;
		}
		if (sub_jetton == 2) {
			table._can_tuizhu_player[seat_index] = -1;
		} else {
			table._can_tuizhu_player[seat_index] = 0;
		}
		table._add_Jetton[seat_index] = table._jetton_info_cur[seat_index][sub_jetton];
		table.add_jetton_ox(seat_index);
		boolean flag = true;
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {
			if (table._player_status[i] == true) {
				if (i == table._cur_banker)
					continue;
				if (table._add_Jetton[i] == 0)
					flag = false;
			}
		}
		// 游戏结束
		if (flag == true) {
			table.send_card_date_ox();

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(NNTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			if ((i == seat_index) && (table._add_Jetton[i] == 0) && (table._player_status[i] == true)) {
				GameStart.Builder game_start = GameStart.newBuilder();
				game_start.setCurBanker(table._cur_banker);
				if (table._cur_banker != i) {

					for (int k = 0; k < GameConstants.GAME_PLAYER_OX; k++) {
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
						if ((k != table._cur_banker) && (table._player_status[k] == true) && (k == i)) {
							if (table.has_rule(GameConstants.GAME_RULE_ONE_TWO)) {
								for (int j = 0; j < 2; j++) {
									cards.addItem(table._jetton_info_sever_ox[0][j]);
									table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[0][j];
								}
							}
							if (table.has_rule(GameConstants.GAME_RULE_TWO_FOUR)) {
								for (int j = 0; j < 2; j++) {
									cards.addItem(table._jetton_info_sever_ox[1][j]);
									table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[1][j];
								}
							}
							if (table.has_rule(GameConstants.GAME_RULE_FOUR_EIGHT)) {
								for (int j = 0; j < 2; j++) {
									cards.addItem(table._jetton_info_sever_ox[2][j]);
									table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[2][j];
								}
							}

						}
						game_start.addJettonCell(k, cards);
					}
				}
				roomResponse_ox.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
		}
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(display_time);
			roomResponse_ox.setDisplayTime(timer);
		}
		roomResponse_ox.setTableResponseOx(tableResponse);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(NNTable table, Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		// tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for (int i = 0; i < GameConstants.GAME_PLAYER_OX; i++) {

			tableResponse.addAddJetter(table._add_Jetton[i]);
		}
		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			Timer_OX.Builder timer = Timer_OX.newBuilder();
			timer.setDisplayTime(display_time);
			roomResponse_ox.setDisplayTime(timer);
		}
		roomResponse_ox.setTableResponseOx(tableResponse);
		roomResponse.setRoomResponseOx(roomResponse_ox);
//		table.send_response_to_player(seat_index, roomResponse);
		table.observers().send(player, roomResponse);
		return true;
	}

}
