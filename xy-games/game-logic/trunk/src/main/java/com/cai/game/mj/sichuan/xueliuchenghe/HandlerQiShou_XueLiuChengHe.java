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
import com.cai.game.mj.handler.NewHandlerQiShou;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.SiChuanTrusteeRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerQiShou_XueLiuChengHe extends NewHandlerQiShou<AbstractSiChuanMjTable> {
	List<Integer> must_out_cards = new ArrayList<>();

	int last_card = -1;

	@Override
	public void exe(AbstractSiChuanMjTable table) {
		must_out_cards.clear();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;
		table._provide_player = _seat_index;

		table._last_dispatch_player = _seat_index;

		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		table.analyse_state = table.FROM_NORMAL;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], 0, chr, Constants_SiChuan.HU_CARD_TYPE_ZI_MO, _seat_index);

		int[] hand_cards = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], hand_cards,
				table.ding_que_pai_se[_seat_index]);

		last_card = hand_cards[hand_card_count - 1];

		if (GameConstants.WIK_NULL != action) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);

			curPlayerStatus.add_zi_mo(last_card, _seat_index);
		} else {
			chr.set_empty();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == _seat_index)
				continue;

			table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
					table.GRR._weave_items[i], table.GRR._weave_count[i], i, 0);

			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}

		int temp_cards[] = new int[GameConstants.MAX_COUNT];
		int temp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], temp_cards,
				table.ding_que_pai_se[_seat_index]);

		Map<Integer, Integer> tmpMap = table.player_switched_cards[_seat_index];

		for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
			int card = entry.getKey();
			int count = entry.getValue();

			if (count > 0) {
				for (int j = 0; j < hand_card_count && count > 0; j++) {
					if (card == temp_cards[j]) {
						temp_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
						--count;
					}
				}
			}
		}

		int must_out_card_count = 0;
		for (int i = 0; i < temp_hand_card_count; i++) {
			int pai_se = table._logic.get_card_color(table.get_real_card(temp_cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
				must_out_card_count++;
				must_out_cards.add(temp_cards[i]);
			}
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

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
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
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int cards[] = new int[GameConstants.MAX_COUNT];
			hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
					table.ding_que_pai_se[_seat_index]);

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

		if (table.GRR._left_card_count > 0) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_card_all_xzdd(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, true, table.passed_gang_cards[_seat_index],
					table.passed_gang_count[_seat_index]);

			boolean flag = false;
			if (cbActionMask != GameConstants.WIK_NULL) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table._logic.get_card_color(m_gangCardResult.cbCardData[i]) + 1 != table.ding_que_pai_se[_seat_index]) {
						curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						flag = true;
					}
				}
			}
			if (flag) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
			}
		}

		// 重置胡牌分析的入口点
		table.analyse_state = table.FROM_NORMAL;

		if (table.had_hu_pai[_seat_index]) {
			handler_be_set_trustee(table, _seat_index);
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
				table.exe_jian_pao_hu_new(_seat_index, GameConstants.WIK_ZI_MO, last_card);
			} else {
				table.operate_player_action(_seat_index, true);

				int auto_out_card = last_card;
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

		table.cancel_trustee_schedule(seat_index);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();

			table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		if (table._playerStatus[seat_index].has_zi_mo() && operate_code != GameConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, m_gangCardResult.cbCardData[i], operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
		case GameConstants.WIK_ZI_MO: {
			table.hu_card_list.add(operate_card);

			table.process_hu_cards(_seat_index, _seat_index, operate_card);

			table.GRR._chi_hu_card[seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(seat_index, operate_card, true);
			table.process_chi_hu_player_score(seat_index, seat_index, operate_card, true);

			table.operate_player_cards_flag(_seat_index, 0, null, 0, null);

			table._player_result.zi_mo_count[seat_index]++;

			table.next_banker_player = seat_index;

			table.had_hu_pai[seat_index] = true;

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

			// 天胡之后 需要获取一次听牌数据
			table._playerStatus[seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[seat_index]._hu_cards,
					table.GRR._cards_index[seat_index], table.GRR._weave_items[seat_index], table.GRR._weave_count[seat_index], seat_index, 0);
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(seat_index, 1, ting_cards);
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

		MahjongUtils.dealAllPlayerCardsNoSpecial(table, tableResponse);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;
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

		table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(AbstractSiChuanMjTable table, int seat_index) {
		table.operate_player_action(seat_index, true);

		if (table._playerStatus[seat_index].has_zi_mo()) {
			table.exe_jian_pao_hu_new(_seat_index, GameConstants.WIK_ZI_MO, last_card);
		} else {
			table.operate_player_action(_seat_index, true);

			int auto_out_card = last_card;
			int size = must_out_cards.size();
			if (size != 0) {
				int index = RandomUtil.getRandomNumber(size);
				auto_out_card = must_out_cards.get(index);
			}

			GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, auto_out_card), table.DELAY_AUTO_OPERATE, TimeUnit.MILLISECONDS);
		}
		return false;
	}
}
