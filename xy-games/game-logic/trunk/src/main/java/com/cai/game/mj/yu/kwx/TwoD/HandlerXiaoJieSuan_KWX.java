package com.cai.game.mj.yu.kwx.TwoD;

import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;

public class HandlerXiaoJieSuan_KWX extends AbstractMJHandler<Table_KWX_2D> {
	@Override
	public void exe(Table_KWX_2D table) {
	}

	@Override
	public boolean handler_player_be_in_room(Table_KWX_2D table, int seat_index) {
		if (table._game_type_index == GameConstants_KWX.GAME_TYPE_KWX_2D) {
			table.handler_player_ready(seat_index, false);
			return true;
		}
		if (table._player_ready[seat_index] == 1) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_PLAYER_READY);
			roomResponse.setOperatePlayer(seat_index);
			table.send_response_to_player(seat_index, roomResponse);
			return true;
		}
		// RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		// roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);
		//
		// TableResponse.Builder tableResponse = TableResponse.newBuilder();
		//
		// table.load_room_info_data(roomResponse);
		// table.load_player_info_data(roomResponse);
		// table.load_common_status(roomResponse);
		//
		// tableResponse.setBankerPlayer(table.GRR._banker_player);
		// tableResponse.setCurrentPlayer(table._current_player);
		// tableResponse.setCellScore(0);
		//
		// tableResponse.setActionCard(0);
		//
		// tableResponse.setOutCardData(0);
		// tableResponse.setOutCardPlayer(0);
		//
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// tableResponse.addTrustee(false);
		// tableResponse.addDiscardCount(table.GRR._discard_count[i]);
		// Int32ArrayResponse.Builder int_array =
		// Int32ArrayResponse.newBuilder();
		// for (int j = 0; j < 55; j++) {
		// int real_card = table.GRR._discard_cards[i][j];
		// int_array.addItem(real_card);
		// }
		// tableResponse.addDiscardCards(int_array);
		//
		// tableResponse.addWeaveCount(table.GRR._weave_count[i]);
		// WeaveItemResponseArrayResponse.Builder weaveItem_array =
		// WeaveItemResponseArrayResponse.newBuilder();
		// for (int j = 0; j < GameConstants_KWX.MAX_WEAVE; j++) {
		// WeaveItemResponse.Builder weaveItem_item =
		// WeaveItemResponse.newBuilder();
		// weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player
		// + GameConstants_KWX.WEAVE_SHOW_DIRECT);
		// weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
		// weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
		// weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
		//
		// if (table.GRR._weave_items[i][j].weave_kind ==
		// GameConstants_KWX.WIK_GANG &&
		// table.GRR._weave_items[i][j].public_card == 0
		// && i != seat_index) {
		// weaveItem_item.setCenterCard(0);
		//
		// for (int x = 0; x < 4; x++) {
		// weaveItem_item.addWeaveCard(-1);
		// }
		// } else {
		// weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
		//
		// int[] weave_cards = new int[4];
		// int count =
		// table._logic.get_weave_card_huangshi(table.GRR._weave_items[i][j].weave_kind,
		// table.GRR._weave_items[i][j].center_card, weave_cards);
		// for (int x = 0; x < count; x++) {
		// if (table._logic.is_magic_card(weave_cards[x]))
		// weave_cards[x] += GameConstants_KWX.CARD_ESPECIAL_TYPE_LAI_ZI;
		//
		// weaveItem_item.addWeaveCard(weave_cards[x]);
		// }
		// }
		//
		// weaveItem_array.addWeaveItem(weaveItem_item);
		// }
		// tableResponse.addWeaveItemArray(weaveItem_array);
		//
		// tableResponse.addWinnerOrder(0);
		//
		// tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		// }
		//
		// tableResponse.setSendCardData(0);
		//
		// int hand_cards[] = new int[GameConstants_KWX.MAX_COUNT];
		// int hand_card_count =
		// table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index],
		// hand_cards);
		// for (int i = 0; i < hand_card_count; i++) {
		// tableResponse.addCardsData(hand_cards[i]);
		// }
		// roomResponse.setTable(tableResponse);
		//
		// table.send_response_to_player(seat_index, roomResponse);

		table.send_response_to_player(seat_index, table.saved_room_response);

		return true;
	}
}
