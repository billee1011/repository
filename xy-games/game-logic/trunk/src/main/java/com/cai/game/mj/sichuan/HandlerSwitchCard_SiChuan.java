package com.cai.game.mj.sichuan;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_SiChuan;
import com.cai.future.GameSchedule;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerSwitchCard_SiChuan extends AbstractMJHandler<AbstractSiChuanMjTable> {
	// 换三张的类型，默认为1：逆时针；2：对家；3：顺时针
	public int switch_type = 1;

	@Override
	public void exe(AbstractSiChuanMjTable table) {
		table._game_status = GameConstants.GS_MJ_SWITCH_CARD;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.had_switch_card[i] = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_SWITCH_CARD);
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
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

		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_SC_FINISHED_SWITCH_CARD);
			roomResponse.setOperateLen(switch_type);
			table.load_room_info_data(roomResponse);
			table.send_response_to_room(roomResponse);
		}

		long delay = 0;
		if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) || table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			delay = 2000;
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

			if (table.is_mj_type(GameConstants.GAME_TYPE_MJ_SRLF)) {
				table._game_status = GameConstants.GS_MJ_PLAY;
				table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
			} else if (table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN)) {
				if (table.has_rule(Constants_SiChuan.GAME_RULE_DING_QUE)) {
					if (table.getTablePlayerNumber() == 4) {
						table.exe_ding_que();
					} else {
						table._game_status = GameConstants.GS_MJ_PLAY;
						table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
					}
				} else {
					table._game_status = GameConstants.GS_MJ_PLAY;
					table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
				}
			} else if (table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
				if (table.getTablePlayerNumber() == 4) {
					table.exe_ding_que();
				} else {
					table._game_status = GameConstants.GS_MJ_PLAY;
					table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
				}
			} else {
				table.exe_ding_que();
			}
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

		int must_out_card_count = 0;
		if ((table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && table.hasRuleDingQue)
				|| table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
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
		}

		for (int i = 0; i < GameConstants.MAX_COUNT; i++) {
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
}
