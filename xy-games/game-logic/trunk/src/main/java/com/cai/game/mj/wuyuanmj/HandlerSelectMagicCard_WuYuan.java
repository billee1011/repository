package com.cai.game.mj.wuyuanmj;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerSelectMagicCard_WuYuan extends AbstractMJHandler<Table_WuYuan> {
	@Override
	public void exe(Table_WuYuan table) {
		table._logic.clean_magic_cards();
		table._send_card_count++;
		table.joker_card_1 = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE || table.BACK_DEBUG_CARDS_MODE) {
			table.joker_card_1 = 0x29;
		}
		if (table.DEBUG_MAGIC_CARD) {
			table.DEBUG_MAGIC_CARD = false;
		}

		table.operate_show_card(table._cur_banker, GameConstants.Show_Card_Center, 1, new int[] { table.joker_card_1 }, GameConstants.INVALID_SEAT);

		table.joker_card_index_1 = table._logic.switch_to_card_index(table.joker_card_1);
		table._logic.add_magic_card_index(table.joker_card_index_1);

		table.GRR._especial_card_count = 1;

		table.GRR._especial_show_cards[0] = table.joker_card_1 + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);

			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				}
			}

			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.check_bao_pai(i, table.GRR._cards_index[i]);
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				table.operate_show_card(table._cur_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
				table.exe_dispatch_card(table._cur_banker, GameConstants.WIK_NULL, 0);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i);
					if (table._playerStatus[i]._hu_card_count > 0) {
						table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
					}
				}
			}
		}, 1, TimeUnit.SECONDS);

	}

	@Override
	public boolean handler_player_be_in_room(Table_WuYuan table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._cur_banker);
		tableResponse.setCellScore(0);
		tableResponse.setActionCard(0);
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);

			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if (table.GRR._discard_cards[i][j] == table.joker_card_1) {
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

			if (i == table._cur_banker) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 显示听牌数据
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 比吃碰的断线重连多了一个客户端显示
		table.operate_show_card(table._cur_banker, GameConstants.Show_Card_Center, 1, new int[] { table.joker_card_1 }, seat_index);

		return true;
	}
}
