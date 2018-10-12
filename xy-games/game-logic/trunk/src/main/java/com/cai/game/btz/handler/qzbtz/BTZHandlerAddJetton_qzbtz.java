package com.cai.game.btz.handler.qzbtz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.BTZConstants;
import com.cai.common.domain.Player;
import com.cai.common.util.PBUtil;
import com.cai.game.btz.BTZTable;
import com.cai.game.btz.handler.BTZHandlerAddJetton;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.btz.BTZRsp.GameStart_BTZ;
import protobuf.clazz.btz.BTZRsp.TableResponse_BTZ;

public class BTZHandlerAddJetton_qzbtz extends BTZHandlerAddJetton<BTZTable> {

	@Override
	public void exe(BTZTable table) {

	}

	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	public boolean handler_add_jetton(BTZTable table, int seat_index, int sub_jetton) {
		if (_game_status != GameConstants.GS_OX_ADD_JETTON) {
			table.log_error("游戏状态不对 " + _game_status + "用户下注 :" + GameConstants.GS_OX_ADD_JETTON);
			return false;
		}
		if (table._add_Jetton[seat_index] != 0) {
			return false;
		}
		if (sub_jetton < 0 || table._jetton_info_cur[seat_index].length <= sub_jetton) {
			return false;
		}
		if (table._jetton_info_cur[seat_index][sub_jetton] <= 0) {
			return false;
		}
		if (seat_index == table._cur_banker) {
			table.log_error("庄家不用下注");
			return false;
		}

		// else {
		// table._can_tuizhu_player[seat_index] = 0;
		// }
		table._add_Jetton[seat_index] = table._jetton_info_cur[seat_index][sub_jetton];
		if (sub_jetton < 3) {
			if (table.has_rule(BTZConstants.BTZ_RULE_BASE_SCORE_TWO)) {
				table._add_Jetton[seat_index] *= 2;
			} else if (table.has_rule(BTZConstants.BTZ_RULE_BASE_SCORE_THREE)) {
				table._add_Jetton[seat_index] *= 3;
			}
		}

		table.add_jetton_ox(seat_index);
		boolean flag = true;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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
	public boolean handler_player_be_in_room(BTZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			if ((i == seat_index) && (table._add_Jetton[i] == 0) && (table._player_status[i] == true)) {
				GameStart_BTZ.Builder game_start = GameStart_BTZ.newBuilder();
				game_start.setCurBanker(table._cur_banker);

				if (seat_index != table._cur_banker) {
					for (int k = 0; k < table.getTablePlayerNumber(); k++) {
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
						if ((k != table._cur_banker) && (table._player_status[k] == true)) {
							for (int j = 0; j < 3; j++) {
								cards.addItem(table._jetton_info_sever_ox[j]);
								table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[j];
							}

							if (table._can_tuizhu_player[k] > 0) {
								table._jetton_info_cur[k][3] = table._can_tuizhu_player[k];
								cards.addItem(table._jetton_info_cur[k][3]);
							}

						}
						game_start.addJettonCell(k, cards);
					}
				}
				tableResponse.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
			tableResponse.addTrustee(table.isTrutess(i));
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		if (table.isTrutess(seat_index)) {
			table.reSendTrusteeToPlayer(seat_index);
		}
		return true;
	}

	@Override
	public boolean handler_observer_be_in_room(BTZTable table, Player player) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(BTZConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_BTZ.Builder tableResponse = TableResponse_BTZ.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			if ((table._add_Jetton[i] == 0) && (table._player_status[i] == true)) {
				GameStart_BTZ.Builder game_start = GameStart_BTZ.newBuilder();
				game_start.setCurBanker(table._cur_banker);

				for (int k = 0; k < table.getTablePlayerNumber(); k++) {
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if ((k != table._cur_banker) && (table._player_status[k] == true)) {
						for (int j = 0; j < 3; j++) {
							cards.addItem(table._jetton_info_sever_ox[j]);
							table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[j];
						}

						if (table._can_tuizhu_player[k] > 0) {
							table._jetton_info_cur[k][3] = table._can_tuizhu_player[k];
							cards.addItem(table._jetton_info_cur[k][3]);
						}

					}
					game_start.addJettonCell(k, cards);
				}
				tableResponse.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
			tableResponse.addCallBankerInfo(table._call_banker[i]);
			tableResponse.addTrustee(table.isTrutess(i));
		}

		int display_time = table._cur_operate_time - ((int) (System.currentTimeMillis() / 1000L) - table._operate_start_time);
		if (display_time > 0) {
			tableResponse.setDisplayTime(display_time);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.observers().send(player, roomResponse);
		return true;
	}

}