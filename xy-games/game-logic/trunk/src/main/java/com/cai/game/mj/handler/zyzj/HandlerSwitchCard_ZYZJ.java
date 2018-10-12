package com.cai.game.mj.handler.zyzj;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.GameConstants_ZYZJ;
import com.cai.future.GameSchedule;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerSwitchCard_ZYZJ extends AbstractMJHandler<Table_ZYZJ> {
	// 换三张的类型，默认为1：逆时针；2：对家；3：顺时针
	public int switch_type = 1;

	@Override
	public void exe(Table_ZYZJ table) {
		table._game_status = GameConstants.GS_MJ_SWITCH_CARD;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.had_switch_card[i] = false;
			table._player_result.biaoyan[i] = -1;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_SWITCH_CARD);
		if (table.is_mj_type(GameConstants.GAME_TYPE_MJ_ZHZJ)) {
			switch_type = ThreadLocalRandom.current().nextInt(1, 4);

			if (table.getTablePlayerNumber() == 2) {
				switch_type = 2;
			}
			if (table.getTablePlayerNumber() == 3) {
				if (switch_type == 2)
					switch_type = 3;
			}
		}
		table.load_room_info_data(roomResponse);

		table.send_response_to_room(roomResponse);
	}

	public boolean handler_switch_cards(Table_ZYZJ table, int seat_index, int[] switch_cards) {
		if (table.had_switch_card[seat_index]) {
			return false;
		}

		table.had_switch_card[seat_index] = true;
		table._player_result.biaoyan[seat_index] = 1;

		if (switch_cards.length == 3) {
			for (int i = 0; i < table.SWITCH_CARD_COUNT; i++) {
				table.switch_card_index[seat_index][i] = table._logic.switch_to_card_index(switch_cards[i]);
			}

			table._logic.remove_cards_by_index(table.GRR._cards_index[seat_index], switch_cards, table.SWITCH_CARD_COUNT);

			int[] hand_cards = new int[GameConstants.MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
					table.ding_que_pai_se[seat_index]);

			table.operate_player_cards(seat_index, hand_card_count, hand_cards, table.GRR._weave_count[seat_index],
					table.GRR._weave_items[seat_index]);
		}

		table.operate_player_info();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.had_switch_card[i] == false) {
				return true;
			}
		}

		if (table.is_mj_type(GameConstants.GAME_TYPE_MJ_ZHZJ)) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SC_FINISHED_SWITCH_CARD);
			roomResponse.setOperateLen(switch_type);
			table.load_room_info_data(roomResponse);
			table.send_response_to_room(roomResponse);
		}
		

		long delay = 0;
		if (table.is_mj_type(GameConstants.GAME_TYPE_MJ_ZHZJ)) {
			delay = 1000;
		}

		int pCount = table.getTablePlayerNumber();
		GameSchedule.put(() -> {
			for (int player = 0; player < pCount; player++) {
				int switch_player = table.get_banker_next_seat(player);

				if (pCount == 2) {
					switch_player = table.get_banker_next_seat(player);
				} else if (pCount == 3) {
					if (switch_type == 1) {
						switch_player = table.get_banker_next_seat(player);
					} else if (switch_type == 2 || switch_type == 3) {
						switch_player = table.get_banker_pre_seat(player);
					}
				} else if (pCount == 4) {
					if (switch_type == 1) {
						switch_player = table.get_banker_next_seat(player);
					} else if (switch_type == 2) {
						switch_player = (player + 1) % pCount;
					} else if (switch_type == 3) {
						switch_player = table.get_banker_pre_seat(player);
					}
				}

				for (int j = 0; j < table.SWITCH_CARD_COUNT; j++) {
					int tmp_index = table.switch_card_index[player][j];
					table.GRR._cards_index[switch_player][tmp_index]++;
				}

				int[] hand_cards = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[switch_player], hand_cards,
						table.ding_que_pai_se[switch_player]);

				table.operate_player_cards(switch_player, hand_card_count, hand_cards, table.GRR._weave_count[switch_player],
						table.GRR._weave_items[switch_player]);
			}			
			
			table._game_status = GameConstants.GS_MJ_PLAY;
			table.exe_dispatch_card(table._current_player, GameConstants_ZYZJ.DispatchCard_Type_Tian_Hu, GameConstants_ZYZJ.DELAY_SEND_CARD_DELAY);
		}, delay, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_ZYZJ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		// 设置骰子点数
		roomResponse.setEffectCount(2);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[0]);
		roomResponse.addEffectsIndex(table.tou_zi_dian_shu[1]);

		/*for (int i = 0; i < table.table_hu_card_count; i++) {
			roomResponse.addCardsList(table.table_hu_cards[i]);
		}*/

		roomResponse.setIsGoldRoom(table.is_sys());

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
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		int must_out_card_count = 0;
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)/* && table.hasRuleDingQue*/) {
			for (int i = 0; i < hand_card_count; i++) {
				if (table._logic.is_magic_card(hand_cards[i]))
					continue;

				int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
				if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
					must_out_card_count++;
				}
			}

			if (must_out_card_count > 0) {
				for (int i = 0; i < hand_card_count; i++) {
					if (table._logic.is_magic_card(hand_cards[i]))
						continue;

					int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
					if ((pai_se + 1) != table.ding_que_pai_se[seat_index]) {
						hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					}
				}
			}
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
			if(table._logic.is_magic_card(hand_cards[i])){
				hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_LAI_ZI;
			}
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(Table_ZYZJ table, int seat_index) {
		if (table.had_switch_card[seat_index]) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_SWITCH_CARD);
		table.load_room_info_data(roomResponse);

		table.send_response_to_player(seat_index, roomResponse);
	}
}
