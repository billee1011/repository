package com.cai.game.mj.sichuan.xueliuchenghe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.MJHandlerDispatchCard;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.SiChuanTrusteeRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerDispatchCard_XueLiuChengHe extends MJHandlerDispatchCard<AbstractSiChuanMjTable> {
	boolean ting_send_card = false;

	List<Integer> must_out_cards = new ArrayList<>();

	@SuppressWarnings("static-access")
	@Override
	public void exe(AbstractSiChuanMjTable table) {
		must_out_cards.clear();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		table.score_when_abandoned_win[_seat_index] = 0;

		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		boolean lj = table.liu_ju();
		if (lj)
			return;

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._provide_player = _seat_index;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x26;
		}

		table._send_card_data = _send_card_data;

		table.mo_pai_count[_seat_index]++;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = Constants_SiChuan.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			card_type = Constants_SiChuan.HU_CARD_TYPE_GANG_KAI;
		} else {
			// 非开杠之后的抓牌，重置牌桌上的杠上杠状态
			table.gang_shang_gang = false;
		}

		table.analyse_state = table.FROM_NORMAL;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type, _seat_index);

		if (action != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			chr.set_empty();
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], temp_cards,
				table.ding_que_pai_se[_seat_index]);

		Map<Integer, Integer> tmpMap = table.player_switched_cards[_seat_index];

		for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
			int card = entry.getKey();
			int count = entry.getValue();

			if (count > 0) {
				for (int j = 0; j < temp_hand_card_count && count > 0; j++) {
					if (card == temp_cards[j]) {
						temp_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
						--count;
					}
				}
			}
		}

		int must_out_card_count = 0;
		int tmp_pai_se = table._logic.get_card_color(_send_card_data);

		for (int i = 0; i < temp_hand_card_count; i++) {
			int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
				must_out_card_count++;
				must_out_cards.add(temp_cards[i]);
			}
		}

		if ((tmp_pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
			must_out_card_count++;
			must_out_cards.add(_send_card_data);
		}

		if (must_out_card_count > 0) {
			for (int i = 0; i < temp_hand_card_count; i++) {
				int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
				if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
					temp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				}
			}

			table.operate_player_cards(_seat_index, temp_hand_card_count, temp_cards, table.GRR._weave_count[_seat_index],
					table.GRR._weave_items[_seat_index]);
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
						table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, ting_count);

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

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			table.GRR._cards_index[_seat_index][send_card_index]--;

			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
					table.ding_que_pai_se[_seat_index]);

			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
				int card = entry.getKey();
				int nCount = entry.getValue();

				if (nCount > 0) {
					for (int j = 0; j < hand_card_count && nCount > 0; j++) {
						if (card == table.get_real_card(cards[j])) {
							cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
							--nCount;
						}
					}
				}
			}

			if (must_out_card_count > 0) {
				for (int j = 0; j < hand_card_count; j++) {
					int pai_se = table._logic.get_card_color(table.get_real_card(cards[j]));
					if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		} else if ((tmp_pai_se + 1) != table.ding_que_pai_se[_seat_index] && must_out_card_count > 0) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
		}

		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
			table.gang_dispatch_count++;
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > table.LEFT_CARD) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_card_all_xlch(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table._playerStatus[_seat_index].get_cards_abandoned_gang());

			if (cbActionMask != GameConstants.WIK_NULL) {
				boolean flag = false;
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table.had_hu_pai[_seat_index]) {
						boolean need_display_gang = true;
						int hu_card_count = table._playerStatus[_seat_index]._hu_card_count;
						for (int y = 0; y < hu_card_count; y++) {
							if (m_gangCardResult.cbCardData[i] == table._playerStatus[_seat_index]._hu_cards[y]) {
								need_display_gang = false;
								break;
							}
						}

						if (need_display_gang) {
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

							boolean has_huan_zhang = table.check_gang_huan_zhang(_seat_index, m_gangCardResult.cbCardData[i]);

							// 还原手牌数据和落地牌数据
							table.GRR._cards_index[_seat_index][tmp_card_index] = tmp_card_count;
							table.GRR._weave_count[_seat_index] = tmp_weave_count;

							// 杠牌之后还是听牌状态，并不需要在gang
							// handler里更新听牌状态，只要出牌时更新就可以
							if (!has_huan_zhang) {
								curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
								flag = true;
							}
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

		// 重置胡牌分析的入口点
		table.analyse_state = table.FROM_NORMAL;

		if (table.had_hu_pai[_seat_index]) {
			boolean has_win = curPlayerStatus.has_zi_mo();
			boolean has_gang = curPlayerStatus.has_action_by_code(GameConstants.WIK_GANG);

			if ((has_win && !has_gang) || (!has_win && !has_gang)) {
				// 如果胡牌之后，只有胡，或者没胡也没杠
				handler_be_set_trustee(table, _seat_index);
			} else {
				// 如果胡牌之后，只有杠，或者有胡有杠
				int delay = table.get_over_time_value();
				table.over_time_left[_seat_index] = delay;
				table.process_over_time_counter(_seat_index);

				table.over_time_trustee_schedule[_seat_index] = GameSchedule.put(new SiChuanTrusteeRunnable(table.getRoom_id(), _seat_index), delay,
						TimeUnit.SECONDS);
				table.schedule_start_time = System.currentTimeMillis();

				if (curPlayerStatus.has_action()) {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
				} else {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			}
		} else if (table.over_time_trustee[_seat_index] == false) {
			int delay = table.get_over_time_value();
			table.over_time_left[_seat_index] = delay;
			table.process_over_time_counter(_seat_index);

			table.over_time_trustee_schedule[_seat_index] = GameSchedule.put(new SiChuanTrusteeRunnable(table.getRoom_id(), _seat_index), delay,
					TimeUnit.SECONDS);
			table.schedule_start_time = System.currentTimeMillis();

			if (curPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		} else if (table.over_time_trustee[_seat_index] == true) {
			if (curPlayerStatus.has_zi_mo()) {
				table.exe_jian_pao_hu_new(_seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
			} else {
				table.operate_player_action(_seat_index, true);

				int auto_out_card = _send_card_data;
				int size = must_out_cards.size();
				if (size != 0) {
					int index = RandomUtil.getRandomNumber(size);
					auto_out_card = must_out_cards.get(index);
				}

				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, auto_out_card), table.DELAY_AUTO_OPERATE,
						TimeUnit.MILLISECONDS);
			}
		}
	}

	@Override
	public boolean handler_operate_card(AbstractSiChuanMjTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_error("没有这个操作");
			return true;
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
		playerStatus.clean_status();

		// 添加过杠
		if ((table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)) && operate_code != GameConstants.WIK_GANG) {
			int cIndex = table._logic.switch_to_card_index(_send_card_data);
			if (table.GRR._cards_index[_seat_index][cIndex] == 1) {
				table._playerStatus[_seat_index].add_cards_abandoned_gang(cIndex);
			}
		}

		table.cancel_trustee_schedule(seat_index);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		if (table._playerStatus[seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			table.GRR._chi_hu_rights[seat_index].set_empty();

			// 记录过胡的时候，牌型的番数，变大了，本圈才能接炮
			table.score_when_abandoned_win[seat_index] = table.score_when_win[seat_index];
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG
							|| _type == GameConstants.GANG_TYPE_JIE_GANG) {
						// 杠之后抓了一张牌，继续杠，牌桌上就是杠上杠状态
						table.gang_shang_gang = true;
					}
					table.exe_gang(_seat_index, _seat_index, m_gangCardResult.cbCardData[i], operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
		case GameConstants.WIK_ZI_MO: {
			table.hu_card_list.add(operate_card);

			table.process_hu_cards(_seat_index, _seat_index, operate_card);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table.operate_player_cards_flag(_seat_index, 0, null, 0, null);

			table._player_result.zi_mo_count[_seat_index]++;

			if (table.has_win() == false) {
				table.next_banker_player = _seat_index;
			}

			table.had_hu_pai[_seat_index] = true;

			table.operate_player_hu_cards();
			table.operate_player_score();

			boolean lj = table.liu_ju();
			if (lj)
				return true;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i]._hu_card_count > 0) {
					table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
				}
			}

			table.exe_dispatch_card(table.get_next_seat(_seat_index), GameConstants.WIK_NULL, 0);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(AbstractSiChuanMjTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		for (int card : table.hu_card_list) {
			roomResponse.addCardData(card);
		}

		MahjongUtils.showTouZiSiChuan(table, roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		MahjongUtils.dealCommonDataReconnect(table, roomResponse, tableResponse);

		MahjongUtils.dealAllPlayerCardsWithDirection(table, tableResponse);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		if (seat_index == _seat_index) {
			table.remove_card_by_data(hand_cards, _send_card_data);
		}

		int out_ting_count = (seat_index == _seat_index) ? table._playerStatus[seat_index]._hu_out_card_count : 0;
		roomResponse.setOutCardCount(out_ting_count);

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

		Map<Integer, Integer> tmpMap = table.player_switched_cards[seat_index];

		for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
			int card = entry.getKey();
			int count = entry.getValue();

			if (count > 0) {
				for (int j = 0; j < hand_card_count && count > 0; j++) {
					if (card == table.get_real_card(hand_cards[j])) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
						--count;
					}
				}
			}
		}

		int must_out_card_count = 0;
		for (int i = 0; i < hand_card_count - 1; i++) {
			int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
				must_out_card_count++;
			}
		}

		if (seat_index == _seat_index) {
			int tmp_pai_se = table._logic.get_card_color(_send_card_data);
			if ((tmp_pai_se + 1) == table.ding_que_pai_se[seat_index]) {
				must_out_card_count++;
			}
		}

		if (must_out_card_count > 0) {
			for (int i = 0; i < hand_card_count - 1; i++) {
				int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
				if ((pai_se + 1) != table.ding_que_pai_se[seat_index]) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				}
			}
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			for (int j = 0; j < ting_card_cout; j++) {
				roomResponse.addDouliuzi(table.ting_pai_fan_shu[seat_index][i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		MahjongUtils.showHuCardsSiChuan(table, roomResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (out_ting_count > 0) {
			table.operate_player_cards_with_ting(seat_index, hand_card_count - 1, hand_cards, 0, null);
		} else if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD && must_out_card_count > 0) {
			table.operate_player_cards(seat_index, hand_card_count - 1, hand_cards, table.GRR._weave_count[seat_index],
					table.GRR._weave_items[seat_index]);
		}

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		if (_type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, seat_index);
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		}

		if (seat_index != _seat_index)
			MahjongUtils.showTingPai(table, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(AbstractSiChuanMjTable table, int seat_index, int card) {
		int tmpCard = card;
		card = table.get_real_card(card);

		if (tmpCard > GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD) {
			Map<Integer, Integer> tmpMap = table.player_switched_cards[seat_index];
			if (tmpMap.containsKey(card)) {
				int count = tmpMap.get(card);
				if (count > 0) {
					tmpMap.replace(card, --count);
				}
			}
		}

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

		table.cancel_trustee_schedule(seat_index);

		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG) {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_GANG);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(AbstractSiChuanMjTable table, int seat_index) {
		table.operate_player_action(seat_index, true);

		if (table._playerStatus[seat_index].has_zi_mo()) {
			table.exe_jian_pao_hu_new(seat_index, GameConstants.WIK_ZI_MO, _send_card_data);
		} else {
			int auto_out_card = _send_card_data;
			int size = must_out_cards.size();
			if (size != 0) {
				int index = RandomUtil.getRandomNumber(size);
				auto_out_card = must_out_cards.get(index);
			}

			GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, auto_out_card), table.DELAY_AUTO_OPERATE, TimeUnit.MILLISECONDS);
		}

		return false;
	}
}
