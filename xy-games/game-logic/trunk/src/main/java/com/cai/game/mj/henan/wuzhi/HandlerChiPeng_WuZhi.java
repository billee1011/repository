package com.cai.game.mj.henan.wuzhi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.handler.MJHandlerChiPeng;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Wuzhi.IntegerArray;
import protobuf.clazz.mj.Wuzhi.LsdyCards;

public class HandlerChiPeng_WuZhi extends MJHandlerChiPeng<Table_WuZhi> {
	@Override
	public boolean handler_player_out_card(Table_WuZhi table, int seat_index, int card) {
		boolean is_from_bao_ting = card > GameConstants.CARD_ESPECIAL_TYPE_TING ? true : false;

		if (table.is_match() || table.isCoinRoom()) {
			table.chi_peng_index_invaild();
		}

		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table.hasLsdy && is_from_bao_ting) {
			if (table._logic.remove_card_by_index(table.lsdyCardsIndex[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		} else {
			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}
		}

		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	@Override
	public void exe(Table_WuZhi table) {
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
			weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

		table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (table.hasLsdy) {
			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			if (table.is_bao_ting[_seat_index] == false) {
				for (int i = 0; i < card_type_count; i++) {
					count = table.lsdyCardsIndex[_seat_index][i];

					if (count > 0) {
						table.lsdyCardsIndex[_seat_index][i]--;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, true,
								table.lsdyCardsIndex[_seat_index]);

						if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
							table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

							ting_count++;
						}

						table.lsdyCardsIndex[_seat_index][i]++;
					}
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				int tmp_cards[] = new int[GameConstants.MAX_COUNT];
				int tmp_hand_card_count = table._logic.switch_to_cards_data(table.lsdyCardsIndex[_seat_index], tmp_cards);

				for (int i = 0; i < tmp_hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				table.operate_player_cards_with_ting_lsdy(_seat_index, hand_card_count, cards, tmp_hand_card_count, tmp_cards, weave_count, weaves);

				if (table.is_bao_ting[_seat_index] == false) {
					// 能报听了
					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			} else {
				// 刷新手牌
				table.operate_player_cards_lsdy(_seat_index, hand_card_count, cards, weave_count, weaves);
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		} else {
			// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
			int count = 0;
			int ting_count = 0;

			int card_type_count = GameConstants.MAX_ZI_FENG;

			if (table.is_bao_ting[_seat_index] == false) {
				for (int i = 0; i < card_type_count; i++) {
					count = table.GRR._cards_index[_seat_index][i];

					if (count > 0) {
						table.GRR._cards_index[_seat_index][i]--;

						table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] = table.get_ting_card(
								table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
								table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index, true, null);

						if (table._playerStatus[_seat_index]._hu_out_card_ting_count[ting_count] > 0) {
							table._playerStatus[_seat_index]._hu_out_card_ting[ting_count] = table._logic.switch_to_card_data(i);

							ting_count++;
						}

						table.GRR._cards_index[_seat_index][i]++;
					}
				}
			}

			table._playerStatus[_seat_index]._hu_out_card_count = ting_count;

			if (ting_count > 0) {
				int tmp_cards[] = new int[GameConstants.MAX_COUNT];
				int tmp_hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], tmp_cards);

				for (int i = 0; i < tmp_hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (tmp_cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
						if (tmp_cards[i] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
							if (table._logic.is_magic_card(tmp_cards[i])) {
								tmp_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							}
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, tmp_hand_card_count, tmp_cards, weave_count, weaves);

				if (table.is_bao_ting[_seat_index] == false) {
					// 能报听了
					curPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
				}
			} else {
				// 刷新手牌
				table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		}

		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}
	}

	@Override
	public boolean handler_operate_card(Table_WuZhi table, int seat_index, int operate_code, int operate_card) {
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

			// 有听点了过，刷新一下手牌
			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
				WeaveItem weaves[] = new WeaveItem[GameConstants.MAX_WEAVE];
				int weave_count = table.GRR._weave_count[_seat_index];
				for (int i = 0; i < weave_count; i++) {
					weaves[i] = new WeaveItem();
					weaves[i].weave_kind = table.GRR._weave_items[_seat_index][i].weave_kind;
					weaves[i].center_card = table.GRR._weave_items[_seat_index][i].center_card;
					weaves[i].public_card = table.GRR._weave_items[_seat_index][i].public_card;
					weaves[i].provide_player = table.GRR._weave_items[_seat_index][i].provide_player + GameConstants.WEAVE_SHOW_DIRECT;
				}

				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				if (table.hasLsdy) {
					table.operate_player_cards_lsdy(_seat_index, hand_card_count, cards, weave_count, weaves);
				} else {
					table.operate_player_cards(_seat_index, hand_card_count, cards, weave_count, weaves);
				}
			}

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();

			return true;
		}

		switch (operate_code) {
		case GameConstants.WIK_BAO_TING: {
			operate_card = table.get_real_card(operate_card);

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			if (seat_index != _seat_index) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			if (table.hasLsdy) {
				if (table._logic.remove_card_by_index(table.lsdyCardsIndex[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}
			} else {
				if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
					table.log_error("出牌删除出错");
					return false;
				}
			}

			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);

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
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		int lsdy_cards[] = new int[GameConstants.MAX_COUNT];
		int tmpCount = 0;
		if (table.hasLsdy)
			tmpCount = table._logic.switch_to_cards_data(table.lsdyCardsIndex[seat_index], lsdy_cards);

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			if (table.hasLsdy) {
				for (int j = 0; j < tmpCount; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (lsdy_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							lsdy_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}
			} else {
				for (int j = 0; j < hand_card_count; j++) {
					for (int k = 0; k < out_ting_count; k++) {
						if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
							hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
						if (hand_cards[j] < GameConstants.CARD_ESPECIAL_TYPE_TING) {
							if (table._logic.is_magic_card(hand_cards[j])) {
								hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
							}
						}
					}
				}
			}
		} else {
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

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

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards, table.ziMoCardsData[seat_index]);
		}

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
