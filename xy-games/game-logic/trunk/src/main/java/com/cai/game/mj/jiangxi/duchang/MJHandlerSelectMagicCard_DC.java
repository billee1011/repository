package com.cai.game.mj.jiangxi.duchang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.MJConstants_HuNan_XiangTan;
import com.cai.common.util.GameDescUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.XiangTanSelectMagicCardRunnable;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.yu.gd_tdh.Table_TDH;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class MJHandlerSelectMagicCard_DC extends AbstractMJHandler<MJTable_DC> {
	protected int _banker;
	protected int _huagang_card;
	protected int _bao_card;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@Override
	public void exe(MJTable_DC table) {
		// 从牌堆拿出一张牌，并显示在牌桌的正中央
		table._send_card_count++;
		_huagang_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE)
			_huagang_card = 0x11;

		_bao_card = get_next_card(table, _huagang_card);
		table.GRR._especial_card_count = 2;
		table.GRR._especial_show_cards[0] = _huagang_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
		table.GRR._especial_show_cards[1] = _bao_card + GameConstants.CARD_ESPECIAL_TYPE_GUI;

		// 添加宝牌,花杠（赖跟做花杠）
		table._logic.clean_magic_cards();
		int index = table._logic.switch_to_card_index(_bao_card);
		table._logic.add_magic_card_index(index);
		table._logic.add_lai_gen_card(_huagang_card);

		// 将花杠牌宝牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 2,
				new int[] { _huagang_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN,
						_bao_card + GameConstants.CARD_ESPECIAL_TYPE_GUI },
				GameConstants.INVALID_SEAT);

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				// 将翻出来的牌从牌桌的正中央移除
				table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);
			}
		}, 2000, TimeUnit.MILLISECONDS);

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == _huagang_card) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
				}
				if (hand_cards[j] == _bao_card) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		// 检测听牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards,
					table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], false, i);
			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);
			}
		}

		boolean is_qishou_hu = false;
		if (is_qishou_hu == false) {
			table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
		}
	}

	@Override
	public boolean handler_player_be_in_room(MJTable_DC table, int seat_index) {
		// 将花杠牌宝牌显示在牌桌的正中央
		table.showSpecialCard(seat_index);

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_banker);
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
					// 宝牌
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_GUI);
				} else if (table._logic.is_lai_gen_card(table.GRR._discard_cards[i][j])) {
					// 花杠
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN);
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

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_GUI;
			} else if (table._logic.is_lai_gen_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

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

	private int get_next_card(MJTable_DC table, int card) {
		int card_next = 0;

		int cur_value = table._logic.get_card_value(card);
		int cur_color = table._logic.get_card_color(card);
		if (cur_value == 9 && cur_color < 3) {
			card_next = card - 8;
		} else if (card == 0x34) {
			card_next = 0x31;
		} else if (card == 0x37) {
			card_next = 0x35;
		} else {
			card_next = _huagang_card + 1;
		}
		return card_next;
	}
}
