package com.cai.game.mj.yu.gy;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_GY;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerYingBao extends AbstractMJHandler<Table_GY> {

	private int _type;
	private GangCardResult m_gangCardResult;

	public void reset(int type, GangCardResult m_gangCardResult) {
		_type = type;
		this.m_gangCardResult = m_gangCardResult;
	}

	@Override
	public void exe(Table_GY table) {
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (p == table._current_player) {
				continue;
			}
			table.change_player_status(p, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(p, false);
		}
	}

	@Override
	public boolean handler_operate_card(Table_GY table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("没有这个操作");
			return false;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		playerStatus.clean_action();
		table.operate_player_action(seat_index, true);

		switch (operate_code) {
		case GameConstants.WIK_GANG: {
			table.player_mo_first[table._current_player] = false;
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					if (m_gangCardResult.cbCardData[i] != table._send_card_data) {
						table.exe_gang(table._current_player, table._current_player, operate_card, operate_code, m_gangCardResult.type[i], true,
								false);
					} else {
						table.exe_gang(table._current_player, table._current_player, operate_card, operate_code, m_gangCardResult.type[i], true,
								true);
					}
					return true;
				}
			}

		}
		case GameConstants.WIK_ZI_MO: {

			table.GRR._chi_hu_rights[table._current_player].set_valid(true);

			table.GRR._chi_hu_rights[table._current_player].opr_or(GameConstants_GY.CHR_TIAN_HU);
			table._cur_banker = table._current_player;

			table.GRR._chi_hu_card[table._current_player][0] = operate_card;

			table.GRR._win_order[table._current_player] = 1;

			table.exe_select_magic();
			table.process_ji_fen();
			table.process_reponsibility_ji_fen();

			// 将胡的牌加入鸡牌中
			if (table.is_ji_card(operate_card))
				table.out_ji_pai[table._current_player][table.out_ji_pai_count[table._current_player]++] = operate_card;

			table.process_chi_hu_player_operate(table._current_player, operate_card, true);
			table.process_chi_hu_player_score(table._current_player, table._current_player, operate_card, true);

			table._player_result.zi_mo_count[table._current_player]++;

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._current_player, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants_GY.WIK_YING_BAO_XIAN: // 报听
		{
			table.player_ying_bao[seat_index] = true;
			table._playerStatus[seat_index].set_card_status(GameConstants.CARD_STATUS_BAO_TING);

			// 效果
			table.operate_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_BAO_TING }, 1,
					GameConstants.INVALID_SEAT);

			table.operate_player_status();
			break;
		}
		case GameConstants.WIK_BAO_TING: // 报听
		{
			table.player_ying_bao[seat_index] = true;
			operate_card -= GameConstants.CARD_ESPECIAL_TYPE_TING;

			if (table._out_card_count == 0) {// 起手报听
				ChiHuRight chr = table.GRR._chi_hu_rights[table._current_player];
				chr.qi_shou_bao_ting = GameConstants.CHR_HUNAN_QISHOU_BAO_TING;
			}

			if (table._logic.is_valid_card(operate_card) == false) {
				table.log_error("出牌,牌型出错");
				return false;
			}

			// 效验参数
			if (seat_index != table._current_player) {
				table.log_error("出牌,没到出牌");
				return false;
			}

			// 删除扑克
			if (table._logic.remove_card_by_index(table.GRR._cards_index[table._current_player], operate_card) == false) {
				table.log_error("出牌删除出错");
				return false;
			}

			// 报听
			table.exe_out_card_bao_ting(table._current_player, operate_card,
					_type == GameConstants.DispatchCard_Type_Tian_Hu ? GameConstants.OutCard_Type_Di_Hu : GameConstants.WIK_NULL);
			return true;
		}

		}
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			if (p == table._current_player) {
				continue;
			}
			if (table._playerStatus[p].has_action_by_code(GameConstants_GY.WIK_YING_BAO_XIAN)) {
				return true;
			}
		}

		if (table._playerStatus[table._current_player].has_action()) {
			table.change_player_status(table._current_player, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(table._current_player, false);
		} else {
			table.player_mo_first[table._current_player] = false;
			table.change_player_status(table._current_player, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(Table_GY table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != table._current_player) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[table._current_player], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		if (_type == GameConstants_GY.DispatchCard_Type_Tian_Hu) {
			// 出牌
			table.exe_out_card(table._current_player, card, GameConstants_GY.OutCard_Type_Di_Hu);
		} else {
			// 出牌
			table.exe_out_card(table._current_player, card, _type);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_GY table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setCellScore(0);

		tableResponse.setActionCard(0);

		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
					// 癞子
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			if (i == table._current_player) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards, seat_index);

		if (seat_index == table._current_player) {
			if (table.player_duan[table._current_player] != -1) {
				if (table.player_duan[table._current_player] == table._logic.get_card_color(table._send_card_data)) {
					table._logic.remove_card_by_data(hand_cards, table._send_card_data);
				} else {
					boolean add_flag = false;
					for (int i = 0; i < GameConstants.MAX_INDEX; i++) {
						if (table.GRR._cards_index[table._current_player][i] > 0
								&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[table._current_player]) {
							add_flag = true;
							break;
						}
					}

					if (add_flag) {
						table._logic.remove_card_by_data(hand_cards, table._send_card_data + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG);
					} else {
						table._logic.remove_card_by_data(hand_cards, table._send_card_data);
					}
				}
			} else {
				table._logic.remove_card_by_data(hand_cards, table._send_card_data);
			}
		}

		// TODO: 出任意一张牌时，能胡哪些牌 -- Begin
		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == table._current_player)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_TING;
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
			roomResponse.addOutCardTing(table._playerStatus[seat_index]._hu_out_card_ting[i] + GameConstants.CARD_ESPECIAL_TYPE_TING);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < ting_card_cout; j++) {
				int_array.addItem(table._playerStatus[seat_index]._hu_out_cards[i][j]);
				roomResponse.addDouliuzi(table._playerStatus[seat_index]._hu_out_cards_fan[i][j]);
			}
			roomResponse.addOutCardTingCards(int_array);
		}

		table.send_response_to_player(seat_index, roomResponse);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		int show_send_card = table._send_card_data;
		if (table._logic.is_magic_card(table._send_card_data)) {
			show_send_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		}

		boolean hand_have_duan_card = false;
		if (table.player_duan[seat_index] != -1) {
			if (seat_index == table._current_player
					&& table.player_duan[table._current_player] == table._logic.get_card_color(table._send_card_data)) {
				hand_have_duan_card = true;
			} else {
				for (int i = 0; i < GameConstants.MAX_INDEX; i++)
					if (table.GRR._cards_index[seat_index][i] > 0
							&& table._logic.get_card_color(table._logic.switch_to_card_data(i)) == table.player_duan[seat_index]) {
						hand_have_duan_card = true;
						break;
					}
			}
		}
		if (hand_have_duan_card) {
			if (table.player_duan[table._current_player] == table._logic.get_card_color(table._send_card_data)) {
				table.operate_player_get_card(table._current_player, 1, new int[] { table._send_card_data }, seat_index);
			} else {
				table.operate_player_get_card(table._current_player, 1,
						new int[] { show_send_card + GameConstants_GY.CARD_ESPECIAL_TYPE_LIANG_ZHANG }, seat_index);
			}
		} else {
			table.operate_player_get_card(table._current_player, 1, new int[] { show_send_card }, seat_index);
		}

		if (seat_index != table._current_player && table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_BAO_TING)) {
			table.operate_player_action(seat_index, false);
			return true;
		}
		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
