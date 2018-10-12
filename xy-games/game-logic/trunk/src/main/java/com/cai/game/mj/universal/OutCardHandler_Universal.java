package com.cai.game.mj.universal;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class OutCardHandler_Universal extends AbstractHandler_Universal<AbstractMahjongTable_Universal> {
	public void reset_status(int seat_index, int card, int type) {
		currentSeatIndex = seat_index;
		cardDataHandled = card;
		outCardType = type;
	}

	@Override
	public void exe(AbstractMahjongTable_Universal table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, UniversalConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._out_card_count++;
		table._out_card_player = currentSeatIndex;
		table._out_card_data = cardDataHandled;

		int next_player = table.get_next_seat(currentSeatIndex);
		int cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[currentSeatIndex], cards);
		table.operate_player_cards(currentSeatIndex, hand_card_count, cards, 0, null);

		table.operate_out_card(currentSeatIndex, 1, new int[] { cardDataHandled }, UniversalConstants.OUT_CARD_TYPE_MID, UniversalConstants.INVALID_SEAT);

		table._playerStatus[currentSeatIndex]._hu_card_count = table.get_ting_card(table._playerStatus[currentSeatIndex]._hu_cards,
				table.GRR._cards_index[currentSeatIndex], table.GRR._weave_items[currentSeatIndex], table.GRR._weave_count[currentSeatIndex],
				currentSeatIndex);

		int ting_cards[] = table._playerStatus[currentSeatIndex]._hu_cards;
		int ting_count = table._playerStatus[currentSeatIndex]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(currentSeatIndex, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(currentSeatIndex, 1, ting_cards);
		}

		table._provide_player = currentSeatIndex;
		table._provide_card = cardDataHandled;

		table.exe_add_discard(currentSeatIndex, 1, new int[] { cardDataHandled }, false, UniversalConstants.DELAY_SEND_CARD_DELAY);

		boolean bAroseAction = table.estimate_player_out_card_respond(currentSeatIndex, cardDataHandled, outCardType);

		if (bAroseAction == false) {
			table.exe_dispatch_card(next_player, UniversalConstants.WIK_NULL, UniversalConstants.DELAY_SEND_CARD_DELAY);
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table.change_player_status(i, UniversalConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean handleOperateCard(AbstractMahjongTable_Universal table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}

		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if ((operate_code != UniversalConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false) {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return true;
		}

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == UniversalConstants.WIK_NULL) {
			table.record_effect_action(seat_index, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { UniversalConstants.WIK_NULL }, 1);
		}

		if (table._playerStatus[seat_index].has_chi_hu() && operate_code != UniversalConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].chi_hu_round_invalid();
		}

		// 优先级判断，不通炮玩法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (currentSeatIndex + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (currentSeatIndex + p) % table.getTablePlayerNumber();

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

		operate_card = cardDataHandled;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == target_player)
				continue;
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case UniversalConstants.WIK_LEFT: {
			int cbRemoveCard[] = new int[] { operate_card + 1, operate_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(currentSeatIndex, cardDataHandled);

			table.exe_chi_peng(target_player, currentSeatIndex, target_action, operate_card, UniversalConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case UniversalConstants.WIK_RIGHT: {
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card - 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(currentSeatIndex, cardDataHandled);

			table.exe_chi_peng(target_player, currentSeatIndex, target_action, operate_card, UniversalConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case UniversalConstants.WIK_CENTER: {
			int cbRemoveCard[] = new int[] { operate_card - 1, operate_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(currentSeatIndex, cardDataHandled);

			table.exe_chi_peng(target_player, currentSeatIndex, target_action, operate_card, UniversalConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case UniversalConstants.WIK_PENG: {
			int cbRemoveCard[] = new int[] { operate_card, operate_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.remove_discard_after_operate(currentSeatIndex, cardDataHandled);

			table.exe_chi_peng(target_player, currentSeatIndex, target_action, operate_card, UniversalConstants.CHI_PENG_TYPE_OUT_CARD);

			return true;
		}
		case UniversalConstants.WIK_GANG: {
			table.remove_discard_after_operate(currentSeatIndex, cardDataHandled);

			table.exe_gang(target_player, currentSeatIndex, operate_card, target_action, UniversalConstants.GANG_TYPE_JIE_GANG, false, false);

			return true;
		}
		case UniversalConstants.WIK_NULL: {
			table._current_player = (currentSeatIndex + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

			table.exe_dispatch_card(table._current_player, UniversalConstants.WIK_NULL, 0);

			return true;
		}
		case UniversalConstants.WIK_CHI_HU: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != target_player) {
					table.GRR._chi_hu_rights[i].set_valid(false);
					table.GRR._chi_hu_rights[i].set_empty();
				}
			}

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table._cur_banker = target_player;

			table.set_niao_card(target_player);

			table.GRR._chi_hu_card[target_player][0] = cardDataHandled;
			table._player_result.jie_pao_count[target_player]++;
			table._player_result.dian_pao_count[currentSeatIndex]++;

			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score(target_player, currentSeatIndex, cardDataHandled, false);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, UniversalConstants.Game_End_NORMAL),
					UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handlePlayerBeInRoom(AbstractMahjongTable_Universal table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(currentSeatIndex);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		tableResponse.setSendCardData(0);

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
			for (int j = 0; j < UniversalConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);

				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);
			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		int hand_cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

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
}
