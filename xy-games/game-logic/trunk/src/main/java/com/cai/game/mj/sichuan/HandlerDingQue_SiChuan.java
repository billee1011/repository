package com.cai.game.mj.sichuan;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.future.GameSchedule;
import com.cai.game.mj.MahjongUtils;
import com.cai.game.mj.handler.AbstractMJHandler;

import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;

public class HandlerDingQue_SiChuan extends AbstractMJHandler<AbstractSiChuanMjTable> {
	@Override
	public void exe(AbstractSiChuanMjTable table) {
		table._game_status = GameConstants.GS_MJ_DING_QUE;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table.had_ding_que[i] = false;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_DING_QUE);
		table.load_room_info_data(roomResponse);

		table.send_response_to_room(roomResponse);
	}

	public boolean handler_ding_que(AbstractSiChuanMjTable table, int seat_index, int pai_se) {

		if (table.had_ding_que[seat_index]) {
			return false;
		}

		table.had_ding_que[seat_index] = true;

		if (pai_se == 1 || pai_se == 2 || pai_se == 3) {
			table.ding_que_pai_se[seat_index] = pai_se;
		}

		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_sichuan(table.GRR._cards_index[seat_index], cards, pai_se);

		int must_out_card_count = 0;
		if ((table.is_mj_type(GameConstants.GAME_TYPE_GUANG_AN) && table.hasRuleDingQue)
				|| table.is_mj_type(GameConstants.GAME_TYPE_LE_SHAN_YAO_JI)) {
			for (int i = 0; i < hand_card_count; i++) {
				if (table._logic.is_magic_card(table.get_real_card(cards[i])))
					continue;

				int tmp_pai_se = table._logic.get_card_color(table.get_real_card(cards[i]));
				if ((tmp_pai_se + 1) == table.ding_que_pai_se[seat_index]) {
					must_out_card_count++;
				}
			}

			if (must_out_card_count > 0) {
				for (int i = 0; i < hand_card_count; i++) {
					if (table._logic.is_magic_card(table.get_real_card(cards[i]))) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
						continue;
					}

					int tmp_pai_se = table._logic.get_card_color(table.get_real_card(cards[i]));
					if ((tmp_pai_se + 1) != table.ding_que_pai_se[seat_index]) {
						cards[i] += GameConstants.CARD_ESPECIAL_TYPE_CAN_NOT_OUT;
					}
				}
			}
		}

		table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index]);

		table.operate_player_info();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.had_ding_que[i] == false) {
				return true;
			}
		}

		table._game_status = GameConstants.GS_MJ_PLAY;

		GameSchedule.put(new Runnable() {
			@Override
			public void run() {
				table.exe_qi_shou(table.GRR._banker_player, GameConstants.WIK_NULL);
			}
		}, 1, TimeUnit.SECONDS);

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
		if (table.had_ding_que[seat_index]) {
			return;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_SC_DING_QUE);
		table.load_room_info_data(roomResponse);

		table.send_response_to_player(seat_index, roomResponse);
	}
}
