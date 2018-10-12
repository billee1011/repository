package com.cai.game.abz.handler.abz_four;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.abz.handler.PUKEHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.abz.AbzRsp.TableResponse_Abz;

public class PUKEHandlerOutCardOperate_ABZ_FOUR extends PUKEHandlerOutCardOperate<PUKETable_ABZ_FOUR> {

	@Override
	public void exe(PUKETable_ABZ_FOUR table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			if (table._turn_out_card_count == 0) {
				return;
			}

			table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
			if (table._current_player == table._out_card_player) {
				table._turn_out_card_count = 0;
				Arrays.fill(table._turn_out_card_data, 0);
			}
			Arrays.fill(table._cur_out_card_data[_out_card_player], 0);
			table._cur_out_card_type[_out_card_player] = GameConstants.ABZ_CT_PASS;
			table._cur_out_card_count[_out_card_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], 0);
			table._cur_out_card_type[table._current_player] = GameConstants.ABZ_CT_ERROR;
			table._cur_out_card_count[table._current_player] = 0;
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_type,
					GameConstants.INVALID_SEAT);
			return;
		}
		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == GameConstants.ABZ_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型");
			return;
		}
		// 下一个玩家
		table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();

		// 清理下一玩家本轮出牌数据
		Arrays.fill(table._cur_out_card_data[table._current_player], 0);
		table._cur_out_card_count[table._current_player] = 0;

		// 保存最后一个出牌玩家牌型数据
		for (int i = 0; i < table._turn_out_card_count; i++) {
			table._turn_out_card_data[i] = 0;
		}
		table._turn_out_card_count = _out_card_count;
		table._turn_out_card_type = cbCardType;
		for (int i = 0; i < table._turn_out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
		}
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._out_card_player = _out_card_player;
		// 保存当前玩家本轮出牌数据
		Arrays.fill(table._cur_out_card_data[_out_card_player], 0);
		for (int i = 0; i < _out_card_count; i++) {
			table._cur_out_card_data[_out_card_player][i] = _out_cards_data[i];
		}
		table._cur_out_card_type[_out_card_player] = cbCardType;
		table._cur_out_card_count[_out_card_player] = _out_card_count;

		Arrays.fill(table._cur_out_card_data[table._current_player], 0);
		table._cur_out_card_type[table._current_player] = GameConstants.ABZ_CT_ERROR;
		table._cur_out_card_count[table._current_player] = 0;
		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 2;
			table._cur_banker = _out_card_player;
			table._current_player = GameConstants.INVALID_SEAT;
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Arrays.fill(table._cur_out_card_data[i], 0);
				table._cur_out_card_count[i] = 0;
			}
			for (int i = 0; i < table._turn_out_card_count; i++) {
				table._turn_out_card_data[i] = 0;
			}
			table._turn_out_card_count = 0;
		} else {
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType,
					GameConstants.INVALID_SEAT);
		}

	}

	public int adjust_out_card_right(PUKETable_ABZ_FOUR table) {
		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);
		if (cbCardType == GameConstants.ABZ_CT_ERROR) {
			return cbCardType;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.comparecarddata(table._turn_out_card_data, table._turn_out_card_count, _out_cards_data,
					_out_card_count)) {
				return GameConstants.ABZ_CT_ERROR;
			}
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			return GameConstants.ABZ_CT_ERROR;
		}
		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(PUKETable_ABZ_FOUR table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_RECONNECT);

		TableResponse_Abz.Builder tableResponse = TableResponse_Abz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setBaoNum(table._bao_num);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addCallBankerAction(table._select_bao[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_cards_data = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}

			} else if (table.GRR._banker_player != GameConstants.INVALID_SEAT
					&& table._select_bao[table.GRR._banker_player] == 1) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}

			if (i == seat_index) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				if (table.has_rule(GameConstants.GAME_RULE_ABZ_YU_PAI_XIANSHI_NO)
						&& table.GRR._banker_player != GameConstants.INVALID_SEAT
						&& table._select_bao[table.GRR._banker_player] != 1) {
					if (table.GRR._card_count[i] <= 2) {
						tableResponse.addCardCount(table.GRR._card_count[i]);
					} else {
						tableResponse.addCardCount(-1);
					}

				} else {
					tableResponse.addCardCount(table.GRR._card_count[i]);
				}
			}

			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				out_cards_data.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addChangCardData(table._chang_card[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
			tableResponse.addOutCardsData(out_cards_data);
			tableResponse.addOutCardsType(table._cur_out_card_type[i]);
			tableResponse.addCardsData(cards);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		tableResponse.setCurrentPlayer(table._current_player);
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse.setIsFirstOut(1);
				tableResponse.setIsCurrentYaPai(1);
			} else {
				tableResponse.setIsFirstOut(0);
				int can_out_card_count = table._logic.Player_Can_out_card(table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index], table._turn_out_card_data, table._turn_out_card_count, null);
				if (can_out_card_count > 0) {
					tableResponse.setIsCurrentYaPai(1);
				} else {
					tableResponse.setIsCurrentYaPai(0);
				}
			}
		} else {
			tableResponse.setIsFirstOut(0);
			tableResponse.setIsCurrentYaPai(0);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}

}
