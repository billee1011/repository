package com.cai.game.mj.hunan.taojiang;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_TaoJiang;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerSelectMagicCard_TaoJiang extends AbstractMJHandler<Table_TaoJiang> {

	protected int _banker;

	protected int _send_card_data;

	public void reset_status(int banker) {
		_banker = banker;
	}

	@SuppressWarnings("static-access")
	@Override
	public void exe(Table_TaoJiang table) {
		// 根据骰子点数，从牌堆的最后拿牌。翻出来的牌，继续留在牌墩。
		table.ding_wang_card = table._repertory_card[table._all_card_len - 1 - (table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1]) * 2];

		// TODO 为了方便测试，定王牌先固定成一万
		if (table.DEBUG_CARDS_MODE) {
			table.ding_wang_card = 0x21;
		}

		if (table.DEBUG_MAGIC_CARD) {
			// SSHE后台管理系统，通过“mj#房间号#5#翻的牌”来指定翻出来的牌是哪张
			table.ding_wang_card = table.magic_card_decidor;
			table.DEBUG_MAGIC_CARD = false;
		}

		// 将翻出来的牌显示在牌桌的正中央
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { table.ding_wang_card }, GameConstants.INVALID_SEAT);

		int cur_data = table._logic.get_card_value(table.ding_wang_card);

		if (cur_data >= 1 && cur_data <= 7) {
			table.joker_card_1 = table.ding_wang_card + 1;
			table.joker_card_2 = table.ding_wang_card + 2;
		} else if (cur_data == 8) {
			table.joker_card_1 = table.ding_wang_card + 1;
			table.joker_card_2 = table.ding_wang_card - 7;
		} else if (cur_data == 9) {
			table.joker_card_1 = table.ding_wang_card - 8;
			table.joker_card_2 = table.ding_wang_card - 7;
		}

		table.ding_wang_card_index = table._logic.switch_to_card_index(table.ding_wang_card);
		table.joker_card_index_1 = table._logic.switch_to_card_index(table.joker_card_1);
		table.joker_card_index_2 = table._logic.switch_to_card_index(table.joker_card_2);

		if (table.has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.joker_card_1));
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.joker_card_2));

			table.GRR._especial_card_count = 3;

			if (table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				table.GRR._especial_show_cards[0] = table.ding_wang_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			} else {
				table.GRR._especial_show_cards[0] = table.ding_wang_card;
			}
			table.GRR._especial_show_cards[1] = table.joker_card_1 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			table.GRR._especial_show_cards[2] = table.joker_card_2 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else {
			table._logic.add_magic_card_index(table._logic.switch_to_card_index(table.joker_card_1));

			table.GRR._especial_card_count = 2;

			if (table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				table.GRR._especial_show_cards[0] = table.ding_wang_card + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			} else {
				table.GRR._especial_show_cards[0] = table.ding_wang_card;
			}
			table.GRR._especial_show_cards[1] = table.joker_card_1 + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;

			// TODO 4王时，需要清空一下
			table.joker_card_2 = 0;
			table.joker_card_index_2 = -1;
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], hand_cards);

			// 处理王牌和定王牌
			for (int j = 0; j < hand_card_count; j++) {
				if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
				} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
				}
			}

			table.operate_player_cards(i, hand_card_count, hand_cards, 0, null);
		}

		// 获取听牌数据，获取听牌数据的时候，只显示那些正常胡牌的牌数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i]._hu_card_count = table.get_ting_card(table._playerStatus[i]._hu_cards, table.GRR._cards_index[i],
					table.GRR._weave_items[i], table.GRR._weave_count[i], i);
			if (table._playerStatus[i]._hu_card_count > 0) {
				table.operate_chi_hu_cards(i, table._playerStatus[i]._hu_card_count, table._playerStatus[i]._hu_cards);

				table.qi_shou_ting[i] = true;

				table._playerStatus[i].set_ting_state(true);
			}
		}

		// TODO 庄家发第14张牌
		++table._send_card_count;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.distance_to_ding_wang_card > 0) {
			table.distance_to_ding_wang_card = table.GRR._left_card_count - (table.tou_zi_dian_shu[0] + table.tou_zi_dian_shu[1]) * 2;
		}

		if (table.DEBUG_CARDS_MODE) {
			// _send_card_data = 0x09;
		}

		table._send_card_data = _send_card_data;

		table._current_player = _banker;
		table._provide_player = _banker;

		table._last_dispatch_player = _banker;

		table.GRR._cards_index[_banker][table._logic.switch_to_card_index(_send_card_data)]++;

		// 处理王牌
		int real_card = _send_card_data;
		if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		// 客户端显示玩家抓牌
		table.operate_player_get_card(_banker, 1, new int[] { real_card }, GameConstants.INVALID_SEAT);

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				table.operate_show_card(_banker, GameConstants.Show_Card_Center, 0, null, GameConstants.INVALID_SEAT);

				if (table.has_rule(Constants_TaoJiang.GAME_RULE_EIGHT_JOKER)) {
					// 八王时，庄家先出牌，再来处理报听
					table.exe_dispatch_card(table._cur_banker, GameConstants.WIK_BAO_TING, 0);
				} else {
					// 处理闲家的‘报听’
					table.exe_bao_ting(1, table._cur_banker, GameConstants.INVALID_VALUE);
				}
			}
		}, table.DELAY_GAME_START, TimeUnit.MILLISECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(Table_TaoJiang table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 离地王牌还有多少张
		if (table.distance_to_ding_wang_card > 0) {
			roomResponse.setOperateLen(table.distance_to_ding_wang_card);
		}

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
				if (table.GRR._discard_cards[i][j] == table.joker_card_1 || table.GRR._discard_cards[i][j] == table.joker_card_2) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_WANG_BA);
				} else if (table.GRR._discard_cards[i][j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
					int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI);
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

			if (i == _banker) {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}

		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);

		// 如果断线重连的人是自己
		if (seat_index == _banker) {
			table._logic.remove_card_by_data(hand_cards, _send_card_data);
		}

		for (int j = 0; j < hand_card_count; j++) {
			if (hand_cards[j] == table.joker_card_1 || hand_cards[j] == table.joker_card_2) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
			} else if (hand_cards[j] == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
				hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 处理王牌
		int real_card = _send_card_data;
		if (real_card == table.joker_card_1 || real_card == table.joker_card_2) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_WANG_BA;
		} else if (real_card == table.ding_wang_card && table.has_rule(Constants_TaoJiang.GAME_RULE_SHOW_DI_PAI)) {
			real_card += GameConstants.CARD_ESPECIAL_TYPE_DING_WANG_PAI;
		}

		// 客户端显示玩家抓牌
		table.operate_player_get_card(_banker, 1, new int[] { real_card }, seat_index);

		// TODO 显示听牌数据
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		// 比吃碰的断线重连多了一个客户端显示
		table.operate_show_card(_banker, GameConstants.Show_Card_Center, 1, new int[] { table.ding_wang_card }, seat_index);

		return true;
	}
}
