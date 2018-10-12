/**
 * 
 */
package com.cai.game.hh.handler.wcphz;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class PHZHandlerDispatchFirstCard_WangCheng extends HHHandlerDispatchCard<WangChengHHTable> {

	@Override
	public void exe(WangChengHHTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
			        % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}
		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x7;
		}
		--table.GRR._left_card_count;

		// 最后一张牌亮牌
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);

		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		for (int n = 0; n < table.getPlayerCount(); n++) {
			if (n == _seat_index)
				continue;

			int an_long_Index[] = new int[5];
			int an_long_count = 0;
			for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
				if (table.GRR._cards_index[n][i] == 4) {
					an_long_Index[an_long_count++] = i;
				}
			}
			if (an_long_count > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				// 效果
				table.operate_effect_action(n, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				        GameConstants.INVALID_SEAT);

				for (int i = 0; i < an_long_count; i++) {
					int cbWeaveIndex = table.GRR._weave_count[n];
					table.GRR._weave_items[n][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[n][cbWeaveIndex].center_card = table._logic
					        .switch_to_card_data(an_long_Index[i]);
					table.GRR._weave_items[n][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
					table.GRR._weave_items[n][cbWeaveIndex].provide_player = n;
					table.GRR._weave_items[n][cbWeaveIndex].hu_xi = table._logic
					        .get_weave_hu_xi(table.GRR._weave_items[n][cbWeaveIndex]);
					table.GRR._weave_count[n]++;
					table._long_count[n]++;
					// 删除手上的牌
					table.GRR._cards_index[n][an_long_Index[i]] = 0;

					table.GRR._card_count[n] = table._logic.get_card_count_by_index(table.GRR._cards_index[n]);

				}
				// 刷新手牌包括组合
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[n], cards);
				table.operate_player_cards(n, hand_card_count, cards, table.GRR._weave_count[n],
				        table.GRR._weave_items[n]);
			}
			if (an_long_count >= 2)
				table._ti_two_long[n] = true;

			if (an_long_count == 2)
				table.mian_zhang[n] = 1;
			if (an_long_count == 3)
				table.mian_zhang[n] = 2;
		}

		table.exe_chuli_first_card(_seat_index, GameConstants.WIK_NULL, 2500);
		_send_card_data = 0;
		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(WangChengHHTable table, int seat_index, int operate_code, int operate_card,
	        int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		table.log_info(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户" + seat_index + "操作用户");
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
			        new long[] { GameConstants.WIK_NULL }, 1);
			if (playerStatus.has_zi_mo() == true) {
				int index = -1;
				for (int i = 0; i < table._guo_hu_pai_count[seat_index]; i++) {
					if (table._guo_hu_pai_cards[seat_index][i] == operate_card) {
						index = i;
					}
				}
				if (index == -1) {
					index = table._guo_hu_pai_count[seat_index]++;
				}
				table._guo_hu_pai_cards[seat_index][index] = operate_card;

				int all_hu_xi = 0;
				for (int i = 0; i < table._hu_weave_count[seat_index]; i++) {
					all_hu_xi += table._hu_weave_items[seat_index][i].hu_xi;
				}
				table._guo_hu_xi[seat_index][index] = all_hu_xi;
			}
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		// if (seat_index != _seat_index) {
		// table.log_info("DispatchCard 不是当前玩家操作");
		// return false;
		// }
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
					        + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
					        table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
					        + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
					        table._playerStatus[target_player]._action_count,
					        table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				}
			}
		}

		// 优先级最高的人还没操作

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("最用户操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL: {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
				        GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				// 要出牌，但是没有牌出设置成相公 下家用户发牌
				int pai_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}
				if (pai_count == 0) {
					// table._is_xiang_gong[_seat_index] = true;
					// table.operate_player_xiang_gong_flag(_seat_index,
					// table._is_xiang_gong[_seat_index]);
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					table.log_info(next_player
					        + "可以胡，而不胡的情况                                                                                                                                                                                                                                                       "
					        + _seat_index);
					return true;
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.operate_player_status();
			}
			return true;

		}

		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}
			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
			        delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(WangChengHHTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int x = 0; x < hand_card_count; x++) {
			if (table.is_card_has_wei(hand_cards[x])) { // 如果是偎的牌
				// 判断打出这张牌是否能听牌
				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]--;
				boolean b_is_ting_state = table.is_ting_state(table.GRR._cards_index[seat_index],
				        table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index);
				table.GRR._cards_index[seat_index][table._logic.switch_to_card_index(hand_cards[x])]++;

				if (!b_is_ting_state)
					hand_cards[x] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_SHOOT;
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// if (table._is_xiang_gong[seat_index] == true)
		// table.operate_player_xiang_gong_flag(seat_index,
		// table._is_xiang_gong[seat_index]);

		table.istrustee[seat_index] = false;

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
