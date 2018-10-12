package com.cai.game.wsk.handler.hs;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.wsk.handler.WSKHandlerCallBnaker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.hsdy.hsdyRsp.CallBankerResponse_hsdy;
import protobuf.clazz.hsdy.hsdyRsp.TableResponse_hsdy;

public class WSKHandlerCallBnakerHS extends WSKHandlerCallBnaker<WSKTable_HS> {
	@Override
	public void exe(WSKTable_HS table) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_CALL_BANKER_RESULT);
		// 发送数据
		CallBankerResponse_hsdy.Builder callbanker_result = CallBankerResponse_hsdy.newBuilder();
		callbanker_result.setBankerPlayer(table.GRR._banker_player);
		callbanker_result.setOpreateAction(-1);
		callbanker_result.setCallPlayer(GameConstants.INVALID_SEAT);
		callbanker_result.setCurrentPlayer(table._current_player);
		callbanker_result.setDisplayTime(10);
		callbanker_result.setRoomInfo(table.getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
		// 自己才有牌数据
		table.send_response_to_room(roomResponse);

		table.GRR.add_room_response(roomResponse);
	}

	public boolean handler_call_banker(WSKTable_HS table, int seat_index, int call_action) {
		if (table._is_call_banker[seat_index] == 1 || table._current_player != seat_index) {
			return false;
		}
		table._is_call_banker[seat_index] = 1;
		if (call_action == 0) {
			boolean is_finish = true;
			int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._is_call_banker[next_player] == 0) {
					is_finish = false;
					table._current_player = next_player;
					break;
				}
			}
			if (is_finish) {
				table.GRR._banker_player = table._cur_banker;
				table.send_call_banker_result(seat_index, GameConstants.INVALID_SEAT, call_action);
				// table._game_status = GameConstants.GS_GFWSK_LIANG_PAI;
				table._game_status = GameConstants.GS_GFWSK_PLAY;
				table._current_player = (table._current_player + 1) % table.getTablePlayerNumber();
				table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
						GameConstants.INVALID_SEAT, false);

				int card_vale = table._logic.get_liang_pai(table.GRR._cards_data[table._current_player],
						table.GRR._card_count[table._current_player]);
				table.deal_liang_pai(table._current_player, card_vale);
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._is_call_banker[i] = 0;
				}
				table.set_handler(table._handler_out_card_operate);
				return true;
			}
		} else {
			table._turn_have_score = -1;
			table._game_status = GameConstants.GS_GFWSK_PLAY;
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._is_yi_da_san = true;
			table._du_num[seat_index]++;
			table.send_call_banker_result(seat_index, GameConstants.INVALID_SEAT, call_action);
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR,
					GameConstants.INVALID_SEAT, false);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._is_call_banker[i] = 0;
			}
			table.set_handler(table._handler_out_card_operate);
			table.Refresh_user_get_score(GameConstants.INVALID_SEAT);
			return true;
		}

		table.send_call_banker_result(seat_index, table._current_player, call_action);
		return true;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_HS table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HSDY_RECONNECT_DATA);

		TableResponse_hsdy.Builder tableResponse = TableResponse_hsdy.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
		tableResponse.setPrevPlayer(table._prev_palyer);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (!table.has_rule(GameConstants.GAME_RULE_HSDY_DISPLAY_CARD) && i != seat_index) {
				tableResponse.addCardCount(-1);
			} else {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			}

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder wang_cards = Int32ArrayResponse.newBuilder();
			if (seat_index == i) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}

			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
			tableResponse.addCallBanker(table._is_call_banker[i]);
		}
		tableResponse.setJiaoCardData(table._jiao_pai_card);
		tableResponse.setIsYiDaSan(table._is_yi_da_san);

		if (!table._is_yi_da_san && table._out_card_ming_ji != GameConstants.INVALID_CARD) {
			if (table.GRR._card_count[seat_index] == 0) {
				tableResponse.setFriendSeatIndex(table._friend_seat[seat_index]);
			} else {
				tableResponse.setFriendSeatIndex(GameConstants.INVALID_SEAT);
			}
		} else {
			tableResponse.setFriendSeatIndex(GameConstants.INVALID_SEAT);
		}
		if (table._out_card_ming_ji == GameConstants.INVALID_CARD) {
			tableResponse.setBankerFriendSeat(GameConstants.INVALID_CARD);
		} else {
			tableResponse.setBankerFriendSeat(table._friend_seat[table.GRR._banker_player]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.send_call_banker_result(GameConstants.INVALID_CARD, table._current_player, 0);
		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);
		table.Refresh_Record_Out_Card(seat_index);
		return true;
	}

}
