package com.cai.game.mj.shanxi.hongdong;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_MJ_SXHD;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerChiPeng_ShanXi_HongDong extends MJHandlerChiPeng<MJTable_HongDong> {
	private GangCardResult m_gangCardResult;

	public MJHandlerChiPeng_ShanXi_HongDong() {
		m_gangCardResult = new GangCardResult();
	}

	@Override
	public void exe(MJTable_HongDong table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			// table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		if (table._logic.is_magic_card(_card)) {
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
		} else {
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		}
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;

		// 设置用户
		table._current_player = _seat_index;
		if (table._magic_peng_gang_color[_seat_index] != null && table._magic_peng_gang_color[_seat_index].getRight() == -1
				&& table._logic.get_card_color(_card) != 3) {
			table._magic_peng_gang_color[_seat_index].setRight(table._logic.get_card_color(_card));
		}
		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if (table.player_magic_card[_seat_index] == table.get_real_card(_card)) {
			table.player_magic_card_show_non[_seat_index] = true;
		}

		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];// table.GRR._weave_items[_seat_index];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
			// 癞子
			if (table._logic.is_magic_card(weaves[i].center_card)) {
				weaves[i].center_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}
		// 刷新手牌
		table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		// 回放
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (table._playerStatus[_seat_index]._card_status != GameConstants.CARD_STATUS_BAO_TING
				&& table._playerStatus[_seat_index]._card_status != Constants_MJ_SXHD.CARD_STATUS_YING_KOU) {
			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;

			int card_type_count = GameConstants.MAX_ZI_FENG;
			int default_ting = 0;
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DJWHFB)) {
				default_ting = 27;
				card_type_count = GameConstants.MAX_FENG;
			}
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB)) {
				default_ting = 31;
				card_type_count = GameConstants.MAX_ZI_FENG;
			}
			for (int i = default_ting; i < card_type_count; i++) {
				count = table.GRR._cards_index[_seat_index][i];

				if (count > 0) {
					table.GRR._cards_index[_seat_index][i]--;

					table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

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
				int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);
				for (int i = 0; i < tmp_hand_card_count; i++) {
					if (table._logic.is_magic_card(tmp_cards[i])) {
						tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					}
					for (int j = 0; j < ting_count; j++) {
						if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]
								|| tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
							tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_BAO_TING);
				if (0 != table.accpet_ying_kou(_seat_index, table._playerStatus[_seat_index]._hu_out_card_count,
						table._playerStatus[_seat_index]._hu_out_card_ting, table._playerStatus[_seat_index]._hu_out_card_ting_count,
						table._playerStatus[_seat_index]._hu_out_cards, new int[Constants_MJ_SXHD.HAND_CARD_MAX_COUNT])
						&& (ting_count != 1 || (ting_count == 1 && table._playerStatus[_seat_index]._hu_out_card_ting_count[0] != 1))) {
					table._playerStatus[_seat_index].add_action(Constants_MJ_SXHD.WIK_YING_KOU);
				}
			} else {
				// 刷新手牌
				table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
			}
		}

		m_gangCardResult.cbCardCount = 0;
		if (table.GRR._left_card_count > 0) {
			int cbActionMask = table.analyse_gang_exclude_magic_card(_seat_index, table.GRR._cards_index[_seat_index], _card,
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, false);
			if (cbActionMask != 0) {
				curPlayerStatus.add_action(GameConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}
		if (curPlayerStatus.has_action()) {
			// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);

		} else {
			// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(MJTable_HongDong table, int seat_index, int card) {
		card = table.get_real_card(card);
		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌--执行出牌切换状态
		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_HongDong table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		operate_card = table.get_real_card(operate_card);

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == table.get_real_card(m_gangCardResult.cbCardData[i])) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
			return true;
		}
		case Constants_MJ_SXHD.WIK_YING_KOU: //
		{
			if (operate_card == 0) {
				PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				curPlayerStatus.add_action(Constants_MJ_SXHD.WIK_YING_KOU);

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards_with_ting_ed(_seat_index, hand_card_count, cards, 0, null);
			} else {
				operate_card -= operate_card > GameConstants.CARD_ESPECIAL_TYPE_TING ? GameConstants.CARD_ESPECIAL_TYPE_TING : 0;
				if (table._logic.is_valid_card(operate_card) == false) {
					table.log_error("出牌,牌型出错");
					return false;
				}
				// 效验参数
				if (seat_index != _seat_index) {
					table.log_error("出牌,没到出牌");
					return false;
				}

				// 删除扑克
				if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}

				// 报听
				table.exe_out_card_bao_ting(_seat_index, operate_card, Constants_MJ_SXHD.CARD_STATUS_YING_KOU);
				return true;
			}
		}
		case GameConstants.WIK_BAO_TING: // 报听
		{
			operate_card -= operate_card > GameConstants.CARD_ESPECIAL_TYPE_TING ? GameConstants.CARD_ESPECIAL_TYPE_TING : 0;
			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
			int card = table.get_real_card(table._playerStatus[_seat_index]._hu_out_cards[0][0]);
			if (table._playerStatus[_seat_index]._hu_out_card_count == 1 && card >= GameConstants.HZ_MAGIC_CARD) {
				table._ying_kou[seat_index].setLeft(true);
			}

			// 报听
			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.CARD_STATUS_BAO_TING);
		}
			return true;

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_HongDong table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 色子
		if (table._cur_round == 1) {
			roomResponse.setEffectCount(4);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[2]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[3]);
		} else {
			roomResponse.setEffectCount(2);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
			roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);
		}

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int iCardIndex = table.GRR._discard_cards[i][j];
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					// int_array.addItem(table.GRR._discard_cards[i][j]);
				}
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					if (iCardIndex > GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI)
						iCardIndex -= GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
					iCardIndex += GameConstants.CARD_ESPECIAL_TYPE_BAO_TING;
				}
				int_array.addItem(iCardIndex);
			}

			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}

				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && !table.player_magic_card_show_non[i]) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 2);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		int h = hand_card_count;
		if (table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB) && table.player_magic_card[seat_index] != 0) {
			int liang_magic_count = 0;
			if (table.player_magic_card_show_non[seat_index]) {
				liang_magic_count = 2;
			}
			for (int j = 0; j < h; j++) {
				if (cards[j] != table.player_magic_card[seat_index]) {
					continue;
				}

				if (liang_magic_count == 2) {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else {
					cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
					liang_magic_count++;
					hand_card_count--;
				}
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber() && table.has_rule(Constants_MJ_SXHD.GAME_RULE_DWDJB); p++) {
			if (!table.player_magic_card_show_non[seat_index]) {
				tableResponse.addHuXi(table.player_magic_card[p]);
			}
		}
		for (int i = 0; i < h; i++) {
			if (cards[i] > GameConstants.CARD_ESPECIAL_TYPE_GUI && cards[i] < GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI) {
				continue;
			}
			tableResponse.addCardsData(cards[i]);
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
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0 && table._playerStatus[seat_index]._card_status != GameConstants.CARD_STATUS_BAO_TING
				&& table._playerStatus[seat_index]._card_status != Constants_MJ_SXHD.CARD_STATUS_YING_KOU == true) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
