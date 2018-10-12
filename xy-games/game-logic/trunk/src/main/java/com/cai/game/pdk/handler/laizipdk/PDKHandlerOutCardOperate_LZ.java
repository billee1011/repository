package com.cai.game.pdk.handler.laizipdk;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.pdk.PDKTable;
import com.cai.game.pdk.handler.PDKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pdk.PdkRsp.RoomInfoPdk;
import protobuf.clazz.pdk_all.PdkRsp.TableResponse_PDK_Error;

public class PDKHandlerOutCardOperate_LZ extends PDKHandlerOutCardOperate {
	@Override
	public void exe(PDKTable table) {
		// TODO Auto-generated method stub

		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		// 玩家不出
		if (_out_type == 0) {
			no_out_card(table);
			return;
		}

		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			return;
		}
		if (table.has_rule(GameConstants.GAME_RULE_FANG_ZOU_BAOPEI)) {
			if (fang_zou_bao_pei(table)) {
				table._bao_pei_palyer = table._turn_out__player;
			}
		}
		// 保存玩家炸弹个数
		if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
			table._boom_num[_out_card_player]++;
			table._all_boom_num[_out_card_player]++;
		}
		// 保存玩家出牌次数
		table._out_card_times[_out_card_player]++;
		// 保存上一操作玩家
		table._prev_palyer = _out_card_player;
		table._out_card_player = _out_card_player;
		// 保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player] = 0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		for (int i = 0; i < this._out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = this._out_cards_data[i];
			// 保存该次出牌数据
			table._turn_out_card_data[i] = this._out_cards_data[i];
		}
		table._turn_out__player = _out_card_player;
		table._turn_out_card_count = this._out_card_count;
		table._turn_out_card_type = cbCardType;
		table.GRR._card_count[_out_card_player] -= this._out_card_count;

		// 刷新手牌
		int cards[] = new int[table.get_hand_card_count_max()];

		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		table.GRR._cur_round_count[table._current_player] = 0;
		table.GRR._cur_round_pass[table._current_player] = 0;
		for (int j = 0; j < this._out_card_count; j++) {
			table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
		}

		if (0 == table.GRR._card_count[_out_card_player]) {
			table._current_player = GameConstants.INVALID_SEAT;
		}

		// 排列变换扑克
		table._logic.sort_card_date_list_by_type(table._turn_out_change_card_data, table._turn_out_card_count,
				cbCardType);
		int tempdate[] = new int[table._turn_out_card_count];
		int tempdatechange[] = new int[table._turn_out_card_count];
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._logic.GetCardLogicValue(table._turn_out_card_data[i]) != table._logic
					.GetCardLogicValue(table._logic._laizi)) {
				tempdate[i] = table._turn_out_card_data[i];
			} else {
				tempdate[i] = GameConstants.INVALID_CARD;
			}
			tempdatechange[i] = table._turn_out_change_card_data[i];
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			for (int j = 0; j < table._turn_out_card_count; j++) {
				if (tempdate[i] == tempdatechange[j] && tempdatechange[j] != GameConstants.INVALID_CARD) {
					int tempcard = tempdate[j];
					tempdate[j] = tempdate[i];
					tempdate[i] = tempcard;

					tempdatechange[j] = -3;
					i = 0;
					break;
				}
			}
		}
		int laiziindex = 0;
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (tempdate[i] == GameConstants.INVALID_CARD) {
				for (; laiziindex < table._turn_out_card_count;) {
					if (table._logic.GetCardLogicValue(table._turn_out_card_data[laiziindex]) == table._logic
							.GetCardLogicValue(table._logic._laizi)) {
						tempdate[i] = table._turn_out_card_data[laiziindex];
						laiziindex++;
						break;
					}
					laiziindex++;
				}

			}
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			table._turn_out_card_data[i] = tempdate[i];
		}

		// 刷新玩家手牌
		table.operate_player_cards();
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT);

		table.exe_add_discard(_out_card_player, _out_card_count, _out_cards_data, false, 1);
		if (0 == table.GRR._card_count[_out_card_player]) {
			int delay = 1;
			if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
				table._out_bomb_score[_out_card_player] += (table.getTablePlayerNumber() - 1) * 10;
				table._player_result.game_score[_out_card_player] += (table.getTablePlayerNumber() - 1) * 10;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == _out_card_player) {
						continue;
					}
					table._out_bomb_score[i] -= 10;
					table._player_result.game_score[i] -= 10;
				}
				RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
				roomResponse2.setGameStatus(table._game_status);
				roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
				table.load_player_info_data(roomResponse2);
				table.send_response_to_room(roomResponse2);

			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			return;
		}
	}

	@Override
	public boolean handler_player_be_in_room(PDKTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_PDK_RECONNECT_DATA);

		TableResponse_PDK_Error.Builder tableResponse_pdk = TableResponse_PDK_Error.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_pdk);
		RoomInfoPdk.Builder room_info = table.getRoomInfoPdk();
		tableResponse_pdk.setRoomInfo(room_info);

		tableResponse_pdk.setBankerPlayer(table.GRR._banker_player);
		tableResponse_pdk.setCurrentPlayer(table._current_player);
		tableResponse_pdk.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_pdk.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_pdk.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_change_round_data[i][j]);
				}
			}
			if (table.has_rule(GameConstants.GAME_RULE_DISPLAY_CARD)) {
				tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
			} else {
				if (i == seat_index) {
					tableResponse_pdk.addCardCount(table.GRR._card_count[i]);
				} else {
					tableResponse_pdk.addCardCount(-1);
				}
			}
			tableResponse_pdk.addCardType(table.GRR._cur_card_type[i]);
			tableResponse_pdk.addOutCardsData(i, out_cards);
			tableResponse_pdk.addChangeCardsData(out_change_cards);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[i]; j++) {
				cards_card.addItem(GameConstants.INVALID_CARD);
			}
			tableResponse_pdk.addCardsData(i, cards_card);
		}

		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		tableResponse_pdk.setCardsData(seat_index, cards_card);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_pdk.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_pdk.addPrChangeCardsData(table._turn_out_change_card_data[i]);
			}
		}
		tableResponse_pdk.setPrCardsCount(table._turn_out_card_count);
		tableResponse_pdk.setPrOutCardType(table._turn_out_card_type);
		tableResponse_pdk.setPrOutCardPlayer(table._turn_out__player);
		if (table._turn_out_card_count == 0) {
			tableResponse_pdk.setIsFirstOut(1);
		} else {
			tableResponse_pdk.setIsFirstOut(0);
		}

		tableResponse_pdk.setDisplayTime(10);
		tableResponse_pdk.setMagicCard(table._logic._laizi);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_pdk));
		table.send_response_to_player(seat_index, roomResponse);

		// table.operate_player_cards();

		return true;
	}

	// 玩家不出
	public void no_out_card(PDKTable table) {
		// if (table.has_rule(GameConstants.GAME_RULE_BI_XU_GUAN) &&
		// table._logic.SearchOutCardLaiZi(
		// table.GRR._cards_data[_out_card_player],
		// table.GRR._card_count[_out_card_player],
		// table._turn_out_card_data, table._turn_out_card_count,
		// table._turn_out_change_card_data)) {
		// return;
		// }
		table.GRR._cur_round_count[_out_card_player] = 0;
		table.GRR._cur_round_pass[_out_card_player] = 1;

		for (int i = 0; i < table.get_hand_card_count_max(); i++) {
			table.GRR._cur_round_data[_out_card_player][i] = GameConstants.INVALID_CARD;
		}
		table._prev_palyer = _out_card_player;

		if (table._current_player == table._out_card_player) {
			return;
		}
		// 判断下一个玩家
		int next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		table._current_player = next_player;
		if (table._current_player == table._out_card_player) {
			// 炸弹分
			int cbCardType = table._logic.GetCardType(table._turn_out_change_card_data, table._turn_out_card_count,
					table._turn_out_card_data);
			if (cbCardType == GameConstants.PDK_CT_ERROR) {
				return;
			}
			if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
				if (cbCardType == GameConstants.PDK_CT_BOMB_CARD) {
					table._out_bomb_score[table._out_card_player] += (table.getTablePlayerNumber() - 1) * 10;
					table._player_result.game_score[table._out_card_player] += (table.getTablePlayerNumber() - 1) * 10;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table._out_card_player) {
							continue;
						}
						table._out_bomb_score[i] -= 10;
						table._player_result.game_score[i] -= 10;
					}
					RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
					roomResponse2.setGameStatus(table._game_status);
					roomResponse2.setType(MsgConstants.RESPONSE_REFRESH_PLAYERS);// 刷新玩家
					table.load_player_info_data(roomResponse2);
					table.send_response_to_room(roomResponse2);

				}
			}

			// 出完一圈牌
			table._turn_out_card_count = 0;
			table._turn_out_card_type = GameConstants.PDK_CT_ERROR;
			for (int i = 0; i < this._out_card_count; i++) {
				table._turn_out_card_data[i] = GameConstants.INVALID_CARD;
			}
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_out_change_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_count, 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}
		table.GRR._cur_round_count[table._current_player] = 0;
		table.GRR._cur_round_pass[table._current_player] = 0;
		for (int j = 0; j < this._out_card_count; j++) {
			table.GRR._cur_round_data[table._current_player][j] = GameConstants.INVALID_CARD;
		}
		// 显示出牌
		table.operate_out_card(_out_card_player, 0, _out_cards_data, GameConstants.PDK_CT_PASS,
				GameConstants.INVALID_SEAT);

		// 通知客户端出牌用户
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table._current_player) {
				continue;
			}
			table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
		}
		table._playerStatus[table._current_player].set_status(GameConstants.Player_Status_OUT_CARD);
		table.operate_player_status();
	}

	// 判断玩家出牌合法
	public int adjust_out_card_right(PDKTable table) {
		int change_data[] = new int[_out_card_count];
		int cbCardType = table._logic.get_change_data(_out_cards_data, change_data, _out_card_count, _out_type);
		if (cbCardType == GameConstants.PDK_CT_ERROR) {
			return cbCardType;
		}
		// 三带一只有在剩4张牌时才能出
		if (cbCardType == GameConstants.PDK_CT_THREE_TAKE_ONE && table.GRR._card_count[this._out_card_player] != 4
				|| (cbCardType == GameConstants.PDK_CT_THREE && table.GRR._card_count[this._out_card_player] != 3)) {
			return GameConstants.PDK_CT_ERROR;
		}

		// 四带
		if (table.has_rule(GameConstants.GAME_RULE_FOUR_DAI_SAN)) {
			if ((cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE
					&& table.GRR._card_count[this._out_card_player] != 5)
					|| (cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
							&& table.GRR._card_count[this._out_card_player] != 6)) {
				return GameConstants.PDK_CT_ERROR;
			}
		} else if (cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE
				|| cbCardType == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO) {
			return GameConstants.PDK_CT_ERROR;
		}

		if (table._turn_out_card_count > 0) {
			if (!table._logic.CompareCardLaizi(table._turn_out_change_card_data, change_data, _out_cards_data,
					table._turn_out_card_data, table._turn_out_card_count, _out_card_count, _out_type)) {
				return GameConstants.PDK_CT_ERROR;
			}
		}
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)) {
			return GameConstants.PDK_CT_ERROR;
		}
		if (fang_zou_bao_pei(table)) {
			return GameConstants.PDK_CT_ERROR;
		}
		for (int i = 0; i < this._out_card_count; i++) {
			table._turn_out_change_card_data[i] = change_data[i];
		}

		return cbCardType;
	}

	// 放走包赔
	public boolean fang_zou_bao_pei(PDKTable table) {
		if (table.GRR._card_count[_out_card_player] == 1 && _out_card_count == 1 && table._turn_out_card_count == 1
				&& ((_out_card_player + table.getTablePlayerNumber() + 2)
						% table.getTablePlayerNumber()) == table._turn_out__player) {
			// 还原上家牌型
			int cards_data_temp[] = new int[table.GRR._card_count[table._turn_out__player] + 1];
			for (int i = 0; i < table.GRR._card_count[table._turn_out__player]; i++) {
				cards_data_temp[i] = table.GRR._cards_data[table._turn_out__player][i];
			}
			cards_data_temp[table.GRR._card_count[table._turn_out__player]] = table._turn_out_card_data[0];

			table._logic.sort_card_date_list(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1);
			return table._logic.fang_zou_bao_pei(cards_data_temp, table.GRR._card_count[table._turn_out__player] + 1,
					table._turn_out_card_data);
		}
		return false;
	}
}
