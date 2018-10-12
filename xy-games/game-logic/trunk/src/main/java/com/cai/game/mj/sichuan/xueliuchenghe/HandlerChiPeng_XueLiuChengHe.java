package com.cai.game.mj.sichuan.xueliuchenghe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.MJHandlerChiPeng;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;
import com.cai.game.mj.sichuan.SiChuanTrusteeRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerChiPeng_XueLiuChengHe extends MJHandlerChiPeng<AbstractSiChuanMjTable> {
	List<Integer> must_out_cards = new ArrayList<>();

	@Override
	public void exe(AbstractSiChuanMjTable table) {
		must_out_cards.clear();

		// 牌桌上只要有人吃碰牌，牌桌就是非杠上杠状态
		table.gang_shang_gang = false;

		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._current_player = _seat_index;

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player;
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();

		// 添加过杠
		int cIndex = table._logic.switch_to_card_index(_card);
		if (table.GRR._cards_index[_seat_index][cIndex] == 1)
			table._playerStatus[_seat_index].add_cards_abandoned_gang(cIndex);

		table.score_when_abandoned_win[_seat_index] = 0;

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
				table.ding_que_pai_se[_seat_index]);

		Map<Integer, Integer> tmpMap = table.player_switched_cards[_seat_index];

		for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
			int card = entry.getKey();
			int count = entry.getValue();

			if (count > 0) {
				for (int j = 0; j < hand_card_count && count > 0; j++) {
					if (card == cards[j]) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
						--count;
					}
				}
			}
		}

		int must_out_card_count = 0;
		for (int i = 0; i < hand_card_count; i++) {
			int pai_se = table._logic.get_card_color(table.get_real_card(cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[_seat_index]) {
				must_out_card_count++;
				must_out_cards.add(cards[i]);
			}
		}

		if (must_out_card_count > 0) {
			for (int i = 0; i < hand_card_count; i++) {
				int pai_se = table._logic.get_card_color(table.get_real_card(cards[i]));
				if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				}
			}
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
			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
			int tmp_hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], tmp_cards,
					table.ding_que_pai_se[_seat_index]);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}

			for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
				int card = entry.getKey();
				int nCount = entry.getValue();

				if (nCount > 0) {
					for (int j = 0; j < hand_card_count && nCount > 0; j++) {
						if (card == table.get_real_card(tmp_cards[j])) {
							tmp_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
							--nCount;
						}
					}
				}
			}

			if (!table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.hasRuleDingQue) {
				if (must_out_card_count > 0) {
					for (int j = 0; j < tmp_hand_card_count; j++) {
						int pai_se = table._logic.get_card_color(table.get_real_card(tmp_cards[j]));
						if ((pai_se + 1) != table.ding_que_pai_se[_seat_index]) {
							tmp_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						}
					}
				}
			}

			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
		} else {
			// 刷新手牌
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

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

	@Override
	public boolean handler_operate_card(AbstractSiChuanMjTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();

			table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
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
		for (int i = 0; i < hand_card_count; i++) {
			int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
				must_out_card_count++;
			}
		}

		if (must_out_card_count > 0) {
			for (int i = 0; i < hand_card_count; i++) {
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
			table.operate_player_cards_with_ting(seat_index, hand_card_count, hand_cards, 0, null);
		} else if (table._playerStatus[seat_index].get_status() == GameConstants.Player_Status_OUT_CARD && must_out_card_count > 0) {
			table.operate_player_cards(seat_index, hand_card_count, hand_cards, table.GRR._weave_count[seat_index],
					table.GRR._weave_items[seat_index]);
		}

		if (seat_index != _seat_index)
			MahjongUtils.showTingPai(table, seat_index);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(AbstractSiChuanMjTable table, int seat_index, int card) {
		if (table.is_match() || table.isCoinRoom() || table.isClubMatch()) {
			table.chi_peng_index_invaild();
		}

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

		table.cancel_trustee_schedule(_seat_index);

		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(AbstractSiChuanMjTable table, int seat_index) {
		int auto_out_card = 0;
		int size = must_out_cards.size();
		if (size != 0) {
			int index = RandomUtil.getRandomNumber(size);
			auto_out_card = must_out_cards.get(index);
		} else {
			int cards[] = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[_seat_index], cards,
					table.ding_que_pai_se[_seat_index]);
			auto_out_card = cards[hand_card_count - 1];
		}

		GameSchedule.put(new OutCardRunnable(table.getRoom_id(), seat_index, auto_out_card), table.DELAY_AUTO_OPERATE, TimeUnit.MILLISECONDS);

		return false;
	}
}
