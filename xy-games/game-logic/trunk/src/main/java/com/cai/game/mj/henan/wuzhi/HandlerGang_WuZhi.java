package com.cai.game.mj.henan.wuzhi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.handler.MJHandlerGang;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Wuzhi.IntegerArray;
import protobuf.clazz.mj.Wuzhi.LsdyCards;

public class HandlerGang_WuZhi extends MJHandlerGang<Table_WuZhi> {
	public int providePlayer = -1;

	@Override
	public void exe(Table_WuZhi table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			providePlayer = _seat_index;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			providePlayer = _provide_player;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			providePlayer = _seat_index;
		}

		if (_type == GameConstants.GANG_TYPE_AN_GANG) {
			exe_gang(table);
			return;
		}

		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];

				if (table.is_bao_ting[i]) {
					if (playerStatus.has_chi_hu()) {
						table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, _center_card);
					}
				} else {
					table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	protected boolean exe_gang(Table_WuZhi table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				WeaveItem weave = table.GRR._weave_items[_seat_index][i];
				int cbWeaveKind = weave.weave_kind;
				int cbCenterCard = weave.center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;

		// TODO: 回头杠时，提供者不更新，暗杠明杠才更新
		if (GameConstants.GANG_TYPE_ADD_GANG != _type) {
			table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		}

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;

		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
		int weave_count = table.GRR._weave_count[_seat_index];
		for (int i = 0; i < weave_count; i++) {
			weaves[i] = new WeaveItem();
			weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
			weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
			weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (table.hasLsdy) {
			table.lsdyCardsIndex[_seat_index][cbCardIndex] = 0;
			table.operate_player_cards_lsdy(_seat_index, hand_card_count, cards, weave_count, weaves);
		} else {
			table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
		}

		if (table.is_bao_ting[_seat_index]) {
			if (table.hasLsdy) {
				table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards,
						table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
						true, table.lsdyCardsIndex[_seat_index]);
			} else {
				table._playerStatus[_seat_index]._hu_card_count = table.get_ting_card(table._playerStatus[_seat_index]._hu_cards,
						table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
						true, null);
			}

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards, table.ziMoCardsData[_seat_index]);
			}
		}

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			table._player_result.an_gang_count[_seat_index]++;
			table.GRR._gang_score[_seat_index].an_gang_count++;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			table._player_result.ming_gang_count[_seat_index]++;
			table.GRR._gang_score[_seat_index].ming_gang_count++;
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			table._player_result.ming_gang_count[_seat_index]++;
			table.GRR._gang_score[_seat_index].ming_gang_count++;
		}

		table.exe_dispatch_card(_seat_index, _type, 0);

		return true;
	}

	@Override
	public boolean handler_operate_card(Table_WuZhi table, int seat_index, int operate_code, int operate_card) {
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
			table.record_discard_gang(seat_index);
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

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

		int target_card = _center_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			exe_gang(table);

			return true;
		}
		case GameConstants.WIK_CHI_HU: {
			table.hasWin = true;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}

			table.GRR._chi_hu_card[target_player][0] = target_card;
			table.alreadyWinCardsSet[target_player].add(operate_card);

			table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { GameConstants.WIK_CHI_HU }, 1,
					GameConstants.INVALID_SEAT);

			if (providePlayer == table.get_banker_pre_seat(target_player)) {
				// 上家提供
				table.winCardsData[target_player].get(0).add(target_card);
			} else if (providePlayer == table.get_banker_next_seat(target_player)) {
				// 下家提供
				table.winCardsData[target_player].get(2).add(target_card);
			} else {
				// 对家提供
				table.winCardsData[target_player].get(1).add(target_card);
			}

			table.process_chi_hu_player_score(target_player, providePlayer, _center_card, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[providePlayer]++;

			table.totalJiePao[target_player]++;
			table.totalFangPao[providePlayer]++;

			table.jiePaoCount[target_player]++;
			table.fangPaoCount[providePlayer]++;

			exe_gang(table);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_WuZhi table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
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
			tableResponse.addTrustee(false);
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
				if (table.GRR._weave_items[i][j].public_card == 0 && i != seat_index) {
					weaveItem_item.setCenterCard(-1);
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		int lsdy_cards[] = new int[GameConstants.MAX_COUNT];
		int tmpCount = 0;
		if (table.hasLsdy)
			tmpCount = table._logic.switch_to_cards_data(table.lsdyCardsIndex[seat_index], lsdy_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		if (table.hasLsdy) {
			LsdyCards.Builder lsdyCardsBuilder = LsdyCards.newBuilder();
			int all_lsdy_cards[][] = new int[table.getTablePlayerNumber()][GameConstants.MAX_COUNT];
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int newTmpCount = table._logic.switch_to_cards_data(table.lsdyCardsIndex[i], all_lsdy_cards[i]);
				lsdyCardsBuilder.addCardsCount(newTmpCount);

				IntegerArray.Builder cards = IntegerArray.newBuilder();
				if (i == seat_index) {
					for (int x = 0; x < tmpCount; x++) {
						cards.addCard(lsdy_cards[x]);
					}
				} else {
					for (int x = 0; x < newTmpCount; x++) {
						cards.addCard(all_lsdy_cards[i][x]);
					}
				}

				lsdyCardsBuilder.addCardsData(cards);
			}

			roomResponse.setCommResponse(PBUtil.toByteString(lsdyCardsBuilder));
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		if (table.GRR._chi_hu_rights[seat_index].is_valid()) {
			table.process_chi_hu_player_operate_reconnect(seat_index, _center_card, false); // 效果
		} else {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards, table.ziMoCardsData[seat_index]);
			}

			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
