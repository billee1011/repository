package com.cai.game.mj.chenchuang.xianning;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.common.constant.game.mj.GameConstants_MJ_XIAN_NING;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerChiPeng_XianNing extends MJHandlerChiPeng<Table_XianNing> {
	protected int _seat_index;
	protected int _action;
	protected int _card;
	protected int _provider;
	protected int _type;

	protected GangCardResult m_gangCardResult;

	public HandlerChiPeng_XianNing() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void reset_status(int seat_index, int provider, int action, int card, int type) {
		_seat_index = seat_index;
		_action = action;
		_card = card;
		_provider = provider;
		_type = type;
	}

	@Override
	public void exe(Table_XianNing table) {
		// 开口次数加1
		table.player_kai_kou[_seat_index]++;
		// 记录每个玩家第三次开口的提供者（玩家位置索引+1，为0表示没有），用来处理包赔（胡牌者必须有清一色或者将一色的牌型）
		if (table.player_kai_kou[_seat_index] == 3)
			table.player_kai_kou3[_seat_index] = _provider + 1;

		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		table._current_player = _seat_index;
		// 清一色和将一色开2次口提醒
		if (table.player_kai_kou[_seat_index] == 2) {
			boolean is_qing_yi_se = table.is_qing_yi_se(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
			boolean is_jiang_yi_se = table.is_jiang_yi_se(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
			if (is_qing_yi_se || is_jiang_yi_se)
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
						new long[] { GameConstants_MJ_XIAN_NING.WIK_CAI_GANG + 1 }, 1);
		}
		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._playerStatus[_seat_index].chi_hu_round_valid();

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[_seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
				if (table._logic.is_magic_card(cards[i])) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (cards[i] == Constants_EZ.HZ_CARD) {
					cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int count = 0;
		int ting_count = 0;

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
				}

				table.GRR._cards_index[_seat_index][i]++;
			}
		}

		table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

		if (ting_count > 0) {
			int tmp_cards[] = new int[GameConstants.MAX_COUNT];
			int tmp_hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[_seat_index], tmp_cards);
			int[] copy_tmp_cards = Arrays.copyOf(tmp_cards, GameConstants.MAX_COUNT);

			for (int i = 0; i < tmp_hand_card_count; i++) {
				for (int j = 0; j < ting_count; j++) {
					if (copy_tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
				if (table._logic.is_magic_card(copy_tmp_cards[i])) {
					tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (copy_tmp_cards[i] == Constants_EZ.HZ_CARD) {
					tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}

			table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
		} else {
			// 刷新手牌
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

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

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_OR_OUT_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
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

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
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
		case GameConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		}

		return true;
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

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[seat_index], hand_cards);

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

		if (_action != GameConstants.WIK_PENG && _type == GameConstants.CHI_TYPE_LAI_ZI_CHI) {
			// 癞子参与吃，播‘癞子吃’动画和音效
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_LAI_ZI_CHI }, 1,
					seat_index);
		} else {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}

	@Override
	public boolean handler_be_set_trustee(Table_XianNing table, int seat_index) {
		return false;
	}
}
