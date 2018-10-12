package com.cai.game.mj.shanxi.jixian;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.UniversalConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.handler.MJHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandleDispatchCard_JiXian extends MJHandlerDispatchCard<Table_JiXian> {
	boolean ting_send_card = false;

	protected int _seat_index;
	protected int _send_card_data;
	protected int _type;
	protected GangCardResult m_gangCardResult;

	public HandleDispatchCard_JiXian() {
		m_gangCardResult = new GangCardResult();
	}

	public void reset_status(int seat_index, int type) {
		_seat_index = seat_index;
		_type = type;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_JiXian table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, UniversalConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count <= 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = UniversalConstants.INVALID_VALUE;
			}

			// 流局庄不变
			table._cur_banker = table.GRR._banker_player;

			// 流局
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._cur_banker, UniversalConstants.Game_End_DRAW),
					UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return;
		}

		PlayerStatus currentPlayerStatus = table._playerStatus[_seat_index];
		currentPlayerStatus.reset();

		++table._send_card_count;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x25;
		}

		table.zhua_pai_count[_seat_index]++;

		table._send_card_data = _send_card_data;
		table._current_player = _seat_index;
		table._provide_player = _seat_index;
		table._last_dispatch_player = _seat_index;
		table._provide_card = _send_card_data;

		if (table.is_bao_ting[_seat_index] || (table.zhua_pai_count[_seat_index] == 1 && table.GRR._weave_count[_seat_index] == 0)) {
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();

			table.analyse_state = table.ANALYSE_NORMAL;
			int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], _send_card_data, chr, UniversalConstants.U_HU_CARD_TYPE_ZI_MO, _seat_index);

			if (UniversalConstants.WIK_NULL != action) {
				currentPlayerStatus.add_action(UniversalConstants.WIK_ZI_MO);
				currentPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			} else {
				chr.set_empty();
			}
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		if (table.is_bao_ting[_seat_index] == false) {
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
							table._playerStatus[_seat_index]._hu_out_cards[ting_count], table.GRR._cards_index[_seat_index],
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index);

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
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.GRR._cards_index[_seat_index][send_card_index]++;

				for (int i = 0; i < hand_card_count; i++) {
					for (int j = 0; j < ting_count; j++) {
						if (cards[i] == table._playerStatus[_seat_index]._hu_out_card_ting[j]) {
							cards[i] += GameConstants.CARD_ESPECIAL_TYPE_TING;
							break;
						}
					}
				}

				table.operate_player_cards_with_ting(_seat_index, hand_card_count, cards, 0, null);

				currentPlayerStatus.add_action(GameConstants.WIK_BAO_TING);
			}
			// TODO: 出任意一张牌时，能胡哪些牌 -- End
		}

		int real_card = _send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		if (_type == UniversalConstants.DISPATCH_CARD_TYPE_GANG) {
			table.operate_player_get_card_gang(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
			table.gang_dispatch_count++;
		} else {
			table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);
		}

		if (table.GRR._left_card_count > 0 && !table.is_bao_ting[_seat_index]) {
			m_gangCardResult.cbCardCount = 0;

			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, false);

			if (0 != cbActionMask) {
				currentPlayerStatus.add_action(UniversalConstants.WIK_GANG);
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					currentPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				}
			}
		}

		if (currentPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, UniversalConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, UniversalConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}
	}

	@Override
	public boolean handler_operate_card(Table_JiXian table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != UniversalConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
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

		if (operate_code == UniversalConstants.WIK_NULL) {
			table.record_effect_action(seat_index, UniversalConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { UniversalConstants.WIK_NULL }, 1);

			playerStatus.clean_action();
			playerStatus.clean_status();

			if (table.is_bao_ting[_seat_index]) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), 1000, TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		switch (operate_code) {
		case UniversalConstants.WIK_GANG: {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}
			break;
		case UniversalConstants.WIK_ZI_MO: {
			table.GRR._chi_hu_rights[seat_index].set_valid(true);

			table._cur_banker = seat_index;

			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._player_result.zi_mo_count[seat_index]++;

			table.set_niao_card(seat_index);

			table.process_chi_hu_player_operate(seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, UniversalConstants.Game_End_NORMAL),
					UniversalConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
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

			if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			table.exe_out_card_bao_ting(_seat_index, operate_card, GameConstants.WIK_NULL);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_JiXian table, int seat_index, int card) {
		card = table.get_real_card(card);

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

		if (_type == UniversalConstants.DISPATCH_CARD_TYPE_GANG) {
			table.exe_out_card(_seat_index, card, UniversalConstants.DISPATCH_CARD_TYPE_GANG);
		} else {
			table.exe_out_card(_seat_index, card, UniversalConstants.WIK_NULL);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_JiXian table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		roomResponse.setIsGoldRoom(table.is_sys());

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
		tableResponse.setSendCardData(0);

		// 杠之后，发的牌的张数
		roomResponse.setPageSize(table.gang_dispatch_count);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();

			for (int j = 0; j < 55; j++) {
				if (j == table.GRR._chi_hu_rights[i].bao_ting_index) {
					int_array.addItem(GameConstants.BLACK_CARD);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
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

			if (i == _seat_index) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		int hand_cards[] = new int[UniversalConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		if (seat_index == _seat_index) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == _seat_index)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		roomResponse.setOutCardCount(out_ting_count);

		for (int i = 0; i < out_ting_count; i++) {
			int ting_card_cout = table._playerStatus[seat_index]._hu_out_card_ting_count[i];
			roomResponse.addOutCardTingCount(ting_card_cout);

			int tmp_card = table._playerStatus[seat_index]._hu_out_card_ting[i];
			roomResponse.addOutCardTing(tmp_card + GameConstants.CARD_ESPECIAL_TYPE_TING);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		int real_card = table._send_card_data;
		if (ting_send_card) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_TING;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { real_card }, seat_index);
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		if (table.is_bao_ting[seat_index]) {
			int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
			int ting_count = table._playerStatus[seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
			}
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
