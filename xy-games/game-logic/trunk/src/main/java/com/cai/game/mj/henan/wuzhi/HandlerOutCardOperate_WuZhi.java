package com.cai.game.mj.henan.wuzhi;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PBUtil;
import com.cai.game.mj.handler.MJHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.mj.Wuzhi.IntegerArray;
import protobuf.clazz.mj.Wuzhi.LsdyCards;

public class HandlerOutCardOperate_WuZhi extends MJHandlerOutCardOperate<Table_WuZhi> {
	public int next_player;

	@Override
	public void exe(Table_WuZhi table) {
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];

		table.change_player_status(_out_card_player, GameConstants.INVALID_VALUE);
		playerStatus.clean_action();

		table._out_card_count++;
		table._out_card_player = _out_card_player;
		table._out_card_data = _out_card_data;

		next_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		// table._current_player = next_player;

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_out_card_player], cards);

		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(cards[i])) {
				cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		if (table.hasLsdy) {
			table.operate_player_cards_lsdy(_out_card_player, hand_card_count, cards, 0, null);
		} else {
			table.operate_player_cards(_out_card_player, hand_card_count, cards, 0, null);
		}

		table.operate_out_card(_out_card_player, 1, new int[] { _out_card_data }, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);

		int ting_cards[] = table._playerStatus[_out_card_player]._hu_cards;
		int ting_count_new = table._playerStatus[_out_card_player]._hu_card_count;

		if (ting_count_new > 0 && table.is_bao_ting[_out_card_player]) {
			table.operate_chi_hu_cards(_out_card_player, ting_count_new, ting_cards, table.ziMoCardsData[_out_card_player]);
		}

		table._provide_player = _out_card_player;
		table._provide_card = _out_card_data;

		table.exe_add_discard(_out_card_player, 1, new int[] { _out_card_data }, false, GameConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = false;
		if (!table._logic.is_magic_card(_out_card_data)) {
			if (table.hasLsdy) {
				bAroseAction = table.estimate_player_out_card_respond_lsdy(_out_card_player, _out_card_data, _type);
			} else {
				bAroseAction = table.estimate_player_out_card_respond(_out_card_player, _out_card_data, _type);
			}
		}

		if (bAroseAction == false) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table.change_player_status(i, GameConstants.INVALID_VALUE);
			}

			table.operate_player_action(_out_card_player, true);

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];

				if (playerStatus.has_action()) {
					if (table.is_bao_ting[i]) {
						if (playerStatus.has_chi_hu()) {
							table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, _out_card_data);
						} else if (playerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
							table.operate_player_action(i, false);
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						} else {
							table.operate_player_action(i, true);
							table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _out_card_data);
						}
					} else {
						table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
						table.operate_player_action(i, false);
					}
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_WuZhi table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		if (playerStatus.is_respone() == false) { // 如果已经相应了，不能重复设置操作状态
			playerStatus.operate(operate_code, operate_card);
		}
		playerStatus.clean_status();

		if (GameConstants.WIK_NULL == operate_code) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != GameConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();

			// 清空CHR
			table.GRR._chi_hu_rights[seat_index].set_empty();
		}

		// 下面的代码直接获取优先级最高的人进行操作，'胡'>'碰'/'杠'>'吃'，而且所有玩家按逆时针顺序判断相同的操作
		// A、B两人能同时胡，但是A是B的上家，那么就只能A胡牌，注意和一炮多响的不同之处
		// TODO: 暂时没看懂这个逻辑是怎么来的
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_out_card_player + p) % table.getTablePlayerNumber();

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

		// 如果优先级最高的人还没有操作
		// 注意一些不同的地方，一些小细节的东西，整个handler里的代码，对逻辑数据的处理是相当混乱的，一不小心就会踩bug
		// 玩家状态的operate方法会更新是否已相应，那么在本方法的前面几行判断代码就要相应的做调整
		if (table._playerStatus[target_player].is_respone() == false)
			return true;

		int target_card = operate_card = _out_card_data;

		// 如果target_action是胡牌动作，只清空所有人的胡
		if (target_action == GameConstants.WIK_CHI_HU) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action(GameConstants.WIK_CHI_HU);
			}
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
				table.operate_player_action(i, true);
			}
		}

		// 注意下面几个不同之处，‘吃’牌或者‘碰’牌的时候，在exe方法之前处理手牌；‘杠’牌的时候，进入handler的exe_gang方法才会处理手牌
		// 这个不同点，对有些处理器的一小部分代码会有影响，处理时要注意
		switch (target_action) {
		case GameConstants.WIK_LEFT: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
			if (table.hasLsdy) {
				if (!table._logic.remove_cards_by_index_lsdy(table.GRR._cards_index[target_player], table.lsdyCardsIndex[target_player], cbRemoveCard,
						2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			} else {
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_RIGHT: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };
			if (table.hasLsdy) {
				if (!table._logic.remove_cards_by_index_lsdy(table.GRR._cards_index[target_player], table.lsdyCardsIndex[target_player], cbRemoveCard,
						2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			} else {
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_CENTER: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
			if (table.hasLsdy) {
				if (!table._logic.remove_cards_by_index_lsdy(table.GRR._cards_index[target_player], table.lsdyCardsIndex[target_player], cbRemoveCard,
						2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			} else {
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_PENG: {
			// 删除扑克
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (table.hasLsdy) {
				if (!table._logic.remove_cards_by_index_lsdy(table.GRR._cards_index[target_player], table.lsdyCardsIndex[target_player], cbRemoveCard,
						2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			} else {
				if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
					table.log_player_error(seat_index, "吃牌删除出错");
					return false;
				}
			}
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			table.exe_chi_peng(target_player, _out_card_player, target_action, operate_card, GameConstants.CHI_PENG_TYPE_OUT_CARD);
		}
			break;
		case GameConstants.WIK_GANG: {
			table.remove_discard_after_operate(_out_card_player, _out_card_data);
			// 是否有抢杠胡
			table.exe_gang(target_player, _out_card_player, operate_card, target_action, GameConstants.GANG_TYPE_JIE_GANG, false, false);
			return true;
		}
		case GameConstants.WIK_NULL: {
			// 用户切换
			_current_player = table._current_player = (_out_card_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			// 发牌
			table.exe_dispatch_card(_current_player, GameConstants.WIK_NULL, 0);

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

			if (_out_card_player == table.get_banker_pre_seat(target_player)) {
				// 上家提供
				table.winCardsData[target_player].get(0).add(target_card);
			} else if (_out_card_player == table.get_banker_next_seat(target_player)) {
				// 下家提供
				table.winCardsData[target_player].get(2).add(target_card);
			} else {
				// 对家提供
				table.winCardsData[target_player].get(1).add(target_card);
			}

			table.process_chi_hu_player_score(target_player, _out_card_player, target_card, false);

			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[_out_card_player]++;

			table.totalJiePao[target_player]++;
			table.totalFangPao[_out_card_player]++;

			table.jiePaoCount[target_player]++;
			table.fangPaoCount[_out_card_player]++;

			boolean hasAction = false;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					hasAction = true;
					break;
				}
			}

			if (hasAction) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					playerStatus = table._playerStatus[i];

					if (playerStatus.has_action()) {
						playerStatus._response = false;

						if (table.is_bao_ting[i]) {
							if (playerStatus.has_chi_hu()) {
								table.exe_jian_pao_hu(i, GameConstants.WIK_CHI_HU, _out_card_data);
							} else if (playerStatus.has_action_by_code(GameConstants.WIK_GANG)) {
								table.operate_player_action(i, false);
								table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							} else {
								table.operate_player_action(i, true);
								table.exe_jian_pao_hu(i, GameConstants.WIK_NULL, _out_card_data);
							}
						} else {
							table.change_player_status(i, GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(i, false);
						}
					}
				}
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._playerStatus[i].clean_action();
					table.change_player_status(i, GameConstants.INVALID_VALUE);
				}

				table.operate_player_action(_out_card_player, true);

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}

			return true;
		}
		default:
			return false;
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
		tableResponse.setCurrentPlayer(_out_card_player);
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
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
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
			table.process_chi_hu_player_operate_reconnect(seat_index, _out_card_data, false);
		} else {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards, table.ziMoCardsData[seat_index]);
			}

			if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
				table.operate_player_action(seat_index, false);
			}
		}

		return true;
	}
}
