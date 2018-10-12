package com.cai.game.mj.chenchuang.xianning;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.mj.GameConstants_MJ_XIAN_NING;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_XianNing extends MJHandlerDispatchCard<Table_XianNing> {
	boolean ting_send_card = false;

	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerDispatchCard_XianNing() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_XianNing table) {
		table._playerStatus[_seat_index].clear_cards_abandoned_hu();
		table._playerStatus[_seat_index].clear_cards_abandoned_peng();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		// 流局
		if (table.GRR._left_card_count == 8) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_LIU_JU }, 1,
					GameConstants.INVALID_SEAT);

			GameSchedule.put(new Runnable() {
				@Override
				public void run() {
					table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
				}
			}, 2, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;

		table._send_card_count++;

		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x14;
		}
		table._provide_player = _seat_index;
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int type = GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_ZI_MO;
		if (_type == GameConstants.GANG_TYPE_AN_GANG || _type == GameConstants.GANG_TYPE_JIE_GANG || _type == GameConstants.GANG_TYPE_ADD_GANG
				|| _type == GameConstants.GANG_TYPE_HONG_ZHONG || _type == GameConstants.GANG_TYPE_LAI_ZI)
			type = GameConstants_MJ_XIAN_NING.HU_CARD_TYPE_GANG_KAI;
		int action = table.analyse_chi_hu_card_new(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, type, _seat_index, _seat_index);
		// 海底捞最后12张牌。看谁能自摸。不用打
		if (table.GRR._left_card_count == table.entrySeabedCount())
			table.operate_effect_action(GameConstants.INVALID_SEAT, GameConstants.Effect_Action_Other, 1,
					new long[] { GameConstants.EFFECT_LAST_FOUR_CARD }, 1, GameConstants.INVALID_SEAT);
		if (table.GRR._left_card_count <= table.entrySeabedCount()) {
			--table.GRR._left_card_count;

			int nextSeatIndex = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			if (action == GameConstants.WIK_NULL || table.can_win_but_without_enough_score) {
				chr.set_empty();
				table.exe_dispatch_card(nextSeatIndex, GameConstants.WIK_NULL, 3000);// 延迟3秒
			} else {
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

				table.change_player_status(_seat_index, GameConstants.Player_Status_NULL);
				table.operate_player_action(_seat_index, false);
			}

			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			int real_card = _send_card_data;
			if (ting_send_card) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
			}
			if (table._logic.is_magic_card(_send_card_data)) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
			if (_send_card_data == GameConstants_MJ_XIAN_NING.HZ_MAGIC_CARD) {
				real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
			}

			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

			table._provide_card = _send_card_data;
			return;

		}
		--table.GRR._left_card_count;
		if (action != GameConstants.WIK_NULL && !table.can_win_but_without_enough_score) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
		} else {
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			chr.set_empty();

			// TODO 表示牌型能胡但是没达到起胡分
			if (table.can_win_but_without_enough_score) {
				table.operate_cant_win_info(_seat_index);
			}
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;
		int send_card_index = table._logic.switch_to_card_index(_send_card_data);
		ting_send_card = false;

		int card_type_count = GameConstants.MAX_ZI_FENG;

		for (int i = 0; i < card_type_count; i++) {
			count = table.GRR._cards_index[_seat_index][i];

			if (count > 0) {
				table.GRR._cards_index[_seat_index][i]--;

				table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
						table._playerStatus[_seat_index]._hu_out_cards[ting_count], table._playerStatus[_seat_index]._hu_out_cards_fan[ting_count],
						table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

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
			int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[_seat_index], cards);
			int[] copy_tmp_cards = Arrays.copyOf(cards, GameConstants.MAX_COUNT);
			table.GRR._cards_index[_seat_index][send_card_index]++;

			for (int i = 0; i < hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (copy_tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (table._logic.is_magic_card(copy_tmp_cards[i])) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (copy_tmp_cards[i] == Constants_EZ.HZ_CARD) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}

			table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (_send_card_data == Constants_EZ.HZ_CARD) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		table._provide_card = _send_card_data;

		if (table.GRR._left_card_count > table.entrySeabedCount()) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_hong_zhong_all_hu_bei(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, true,
					table.GRR._cards_abandoned_gang[_seat_index], HandlerSelectMagic_XianNing._da_dian_card);

			if (cbActionMask != GameConstants.WIK_NULL) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (table.istrustee[_seat_index]) {
			handler_be_set_trustee(table, _seat_index);
		} else {
			if (curPlayerStatus.has_action()) {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_OR_OUT_CARD);
				table.operate_player_action(_seat_index, false);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}

		return;
	}

	@Override
	public boolean handler_player_out_card(Table_XianNing table, int seat_index, int card) {
		if(card > 80000){
			card -= 80000;
		}else if(card > 40000){
			card -= 40000;
			_type = GameConstants_MJ_XIAN_NING.WIK_DA_CHU;
		}else if(table.GRR._left_card_count > table.entrySeabedCount() && table.has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_HZLZKG)){
			int cardf = table.get_real_card(card);
			if(cardf == Constants_EZ.HZ_CARD || table._logic.is_magic_card(cardf)){
				PlayerStatus playerStatus = table._playerStatus[_seat_index];
				playerStatus.clean_action();
				playerStatus.add_action(GameConstants_MJ_XIAN_NING.WIK_DA_CHU);
				playerStatus.add_action_card(1, card, GameConstants_MJ_XIAN_NING.WIK_DA_CHU, seat_index);
				playerStatus.add_action(GameConstants_MJ_XIAN_NING.WIK_DA_CHU_GANG);
				playerStatus.add_action_card(1, card, GameConstants_MJ_XIAN_NING.WIK_DA_CHU_GANG, seat_index);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(_seat_index, false);
				return false;
			}
		}
		
		boolean flag = false;
		card = table.get_real_card(card);
		if(table.GRR._left_card_count == table.entrySeabedCount() || !table.has_rule(GameConstants_MJ_XIAN_NING.GAME_RULE_HZLZKG)){
			if(card == Constants_EZ.HZ_CARD || table._logic.is_magic_card(card)){
				if(table.GRR._left_card_count == table.entrySeabedCount())
					_type = GameConstants_MJ_XIAN_NING.WIK_DA_CHU;
				table._playerStatus[_seat_index]._card_status = 2;
				flag = true;
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

		table.exe_out_card(_seat_index, card, _type);
		if(flag)
			table._playerStatus[_seat_index]._card_status = 0;
		return true;
	}

	@Override
	public boolean handler_operate_card(Table_XianNing table, int seat_index, int operate_code, int operate_card) {
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
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table.GRR._left_card_count < table.entrySeabedCount()) {
				int nextSeatIndex = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table.exe_dispatch_card(nextSeatIndex, _type, 0);
				return true;
			}

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
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
		case GameConstants_MJ_XIAN_NING.WIK_DA_CHU: {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_MJ_XIAN_NING.WIK_DA_CHU }, 1);
			handler_player_out_card(table, seat_index, operate_card + 40000);
			return true;
		}
		case GameConstants_MJ_XIAN_NING.WIK_DA_CHU_GANG: {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants_MJ_XIAN_NING.WIK_DA_CHU_GANG }, 1);
			handler_player_out_card(table, seat_index, operate_card + 80000);
			return true;
		}
		case GameConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table._cur_banker = _seat_index;

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			table._player_result.zi_mo_count[_seat_index]++;

			table.showAction(_seat_index);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), 1, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_XianNing table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// roomResponse.setTarget(seat_index);
		// roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
		// table.send_response_to_other(seat_index, roomResponse);

		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

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
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int real_card = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (real_card == Constants_EZ.HZ_CARD) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0) {
					// 暗杠的牌的显示
					if (seat_index == i) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					} else {
						weaveItem_item.setCenterCard(0);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
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
		int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
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
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (hand_cards[j] == Constants_EZ.HZ_CARD) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (hand_cards[j] == Constants_EZ.HZ_CARD) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		// TODO 添加是否托管
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.istrustee[i]);
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
				roomResponse.addDouliuzi(table._playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}
		if (table._logic.is_magic_card(_send_card_data)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		}
		if (real_card == Constants_EZ.HZ_CARD) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);

			if (seat_index == _seat_index) {
				// 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
				table.operate_auto_win_card(seat_index, false);
			} else {
				// 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
				table.operate_auto_win_card(seat_index, true);
			}
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_XianNing table, int seat_index) {
		if (!table.istrustee[seat_index])
			return false;

		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];

		if (curPlayerStatus.has_zi_mo() && _send_card_data != GameConstants.INVALID_VALUE) {
			// 有自摸就胡牌
			table.operate_player_action(seat_index, true);

			table.exe_jian_pao_hu(seat_index, GameConstants.WIK_ZI_MO, _send_card_data);

			return true;
		} else {
			// 有明杠、暗杠，等待3秒，如果3秒之内点了‘杠’操作，进行‘杠’动作并自动取消托管
			if (curPlayerStatus.has_action() && curPlayerStatus.is_respone() == false) {
				table.change_player_status(seat_index, GameConstants.Player_Status_OPR_CARD);
				// 显示操作按钮
				table.operate_player_action(seat_index, false);

				// 添加定时任务，3秒之内点了操作，取消定时任务
				table._trustee_schedule[seat_index] = GameSchedule.put(new Runnable() {
					@Override
					public void run() {
						// 关闭操作按钮
						table.operate_player_action(seat_index, true);

						handler_player_out_card(table, seat_index, _send_card_data);
					}
				}, table.action_wait_time, TimeUnit.MILLISECONDS);
			} else {
				// 没任何操作，直接出牌
				GameSchedule.put(new Runnable() {
					@Override
					public void run() {
						handler_player_out_card(table, seat_index, _send_card_data);
					}
				}, table.auto_out_card_delay, TimeUnit.MILLISECONDS);
			}

			return true;
		}
	}
}
