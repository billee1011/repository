package com.cai.game.mj.hubei.ezhou;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.Constants_EZ;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerSelectMagic_EZ extends AbstractMJHandler<Table_EZ> {
	protected int _da_dian_card;

	protected int _banker;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_EZ table) {
		// 从牌堆拿出一张牌，并显示在牌桌的正中央
		/**
		 * 摇出来的两个骰子，点数小的用来起牌，点数大的用来翻牌。比如摇出来3和4，起牌时，以玩家1为起始位置，逆时针数第3家（玩家3）的第4墩牌，开始抓牌。
		 * 翻牌时，以起牌位置（玩家3）的下家（玩家4）的逆时针（从左到右）的第4墩牌的上面一张牌，为翻出来的牌。
		 * 翻出来的牌，不能再次使用，不能被杠走也不能被抓走。 癞子牌参与吃时，只能把癞子牌值还原之后参与吃牌。
		 */
		table._send_card_count++;
		_da_dian_card = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE)
			_da_dian_card = 0x15;

		table.da_dian_card = _da_dian_card;

		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			table.da_dian_card = _da_dian_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { _da_dian_card }, GameConstants.INVALID_SEAT);

		int card_next = 0;

		int cur_value = table._logic.get_card_value(_da_dian_card);
		int cur_color = table._logic.get_card_color(_da_dian_card);

		if (cur_color == 3) {
			if (cur_value == 5) {
				card_next = _da_dian_card + 1;
			} else if (cur_value == 6) {
				card_next = _da_dian_card + 1;
			} else if (cur_value == 7) {
				card_next = _da_dian_card - 1;
			}
		} else {
			if (cur_value == 9) {
				card_next = _da_dian_card - 8;
			} else {
				card_next = _da_dian_card + 1;
			}
		}

		table.magic_card_index = table._logic.switch_to_card_index(card_next);

		// 添加鬼
		table._logic.add_magic_card_index(table.magic_card_index);
		table.GRR._especial_card_count = 1;
		table.GRR._especial_show_cards[0] = card_next + GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;

		// 处理每个玩家手上的牌，如果有王牌，处理一下
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[i], hand_cards);
			for (int j = 0; j < hand_card_count; j++) {
				if (table._logic.is_magic_card(hand_cards[j])) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (hand_cards[j] == Constants_EZ.HZ_CARD) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
			}
			// 玩家客户端刷新一下手牌
			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		// 检测听牌
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards, table._playerStatus[i]._hu_out_cards_fan[0],
					table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i);
			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);

				// 显示自动胡牌按钮
				table.operate_auto_win_card(i, true);
			}
		}

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				// 将翻出来的牌从牌桌的正中央移除
				table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

				if (table.is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
					table.exe_add_discard(_banker, 1, new int[] { _da_dian_card }, false, GameConstants.DELAY_SEND_CARD_DELAY);
					table.operate_add_discard(_banker, 1, new int[] { _da_dian_card + GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN });
				}

				table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, GameConstants.DELAY_SEND_CARD_DELAY);
			}
		}, 3000, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(Table_EZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// roomResponse.setTarget(seat_index);
		// roomResponse.setScoreType(table.get_player_fan_shu(seat_index));
		// table.send_response_to_other(seat_index, roomResponse);

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
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				int real_card = table.GRR._discard_cards[i][j];
				if (j == 0 && i == table.GRR._banker_player && table.is_mj_type(GameConstants.GAME_TYPE_3D_E_ZHOU)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_GEN;
				} else if (table._logic.is_magic_card(real_card)) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
				} else if (real_card == Constants_EZ.HZ_CARD) {
					real_card += GameConstants.CARD_ESPECIAL_TYPE_HZ;
				}
				int_array.addItem(real_card);
			}
			tableResponse.addDiscardCards(int_array);

			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player + GameConstants.WEAVE_SHOW_DIRECT);

				if (table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_GANG && table.GRR._weave_items[i][j].public_card == 0) {
					// 暗杠的牌的显示
					if (seat_index == i) {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					} else {
						weaveItem_item.setCenterCard(0);
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			tableResponse.addWinnerOrder(0);

			tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
		}

		tableResponse.setSendCardData(0);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		table._logic.switch_to_cards_data_ezhou(table.GRR._cards_index[seat_index], hand_cards);

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if (table._logic.is_magic_card(hand_cards[i])) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			} else if (hand_cards[i] == Constants_EZ.HZ_CARD) {
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_HZ;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		// TODO 添加是否托管
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(table.istrustee[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);

			// 出牌之后，如果有听牌数据，显示‘自动胡牌’按钮
			table.operate_auto_win_card(seat_index, true);
		}

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
			table.operate_player_action(seat_index, false);
		}

		return true;
	}
}
