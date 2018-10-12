package com.cai.game.mj.handler.henanxc;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.RevomeHunHenanXcCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.henan.MJHandlerHun;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerHun_XC extends MJHandlerHun {
	protected int _da_dian_card;
	protected int _banker;

	@Override
	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(MJTable table) {
		int index = RandomUtil.getRandomNumber(GameConstants.MAX_ZI - 1);
		_da_dian_card = table._logic.switch_to_card_data(index);

		if (table.DEBUG_CARDS_MODE) {
			_da_dian_card = 0x16;
		}

		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		int card_next = 0;
		int cur_data = table._logic.get_card_value(_da_dian_card);

		if (cur_data == 1) {
			card_next = _da_dian_card + 1;
		} else if (cur_data == 9) {
			card_next = _da_dian_card - 8;
		} else {
			card_next = _da_dian_card + 1;
		}

		table._logic.add_magic_card_index(table._logic.switch_to_card_index(card_next));

		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = card_next;

		GameSchedule.put(new RevomeHunHenanXcCardRunnable(table.getRoom_id(), _banker), 3, TimeUnit.SECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(MJTable table, int seat_index) {
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
