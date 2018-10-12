package com.cai.game.mj.handler.yiyang.szg;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerDispatchCard_YiYang_SZG extends MJHandlerDispatchCard<MJTable_YiYang_SZG> {
	// TODO 是否是海底公胡，如果是海底公胡，需要走不一样的handler_operate_card
	boolean is_hai_di_gong_hu = false;

	boolean ting_send_card = false;

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable_YiYang_SZG table) {
		is_hai_di_gong_hu = false;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid(); // 可以胡了

		if (table.DEBUG_CARDS_MODE) {
			table.GRR._left_card_count = 1;
		}

		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._cur_banker = table.GRR._banker_player = table._last_dispatch_player;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);

			return;
		} else if (table.GRR._left_card_count == 1) {
			// TODO 海底公胡。杠之后，发最后一张牌，不走海底公胡。
			is_hai_di_gong_hu = true;

			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;

			table.GRR._banker_player = _seat_index;

			table._provide_player = _seat_index;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x27;
			}

			table.is_hai_di_pai = true;
			table.hai_di_card = _send_card_data;

			table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);

			boolean have_hu = false;

			for (int player = 0; player < table.getTablePlayerNumber(); player++) {
				ChiHuRight chr = table.GRR._chi_hu_rights[player];
				chr.set_empty();
				int action = table.analyse_chi_hu_card(table.GRR._cards_index[player], table.GRR._weave_items[player], table.GRR._weave_count[player],
						_send_card_data, chr, GameConstants.HU_CARD_TYPE_ZIMO, player);

				if (action != GameConstants.WIK_NULL) {
					table._playerStatus[player].add_action(GameConstants.WIK_ZI_MO);
					table._playerStatus[player].add_zi_mo(_send_card_data, player);

					if (player == _seat_index && _type == GameConstants.WIK_GANG) {
						// 杠牌人判断最后一张牌
						table.GRR._chi_hu_rights[player].opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
					} else {
						table.GRR._chi_hu_rights[player].opr_or(GameConstants.CHR_HUNAN_HAI_DI_LAO);
					}

					have_hu = true;
				} else {
					table.GRR._chi_hu_rights[player].set_empty();
				}
			}

			if (have_hu) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._playerStatus[i].has_action()) {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				}
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}

				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW), table.YY_GAME_FINISH_DELAY,
						TimeUnit.MILLISECONDS);
			}

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index; // 轮到操作的人是自己

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		table._provide_player = _seat_index;

		table._last_dispatch_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			// _send_card_data = 0x04;
		}

		// TODO 统计玩家抓的牌
		table.zhua_pai_count[_seat_index]++;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.WIK_GANG) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		}
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index); // 自摸

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			if (_type == GameConstants.WIK_GANG) {
				chr.opr_or(GameConstants.CHR_HUNAN_GANG_KAI);
			}
		} else {
			chr.set_empty();
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI;

		if (!table.is_bao_ting[_seat_index]) {
			// 获取听牌数据时，虚拟成多抓了一张牌了，不然板板胡听牌显示会有错误
			table.zhua_pai_count[_seat_index]++;

			for (int i = 0; i < card_type_count; i++) {
				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);
					if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
						table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

						ting_count++;

						if (send_card_index == i) {
							ting_send_card = true;
						}
					}

					table.GRR._cards_index[_seat_index][i]++;
				}
			}

			// 获取听牌数据时，虚拟成多抓了一张牌了，不然板板胡听牌显示会有错误
			table.zhua_pai_count[_seat_index]--;

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				table.GRR._cards_index[_seat_index][send_card_index]--;

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
			}
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;

		m_gangCardResult.cbCardCount = 0;

		if (table.GRR._left_card_count > 0) {
			int cbActionMask = GameConstants.WIK_NULL;

			if (table.is_bao_ting[_seat_index]) {
				// 报听后，只能暗杠
				cbActionMask = table._logic.analyse_gang_by_card_hand_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], m_gangCardResult);
			} else {
				// 只能分析手牌能不能暗杠；和抓的那张牌，能不能进行回头杠，其他手里的牌不能分析回头杠
				cbActionMask = table._logic.analyse_gang_by_card(table.GRR._cards_index[_seat_index], _send_card_data,
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult);
			}

			if (cbActionMask != GameConstants.WIK_NULL) {

				boolean flag = false;
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table.is_bao_ting[_seat_index]) {
						// 删除手牌并放入落地牌之前，保存状态数据信息
						int tmp_card_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						int tmp_card_count = table.GRR._cards_index[_seat_index][tmp_card_index];
						int tmp_weave_count = table.GRR._weave_count[_seat_index];

						// 删除手牌并加入一个落地牌组合，如果是暗杠，需要多加一个组合，如果是碰杠，并不需要加，因为等下分析听牌时要用
						// 发牌时，杠牌只要碰杠和暗杠这两种
						table.GRR._cards_index[_seat_index][tmp_card_index] = 0;
						if (GameConstants.GANG_TYPE_AN_GANG == m_gangCardResult.type[i]) {
							table.GRR._weave_items[_seat_index][tmp_weave_count].public_card = 0;
							table.GRR._weave_items[_seat_index][tmp_weave_count].center_card = m_gangCardResult.cbCardData[i];
							table.GRR._weave_items[_seat_index][tmp_weave_count].weave_kind = GameConstants.WIK_GANG;
							table.GRR._weave_items[_seat_index][tmp_weave_count].provide_player = _seat_index;
							++table.GRR._weave_count[_seat_index];
						}

						boolean is_ting_state_after_gang = table.is_ting_card(table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

						// 还原手牌数据和落地牌数据
						table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
						table.GRR._weave_count[_seat_index] = tmp_weave_count;

						// 杠牌之后还是听牌状态，并不需要在gang handler里更新听牌状态，只要出牌时更新就可以
						if (is_ting_state_after_gang) {
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
							flag = true;
						}
					} else {
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						flag = true;
					}
				}
				if (flag) { // 如果能杠，当前用户状态加上杠牌动作
					curPlayerStatus.add_action(GameConstants.WIK_GANG);
				}
			}
		}

		if (curPlayerStatus.has_action()) {
			// 如果有杠或胡
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.is_bao_ting[_seat_index]) {
				// TODO 报听之后，抓牌，没杠也没胡，抓什么牌出什么牌
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		if (is_hai_di_gong_hu) {
			return handler_operate_card_gong_hu(table, seat_index, operate_code, operate_card);
		} else {
			return handler_operate_card_fei_gong_hu(table, seat_index, operate_code, operate_card);
		}
	}

	public boolean handler_operate_card_gong_hu(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		// 海底公胡，吃胡等待
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_zi_mo()))
				return false;
		}

		// 海底公胡，都是自摸，只有一个人能胡
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._cur_banker = table.GRR._banker_player = table._last_dispatch_player;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_DRAW), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			// table.set_niao_card(target_player, _send_card_data, true, 0); //
			// 抓鸟
			table.set_niao_card_fei(target_player, _send_card_data, true, 0); // 飞鸟

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = _send_card_data;

			table._cur_banker = target_player;

			if (is_hai_di_gong_hu) {
				table.process_chi_hu_player_operate(target_player, _send_card_data, false);
			} else {
				table.process_chi_hu_player_operate(target_player, _send_card_data, true);
			}

			table.process_chi_hu_player_score(target_player, target_player, _send_card_data, true);

			table.display_bird_cards();

			table._player_result.zi_mo_count[target_player]++;

			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);

			return true;
		}
		}

		return true;
	}

	public boolean handler_operate_card_fei_gong_hu(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();

			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);

			if (table.is_bao_ting[seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		case GameConstants.WIK_ZI_MO: {
			// table.set_niao_card(_seat_index, _send_card_data, true, 0); // 抓鸟
			table.set_niao_card_fei(_seat_index, _send_card_data, true, 0); // 飞鸟

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._cur_banker = _seat_index;

			if (is_hai_di_gong_hu) {
				table.process_chi_hu_player_operate(_seat_index, operate_card, false);
			} else {
				table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			}

			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table.display_bird_cards();

			table._player_result.zi_mo_count[_seat_index]++;

			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[_seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), table.YY_GAME_FINISH_DELAY,
					TimeUnit.MILLISECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YiYang_SZG table, int seat_index) {
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
			tableResponse.addTrustee(table.is_bao_ting[i]);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (table.GRR._left_card_count != 0) {
			// 如果断线重连的人是自己
			if (seat_index == _seat_index) {
				table._logic.remove_card_by_data(hand_cards, _send_card_data);
				hand_card_count--;
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._left_card_count != 0) {
			int real_card = _send_card_data;
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		} else {
			table.operate_show_card(_seat_index, GameConstants.Show_Card_Center, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);
		}

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(MJTable_YiYang_SZG table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		table.exe_out_card(_seat_index, card, _type);

		return true;
	}
}
