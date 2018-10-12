package com.cai.game.mj.henan.kfzz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.RandomUtil;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerHun_KFZZ extends AbstractMJHandler<MJTable_KFZZ> {
	protected int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable_KFZZ table) {
		_da_dian_card = table._repertory_card[RandomUtil.getRandomNumber(table._all_card_len - 1)];

		if (table.DEBUG_CARDS_MODE)
			_da_dian_card = 0x18;

		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			_da_dian_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_HUN },
				GameConstants.INVALID_SEAT);

		table._logic.add_magic_card_index(table._logic.switch_to_card_index(_da_dian_card));

		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_HUN;

		if ((table._game_status == GameConstants.GS_MJ_FREE || table._game_status == GameConstants.GS_MJ_WAIT) && table.is_sys())
			return;

		table.operate_show_card(table.GRR._banker_player, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			boolean has_lai_zi = false;
			for (int j = 0; j < GameConstants.MAX_INDEX; j++) {
				if (table.GRR._cards_index[i][j] > 0 && table._logic.is_magic_index(j)) {
					has_lai_zi = true;
					break;
				}
			}

			if (has_lai_zi) {
				int cards[] = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				for (int j = 0; j < hand_card_count; j++) {
					if (table._logic.is_magic_card(cards[j])) {
						cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
					}
				}

				table.operate_player_cards(i, hand_card_count, cards, 0, null);
			}
		}

		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i]._hu_card_count = table.get_henan_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
					table.GRR._weave_items[i], table.GRR._weave_count[i]);
			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}

		table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_KFZZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table.GRR._banker_player);
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
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
				} else {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}

			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if (table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)) {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card + GameConstants.CARD_ESPECIAL_TYPE_HUN);
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

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		for (int j = 0; j < hand_card_count; j++) {
			if (table._logic.is_magic_card(cards[j])) {
				cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HUN;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[_da_dian_card], seat_index);

		return true;
	}

}
