package com.cai.game.mj.sichuan.xueliuchenghe;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.future.GameSchedule;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.AbstractMJHandler;
import com.cai.game.mj.sichuan.AbstractSiChuanMjTable;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerSwitchCard_XueLiuChengHe extends AbstractMJHandler<AbstractSiChuanMjTable> {
	// 换三张的类型，默认为1：逆时针；2：对家；3：顺时针
	public int switch_type = 1;

	@Override
	public void exe(AbstractSiChuanMjTable table) {
		int pCount = table.getTablePlayerNumber();

		table._game_status = GameConstants.GS_MJ_SWITCH_CARD;

		for (int i = 0; i < pCount; i++) {
			table.had_switch_card[i] = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_SWITCH_CARD);

		int d = table.tou_zi_dian_shu[0];
		if (pCount == 2) {
			switch_type = 2;
		} else if (pCount == 3) {
			if (d == 1 || d == 2 || d == 3)
				switch_type = 3;
			if (d == 4 || d == 5 || d == 6)
				switch_type = 1;
		} else if (pCount == 4) {
			if (d == 1 || d == 2)
				switch_type = 3;
			if (d == 3 || d == 4)
				switch_type = 1;
			if (d == 5 || d == 6)
				switch_type = 2;
		}

		table.load_room_info_data(roomResponse);

		table.send_response_to_room(roomResponse);
	}

	public boolean handler_switch_cards(AbstractSiChuanMjTable table, int seat_index, int[] switch_cards) {
		if (table.had_switch_card[seat_index]) {
			return false;
		}

		table.had_switch_card[seat_index] = true;

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

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_FINISHED_SWITCH_CARD);

		// 骰子点数
		roomResponse.setOperateLen(table.tou_zi_dian_shu[0]);

		table.load_room_info_data(roomResponse);
		table.send_response_to_room(roomResponse);

		long delay = 2000;

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

				Map<Integer, Integer> tmpMap = table.player_switched_cards[switch_player];

				for (int j = 0; j < table.SWITCH_CARD_COUNT; j++) {
					int tmp_index = table.switch_card_index[player][j];
					int tmp_card = table._logic.switch_to_card_data(tmp_index);
					table.GRR._cards_index[switch_player][tmp_index]++;

					if (tmpMap.containsKey(tmp_card)) {
						int count = tmpMap.get(tmp_card);
						tmpMap.put(tmp_card, ++count);
					} else {
						tmpMap.put(tmp_card, 1);
					}
				}

				int[] hand_cards = new int[GameConstants.MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[switch_player], hand_cards,
						table.ding_que_pai_se[switch_player]);

				for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
					int card = entry.getKey();
					int count = entry.getValue();

					if (count > 0) {
						for (int j = 0; j < hand_card_count && count > 0; j++) {
							if (card == hand_cards[j]) {
								hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
								--count;
							}
						}
					}
				}

				table.operate_player_cards(switch_player, hand_card_count, hand_cards, table.GRR._weave_count[switch_player],
						table.GRR._weave_items[switch_player]);
			}

			table.exe_ding_que();
		}, delay, TimeUnit.MILLISECONDS);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(AbstractSiChuanMjTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		MahjongUtils.showTouZiSiChuan(table, roomResponse);

		roomResponse.setIsGoldRoom(table.is_sys());

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		MahjongUtils.dealCommonDataReconnect(table, roomResponse, tableResponse);

		MahjongUtils.dealAllPlayerCardsNoSpecial(table, tableResponse);

		int hand_cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], hand_cards,
				table.ding_que_pai_se[seat_index]);

		Map<Integer, Integer> tmpMap = table.player_switched_cards[seat_index];

		for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
			int card = entry.getKey();
			int count = entry.getValue();

			if (count > 0) {
				for (int j = 0; j < hand_card_count && count > 0; j++) {
					if (card == hand_cards[j]) {
						hand_cards[j] += GameConstants.CARD_ESPECIAL_TYPE_SWITCHED_CARD;
						--count;
					}
				}
			}
		}

		int must_out_card_count = 0;
		for (int i = 0; i < hand_card_count; i++) {
			if (table._logic.is_magic_card(table.get_real_card(hand_cards[i])))
				continue;

			int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
			if ((pai_se + 1) == table.ding_que_pai_se[seat_index]) {
				must_out_card_count++;
			}
		}

		if (must_out_card_count > 0) {
			for (int i = 0; i < hand_card_count; i++) {
				if (table._logic.is_magic_card(table.get_real_card(hand_cards[i]))) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					continue;
				}

				int pai_se = table._logic.get_card_color(table.get_real_card(hand_cards[i]));
				if ((pai_se + 1) != table.ding_que_pai_se[seat_index]) {
					hand_cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
				}
			}
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		player_reconnect(table, seat_index);

		return true;
	}

	private void player_reconnect(AbstractSiChuanMjTable table, int seat_index) {
		if (table.had_switch_card[seat_index]) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_SWITCH_CARD);
		table.load_room_info_data(roomResponse);

		table.send_response_to_player(seat_index, roomResponse);
	}

	@Override
	public boolean handler_be_set_trustee(AbstractSiChuanMjTable table, int seat_index) {
		return false;
	}
}
