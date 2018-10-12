package com.cai.game.gdy.handler.gdy_rar;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.gdy.GDYConstants;
import com.cai.game.gdy.data.tagAnalyseIndexResult_GDY;
import com.cai.game.gdy.handler.GDYHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gdy.gdyRsp.TableResponse_Gdy;

public class GDYHandlerOutCardOperate_RAR extends GDYHandlerOutCardOperate<GDYTable_RAR> {

	@Override
	public void exe(GDYTable_RAR table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			if (table._turn_out_card_count == 0) {
				table.send_error_notify(_out_card_player, 2, "请出牌");
				return;
			}
			table.cancelShedule(table.ID_TIMER_AUTO_PASS);
			table.kill_timer();
			if (table._auto_out_card_scheduled != null) {
				table._auto_out_card_scheduled.cancel(false);
				table._auto_out_card_scheduled = null;
			}
			// 保存所有玩家本轮出牌数据
			table.GRR._cur_round_pass[_out_card_player] = 1;
			table.GRR._cur_round_count[_out_card_player] = 0;
			Arrays.fill(table.GRR._cur_round_data[_out_card_player], GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_change_round_data[_out_card_player], GameConstants.INVALID_CARD);

			table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
			table._prev_palyer = _out_card_player;
			if (table._current_player == table._out_card_player) {
				Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
				table._turn_out_card_count = 0;
				table._turn_out_card_type = GameConstants.GDY_CT_PASS_HL;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i != table._out_card_player) {
						table._ying_ti[i] = false;
					}
				}
			} else {
				if (!table._logic.search_card_data(table._turn_out_real_card_data, table._turn_out_card_data,
						table._turn_out_card_count, table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player])) {
					table.schedule(table.ID_TIMER_AUTO_PASS, SheduleArgs.newArgs(), 3000);
				}
			}
			table._ying_ti[_out_card_player] = false;
			Arrays.fill(table.GRR._cur_round_data[table._current_player], GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_change_round_data[table._current_player], GameConstants.INVALID_CARD);
			table.GRR._cur_round_count[table._current_player] = 0;

			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_change_cards_data, 0,
					table._is_shou_chu, GameConstants.INVALID_SEAT);

			if (table._current_player == table._out_card_player && table.GRR._left_card_count > 0) {
				table.exe_dispatch_card(table._current_player, 1, false, 200);
			}

			return;
		}
		table._logic.SortCardList(_out_change_cards_data, _out_card_count);
		int card_type = GameConstants.GDY_CT_ERROR;
		tagAnalyseIndexResult_GDY card_index = new tagAnalyseIndexResult_GDY();
		table._logic.AnalysebCardDataToIndex(_out_cards_data, _out_card_count, card_index);
		// 全是王
		boolean is_all_magic = false;
		if (card_index.card_index[13] + card_index.card_index[14] == _out_card_count) {
			is_all_magic = true;
		}
		if (is_all_magic == true) {
			if (table.GRR._card_count[_out_card_player] != _out_card_count && _out_card_count == 1) {
				table.send_error_notify(_out_card_player, 2, "您选择的牌型不符合规则");
				return;

			}
		} else {
			if (card_index.card_index[0] != _out_card_count) {
				if (!table._logic.is_have_card(_out_cards_data, _out_change_cards_data, _out_card_count)) {
					table.send_error_notify(_out_card_player, 2, "您选择的牌型不符合规则");
					return;
				}
			}
		}

		// 判断出牌牌型
		card_type = table._logic.GetCardType_GDY(_out_cards_data, _out_change_cards_data, _out_card_count);
		if (card_type == GameConstants.GDY_CT_ERROR_RAR) {
			table.send_error_notify(_out_card_player, 2, "您选择的牌型不符合规则");
			return;
		}
		if (table._turn_out_card_count != 0) {
			table._is_shou_chu = 0;
			if (!table._logic.comparecarddata(_out_cards_data, _out_change_cards_data, _out_card_count,
					table._turn_out_real_card_data, table._turn_out_card_data, table._turn_out_card_count)) {
				table.send_error_notify(_out_card_player, 2, "您选择的牌型不符合规则");
				return;
			}
		} else {
			table._is_shou_chu = 1;
		}

		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player], _out_cards_data, _out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "您选择的牌型不符合规则");
			return;
		}

		// 没有癞子是硬提
		if (card_index.card_index[0] > 0 && card_index.card_index[0] != _out_card_count && _out_card_count != 4) {
			table._ying_ti[_out_card_player] = false;
		} else if (card_index.card_index[13] + card_index.card_index[14] > 0) {
			if (card_index.card_index[13] + card_index.card_index[14] != 2) {
				table._ying_ti[_out_card_player] = false;
			} else if (_out_card_count != 2) {
				table._ying_ti[_out_card_player] = false;
			}

		}
		// 清理定时器
		table.cancelShedule(table.ID_TIMER_AUTO_PASS);
		table.kill_timer();

		if (table._auto_out_card_scheduled != null) {
			table._auto_out_card_scheduled.cancel(false);
			table._auto_out_card_scheduled = null;
		}
		// 所有玩家本轮出牌数据
		table.GRR._cur_round_count[_out_card_player] = 0;
		Arrays.fill(table.GRR._cur_round_data[_out_card_player], GameConstants.INVALID_CARD);
		Arrays.fill(table.GRR._cur_change_round_data[_out_card_player], GameConstants.INVALID_CARD);
		table.GRR._cur_round_pass[_out_card_player] = 0;
		for (int i = 0; i < _out_card_count; i++) {
			table.GRR._cur_round_data[_out_card_player][i] = _out_cards_data[i];
			table.GRR._cur_change_round_data[_out_card_player][i] = _out_change_cards_data[i];
		}
		table.GRR._cur_round_count[_out_card_player] = _out_card_count;
		table._turn_out_card_type = card_type;
		// 炸弹限制倍数
		if (card_type == GameConstants.GDY_CT_RUAN_BOMB_RAR) {
			table._boom_times *= 2;
		} else if (card_type == GameConstants.GDY_CT_BOMB_RAR) {
			table._boom_times *= 4;
		} else if (card_type == GameConstants.GDY_CT_KING_BOMB_RAR) {
			table._boom_times *= 4;
		} else if (card_type == GameConstants.GDY_CT_MAGIC_BOMB_RAR) {
			table._boom_times *= 8;
		}

		table._out_card_time[_out_card_player]++;

		if (table._turn_out_card_count != 0) {
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < _out_card_count; i++) {
				table._turn_out_card_data[i] = _out_change_cards_data[i];
				table._turn_out_real_card_data[i] = _out_cards_data[i];
			}
			table._turn_out_card_count = _out_card_count;
		} else {
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < _out_card_count; i++) {
				table._turn_out_card_data[i] = _out_change_cards_data[i];
				table._turn_out_real_card_data[i] = _out_cards_data[i];
			}
			table._turn_out_card_count = _out_card_count;
		}
		if (table._turn_out_card_count == 0) {
			int a = 0;
			a++;
		}
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._current_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		table._prev_palyer = _out_card_player;

		Arrays.fill(table.GRR._cur_round_data[table._current_player], GameConstants.INVALID_CARD);
		Arrays.fill(table.GRR._cur_change_round_data[table._current_player], GameConstants.INVALID_CARD);
		table.GRR._cur_round_count[table._current_player] = 0;

		if (table.GRR._card_count[_out_card_player] == 0) {
			// 有玩家出完牌
			table._current_player = GameConstants.INVALID_SEAT;
			table._win_player = _out_card_player;
			if (table.has_rule(GDYConstants.GAME_RULE_GDY_WIN_ZHUANG_RAR)) {
				table._cur_banker = _out_card_player;
			} else {
				table._cur_banker = GameConstants.INVALID_SEAT;
			}

			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_change_cards_data,
					card_type, table._is_shou_chu, GameConstants.INVALID_SEAT);
			table._out_card_player = _out_card_player;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._win_player, GameConstants.Game_End_NORMAL), 2,
					TimeUnit.SECONDS);
		} else {
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_change_cards_data,
					card_type, table._is_shou_chu, GameConstants.INVALID_SEAT);
			table._out_card_player = _out_card_player;
			if (!table._logic.search_card_data(table._turn_out_real_card_data, table._turn_out_card_data,
					table._turn_out_card_count, table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player])) {
				table.schedule(table.ID_TIMER_AUTO_PASS, SheduleArgs.newArgs(), 3000);
			}
		}

	}

	@Override
	public boolean handler_player_be_in_room(GDYTable_RAR table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_RECONNECT_DATA);

		TableResponse_Gdy.Builder tableResponse_gdy = TableResponse_Gdy.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_gdy);
		tableResponse_gdy.setRoomInfo(table.getRoomInfo());

		tableResponse_gdy.setBankerPlayer(table.GRR._banker_player);
		tableResponse_gdy.setCurrentPlayer(table._current_player);
		tableResponse_gdy.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_gdy.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_gdy.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_change_round_data[i][j]);
				}
			}
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					hand_cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			tableResponse_gdy.addHandCardsData(hand_cards);
			if (table.has_rule(GameConstants.GAME_RULE_GDY_CARD_NUMBER) || i == seat_index) {
				tableResponse_gdy.addHandCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse_gdy.addHandCardCount(-1);
			}

			tableResponse_gdy.addOutCardsData(i, out_cards);
			tableResponse_gdy.addOutChangeCardsData(out_change_cards);
		}
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_gdy.setCurPlayerYaPai(1);
			} else {
				if (table._logic.search_card_data(table._turn_out_real_card_data, table._turn_out_card_data,
						table._turn_out_card_count, table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index])) {
					tableResponse_gdy.setCurPlayerYaPai(1);
				} else {
					tableResponse_gdy.setCurPlayerYaPai(0);
				}
			}
		} else {
			tableResponse_gdy.setCurPlayerYaPai(0);
		}
		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_gdy.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_gdy.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_gdy.setPrCardsCount(table._turn_out_card_count);
		tableResponse_gdy.setPrOutCardType(table._turn_out_card_type);
		tableResponse_gdy.setPrOutCardPlayer(table._out_card_player);
		tableResponse_gdy.setIsFirstOut(table._is_shou_chu);
		tableResponse_gdy.setLeftCardCount(table.GRR._left_card_count);
		tableResponse_gdy.setTimesNum(table._boom_times);
		tableResponse_gdy.setGameCell((int) table.game_cell);
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_gdy.setIsCurrentFirstOut(1);
				tableResponse_gdy.setDisplayTime(15);
			} else {
				tableResponse_gdy.setIsCurrentFirstOut(0);
				tableResponse_gdy.setDisplayTime(3);
			}
		} else {
			tableResponse_gdy.setIsCurrentFirstOut(0);
			tableResponse_gdy.setDisplayTime(15);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_gdy));
		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
