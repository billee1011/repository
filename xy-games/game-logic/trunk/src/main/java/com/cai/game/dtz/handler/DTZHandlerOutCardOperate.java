package com.cai.game.dtz.handler;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_DTZ;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.dtz.DTZHandler;
import com.cai.game.dtz.Table_DTZ;
import com.cai.game.dtz.runnable.AutoOutCardRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.dtz.DTZPro.RoomResponseDTZ;

public class DTZHandlerOutCardOperate extends DTZHandler<Table_DTZ> {

	public int _out_card_player;
	public int[] _out_cards_data;
	public int _out_card_count;
	public int _out_type;
	public String _desc;

	public void reset_status(int seat_index, int cards[], int card_count, int is_out, String desc) {
		_out_card_player = seat_index;
		_out_cards_data = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			_out_cards_data[i] = cards[i];
		}
		_out_card_count = card_count;
		_out_type = is_out;
		_desc = desc;
	}

	@Override
	public void exe(Table_DTZ table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		if (table.GRR == null) {
			return;
		}

		// 玩家不出
		if (_out_type == 0) {
			pass(table);
			return;
		}

		table._logic.sort_card_date_list(_out_cards_data, _out_card_count);
		int cbCardType = adjust_out_card_right(table);
		if (cbCardType == GameConstants_DTZ.CT_ERROR) {
			table.log_info("_out_cards_data:" + Arrays.toString(_out_cards_data) + "_out_card_count:" + _out_card_count);
			table.log_info("desc:" + this._desc);
			return;
		}
		if ((table._is_shou_chu == 1) && (cbCardType == GameConstants_DTZ.CT_PLANE_LOST || cbCardType == GameConstants_DTZ.CT_PLANE)) {
			table.out_plane_count = table._logic.getPineCount(_out_cards_data, _out_card_count);
		}
		table.GRR._card_count[_out_card_player] -= _out_card_count;

		table._logic.getCardScoreDetail(_out_cards_data, _out_card_count, table._curr_round_score);
		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count, table.score_detail_count, table.score_detail);
		table._prev_player = _out_card_player;
		table._turn_out_player = _out_card_player;
		table._turn_out_card_count = _out_card_count;
		table._turn_out_card_type = cbCardType;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		table.GRR._cur_round_pass[_out_card_player] = 0;
		for (int o = 0; o < _out_card_count; o++) {
			table._turn_out_card_data[o] = _out_cards_data[o];
			table._cur_out_card_data[_out_card_player][o] = _out_cards_data[o];
		}

		// 找出下一个操作牌的人
		int next_player = (_out_card_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._turn_out_player) {
				// 显示出牌
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				table.GRR._cur_round_pass[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
		table._cur_out_card_count[table._current_player] = 0;
		Arrays.fill(table._cur_out_card_data[table._current_player], 0);

		// 排名
		if (table.GRR._card_count[_out_card_player] == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
					table._chuwan_shunxu[i] = _out_card_player;
					break;
				}
			}
		}

		// 差一个人走都走完那就是结束了啊，厉害了！！！！！！！！！！！！！！！
		int end_player_count = 0;
		for (int p = 0; p < table.getTablePlayerNumber() && table.GRR._card_count[next_player] != 0; p++) {
			if (table.GRR._card_count[p] == 0) {
				end_player_count++;
			}
		}
		// if (end_player_count + 1 == table.getTablePlayerNumber() ||
		// end_player_count == table.getTablePlayerNumber()) {
		if (table.GRR._card_count[next_player] == 0 || next_player == table._turn_out_player) {
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table.GRR._card_count[j] != 0) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
							table._chuwan_shunxu[i] = j;
							break;
						}
					}
				}
			}

			// 清空桌面牌分
			int magic_score = table._logic.getMagicCardScore(table._turn_out_card_type, table._turn_out_card_data[0], table.score_detail_count,
					table.score_detail);
			table._get_score[table._turn_out_player] += table._turn_have_score;
			table._magic_score[table._turn_out_player] += magic_score;
			// table._history_score[table._turn_out_player] +=
			// table._turn_have_score;
			table._player_result.game_score[table._turn_out_player] += table._turn_have_score;
			table.player_curr_round_score[table._turn_out_player] += table._turn_have_score;

			if (magic_score > 0 || table._turn_have_score > 0) {
				table.operate_effect_action(table._turn_out_player, GameConstants_DTZ.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants_DTZ.EFFECT_ACTION_TYPE_MONEY }, 1, GameConstants.INVALID_SEAT);
			}
			for (int i = 0; i < table.score_detail_count.length; i++) {
				table.player_score_detail_count[table._turn_out_player][i] += table.score_detail_count[i];
				table.score_detail_count[i] = 0;
				table.player_score_detail[table._turn_out_player][i] += table.score_detail[i];
				table.score_detail[i] = 0;
			}

			table._current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), 1, TimeUnit.SECONDS);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, cbCardType, GameConstants.INVALID_SEAT);

		auto_out_card(table);

		table._is_shou_chu = 0;
		table.refresh_user_get_score(GameConstants_DTZ.INVALID_SEAT, false);
		table.refresh_curr_round_score(GameConstants_DTZ.INVALID_SEAT);
	}

	private void pass(Table_DTZ table) {
		if (table._current_player == _out_card_player) {
			int[] user_can_out_data = new int[table.get_hand_card_count_max()];
			int can_out_card_count = table._logic.Player_Can_out_card(table.GRR._cards_data[_out_card_player],
					table.GRR._card_count[_out_card_player], table._turn_out_card_data, table._turn_out_card_count, user_can_out_data);
			if (can_out_card_count != 0 && table.has_rule(GameConstants_DTZ.GAME_RULE_HAVE_CARD_THAN_OUT)) {
				table.send_error_notify(_out_card_player, 2, "玩家有牌必出，不允许过牌");
				return;
			}
		}
		if (table._turn_out_card_count == 0) {
			return;
		}

		table.GRR._cur_round_pass[_out_card_player] = 1;

		table._prev_player = _out_card_player;
		int next_player = (_out_card_player + 1 + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._turn_out_player) {
				// 显示出牌
				// table._current_player = next_player;
				// table.operate_out_card(next_player, 0, null,
				// table._turn_out_card_type, GameConstants.INVALID_SEAT);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}

		// 一轮不出, 计算分分数
		if (next_player == table._turn_out_player) {
			// 清空桌面牌分
			int magic_score = table._logic.getMagicCardScore(table._turn_out_card_type, table._turn_out_card_data[0], table.score_detail_count,
					table.score_detail);
			table._get_score[table._turn_out_player] += table._turn_have_score;
			table._magic_score[table._turn_out_player] += magic_score;
			// table._history_score[table._turn_out_player] +=
			// table._turn_have_score;
			table._player_result.game_score[table._turn_out_player] += table._turn_have_score;
			table.player_curr_round_score[table._turn_out_player] += table._turn_have_score;

			if (magic_score > 0 || table._turn_have_score > 0) {
				table.operate_effect_action(table._turn_out_player, GameConstants_DTZ.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants_DTZ.EFFECT_ACTION_TYPE_MONEY }, 1, GameConstants.INVALID_SEAT);
			}

			for (int i = 0; i < table.score_detail_count.length; i++) {
				table.player_score_detail_count[table._turn_out_player][i] += table.score_detail_count[i];
				table.score_detail_count[i] = 0;
				table.player_score_detail[table._turn_out_player][i] += table.score_detail[i];
				table.score_detail[i] = 0;
			}
			table._turn_have_score = 0;
			table._turn_out_card_count = 0;
			table._turn_out_card_type = 0;
			table._cur_out_card_count[table._turn_out_player] = 0;
			Arrays.fill(table._turn_out_card_data, 0);
			Arrays.fill(table._curr_round_score, 0);
			Arrays.fill(table._cur_out_card_data[table._turn_out_player], 0);
			Arrays.fill(table.GRR._cur_round_pass, 0);
		}

		// 一轮不出
		if (next_player == table._turn_out_player && table.GRR._card_count[table._turn_out_player] == 0) {
			int end_player_count = 0;
			for (int p = 0; p < table.getTablePlayerNumber(); p++) {
				if (table.GRR._card_count[p] == 0) {
					end_player_count++;
				}
			}
			// 差一个人走都走完那就是结束了啊，厉害了！！！！！！！！！！！！！！！
			if (end_player_count + 1 == table.getTablePlayerNumber()) {
				for (int j = 0; j < table.getTablePlayerNumber(); j++) {
					if (table.GRR._card_count[j] != 0) {
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
								table._chuwan_shunxu[i] = j;
								break;
							}
						}
					}
				}
				table._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), 1,
						TimeUnit.SECONDS);
			} else {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.GRR._card_count[next_player] == 0) {
						next_player = (next_player + 1) % table.getTablePlayerNumber();
					} else {
						break;
					}
				}
			}
		}

		table._current_player = next_player;
		table.GRR._cur_round_pass[table._current_player] = 0;
		table._cur_out_card_count[table._current_player] = 0;
		Arrays.fill(table._cur_out_card_data[table._current_player], 0);

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants_DTZ.CT_PASS, GameConstants.INVALID_SEAT);

		auto_out_card(table);

		table.refresh_user_get_score(GameConstants_DTZ.INVALID_SEAT, false);
		table.refresh_curr_round_score(GameConstants_DTZ.INVALID_SEAT);
		if (table._turn_out_card_count == 0) {
			table._is_shou_chu = 1;
		}
	}

	private void auto_out_card(Table_DTZ table) {
		if (table._auto_out_card_scheduled != null) {
			table._auto_out_card_scheduled.cancel(true);
			table._auto_out_card_scheduled = null;
		}

		table._auto_out_card_scheduled = GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				table.operate_show_auto_time(table._current_player, GameConstants_DTZ.DISPLAY_TIME_AUTO_OUT);
				table._auto_out_card_scheduled = table._auto_out_card_scheduled = GameSchedule.put(
						new AutoOutCardRunnable(table.getRoom_id(), table._current_player, table), GameConstants_DTZ.DISPLAY_TIME_AUTO_OUT,
						TimeUnit.SECONDS);
			}
		}, GameConstants_DTZ.DISPLAY_TIME_15, TimeUnit.SECONDS);
	}

	public int adjust_out_card_right(Table_DTZ table) {
		int cbCardType = table._logic.GetCardType(_out_cards_data, _out_card_count);
		if (cbCardType == GameConstants_DTZ.CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants_DTZ.CT_ERROR;
		}
		table._logic.sort_card_date_list_by_type(this._out_cards_data, this._out_card_count, cbCardType);
		if (!table.has_rule(GameConstants_DTZ.GAME_RULE_CAN_DAI_CARD) && (cbCardType == GameConstants_DTZ.CT_THREE_TAKE_ONE
				|| cbCardType == GameConstants_DTZ.CT_THREE_TAKE_TWO || cbCardType == GameConstants_DTZ.CT_PLANE)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants_DTZ.CT_ERROR;
		}
		if (!table.has_rule(GameConstants_DTZ.GAME_RULE_CAN_DAI_CARD) && cbCardType == GameConstants_DTZ.CT_PLANE_LOST
				&& table._turn_out_card_type == GameConstants_DTZ.CT_PLANE_LOST && _out_card_count != table._turn_out_card_count) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants_DTZ.CT_ERROR;
		}
		if (table._turn_out_card_count != 0
				&& !table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants_DTZ.CT_ERROR;
		}
		if (cbCardType == GameConstants_DTZ.CT_DOUBLE_LINE && table._turn_out_card_count != 0 && _out_card_count != table._turn_out_card_count) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants_DTZ.CT_ERROR;
		}
		if (table.GRR != null && !table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],
				this._out_cards_data, this._out_card_count)) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return GameConstants.PDK_CT_ERROR;
		}
		return cbCardType;
	}

	@Override
	public boolean handler_player_be_in_room(Table_DTZ table, int seat_index) {
		RoomResponse.Builder roomResponseBuilder = RoomResponse.newBuilder();
		roomResponseBuilder.setType(MsgConstants.RESPONSE_DTZ_RECONNECT_DATA);

		RoomResponseDTZ.Builder reconnectBuilder = RoomResponseDTZ.newBuilder();
		table.load_player_info_data(reconnectBuilder);
		reconnectBuilder.setRoomInfo(table.getRoomInfo());
		reconnectBuilder.setBankerPlayer(table.GRR._banker_player);
		reconnectBuilder.setCurrentPlayer(table._current_player);
		reconnectBuilder.setPrevPlayer(table._prev_player);
		reconnectBuilder.setPrOutCardPlayer(table._turn_out_player);
		reconnectBuilder.setPrCardsCount(table._turn_out_card_count);
		reconnectBuilder.setPrOutCardType(table._turn_out_card_type);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			reconnectBuilder.addPrCardsData(table._turn_out_card_data[i]);
		}

		if (table._is_shou_chu == 1) {
			// 首出玩家一次能出完就全部弹起来
			int cbCardType = table._logic.GetCardType(table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player]);
			if (!table.has_rule(GameConstants_DTZ.GAME_RULE_CAN_DAI_CARD) && (cbCardType == GameConstants_DTZ.CT_THREE_TAKE_ONE
					|| cbCardType == GameConstants_DTZ.CT_THREE_TAKE_TWO || cbCardType == GameConstants_DTZ.CT_PLANE)) {
				cbCardType = GameConstants_DTZ.CT_ERROR;
			}
			if (cbCardType != GameConstants_DTZ.CT_ERROR) {
				reconnectBuilder.setIsHaveNotCard(true);
			}
			reconnectBuilder.setIsFirstOut(true);
		} else {
			reconnectBuilder.setIsFirstOut(false);
		}

		if (table._turn_out_card_type == GameConstants_DTZ.CT_PLANE_LOST || table._turn_out_card_type == GameConstants_DTZ.CT_PLANE) {
			reconnectBuilder.setPineCount(table._logic.getPineCount(table._turn_out_card_data, table._turn_out_card_count));
		}

		if (table._current_player != GameConstants_DTZ.INVALID_SEAT) {
			int[] user_can_out_data = new int[table.get_hand_card_count_max()];
			int can_out_card_count = table._logic.Player_Can_out_card(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count, user_can_out_data);
			for (int i = 0; i < can_out_card_count; i++) {
				reconnectBuilder.addUserCanOutData(user_can_out_data[i]);
			}
			reconnectBuilder.setUserCanOutCount(can_out_card_count);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			reconnectBuilder.addCardCount(table.GRR._card_count[i]);

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if (seat_index == i) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			reconnectBuilder.addPlayerPass(table.GRR._cur_round_pass[i]);
			reconnectBuilder.addOutCardsCount(table._cur_out_card_count[i]);
			reconnectBuilder.addOutCardsData(cur_out_cards);
			reconnectBuilder.addCardsData(cards);
			reconnectBuilder.addWinOrder(table._chuwan_shunxu[i]);
		}
		roomResponseBuilder.setCommResponse(PBUtil.toByteString(reconnectBuilder));
		table.send_response_to_player(seat_index, roomResponseBuilder);

		table.refresh_user_get_score(seat_index, false);
		table.refresh_curr_round_score(seat_index);

		return true;
	}

}
