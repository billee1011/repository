package com.cai.game.mj.handler.yiyang.szg;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerQiShouBaoTing_YiYang_SZG extends AbstractMJHandler<MJTable_YiYang_SZG> {
	public GangCardResult m_gangCardResult = new GangCardResult();

	@Override
	public void exe(MJTable_YiYang_SZG table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_weave();
		}

		boolean bAroseAction = false;

		// 每个能听牌的闲家在客户端弹出‘报听’的界面
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table.GRR._banker_player) {
				continue;
			}

			if (table.qi_shou_ting[i]) { // 能听牌的才在客户端弹出‘报听’的界面
				table._playerStatus[i].add_action(GameConstants.WIK_BAO_TING);
				table._playerStatus[i].add_bao_ting(GameConstants.INVALID_VALUE, GameConstants.WIK_BAO_TING, i);

				bAroseAction = true;
			}
		}

		if (bAroseAction == false) {
			m_gangCardResult.cbCardCount = 0;
			int cbActionMask = table._logic.analyse_gang_by_card_hand_card(table.GRR._cards_index[table.GRR._banker_player],
					table.GRR._weave_items[table.GRR._banker_player], table.GRR._weave_count[table.GRR._banker_player], m_gangCardResult);

			if (cbActionMask != GameConstants.WIK_NULL) {
				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					table._playerStatus[table.GRR._banker_player].add_gang(m_gangCardResult.cbCardData[i], table.GRR._banker_player,
							m_gangCardResult.isPublic[i]);
					table._playerStatus[table.GRR._banker_player].add_action(GameConstants.WIK_GANG);
				}
			}

			if (table._playerStatus[table.GRR._banker_player].has_action()) {
				table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OPR_CARD);
				table.operate_player_action(table.GRR._banker_player, false);
			} else {
				table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		} else {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				PlayerStatus playerStatus = table._playerStatus[i];
				if (playerStatus.has_action()) {
					table.operate_player_action(i, false);
				}
			}
		}
	}

	@Override
	public boolean handler_operate_card(MJTable_YiYang_SZG table, int seat_index, int operate_code, int operate_card) {
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
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			if (table._playerStatus[seat_index].has_action_by_code(GameConstants.WIK_GANG)) {
				table._playerStatus[seat_index].clean_action();

				table.change_player_status(seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();

				return true;
			}
		}

		if (operate_code == GameConstants.WIK_BAO_TING) {
			// TODO 报听之后，相当于自动托管，摸什么牌打什么牌。直到胡牌
			table.is_bao_ting[seat_index] = true;

			table.operate_player_info();
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_action_by_code(GameConstants.WIK_BAO_TING)))
				return false;
		}

		if (operate_code == GameConstants.WIK_GANG) {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					table.exe_gang(seat_index, seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		m_gangCardResult.cbCardCount = 0;
		int cbActionMask = table._logic.analyse_gang_by_card_hand_card(table.GRR._cards_index[table.GRR._banker_player],
				table.GRR._weave_items[table.GRR._banker_player], table.GRR._weave_count[table.GRR._banker_player], m_gangCardResult);

		if (cbActionMask != GameConstants.WIK_NULL) {
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				table._playerStatus[table.GRR._banker_player].add_gang(m_gangCardResult.cbCardData[i], table.GRR._banker_player,
						m_gangCardResult.isPublic[i]);
				table._playerStatus[table.GRR._banker_player].add_action(GameConstants.WIK_GANG);
			}
		}

		if (table._playerStatus[table.GRR._banker_player].has_action()) {
			table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(table.GRR._banker_player, false);
		} else {
			table.change_player_status(table.GRR._banker_player, GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
		}

		return true;
	}

	@Override
	public boolean handler_player_out_card(MJTable_YiYang_SZG table, int seat_index, int card) {
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		if (seat_index != table.GRR._banker_player) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		if (table._logic.remove_card_by_index(table.GRR._cards_index[table.GRR._banker_player], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		table.exe_out_card(table.GRR._banker_player, card, GameConstants.WIK_NULL);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_YiYang_SZG table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.is_bao_ting[i]);
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

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		int out_ting_count = table._playerStatus[seat_index]._hu_out_card_count;

		if ((out_ting_count > 0) && (seat_index == table.GRR._banker_player)) {
			for (int j = 0; j < hand_card_count; j++) {
				for (int k = 0; k < out_ting_count; k++) {
					if (hand_cards[j] == table._playerStatus[seat_index]._hu_out_card_ting[k]) {
						hand_cards[j] = hand_cards[j] + GameConstants.CARD_ESPECIAL_TYPE_TING;
						break;
					}
				}
			}
		}

		for (int i = 0; i < hand_card_count; i++) {
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
		// TODO: 出任意一张牌时，能胡哪些牌 -- End

		// TODO 显示听牌数据
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
