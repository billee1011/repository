package com.cai.game.wsk.handler.yxzd;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.Player;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.game.wsk.WSKConstants;
import com.cai.game.wsk.WSKMsgConstants;
import com.cai.game.wsk.handler.WSKHandlerCallBnaker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.yxzd.yxzdkRsp.FanPai_Response_yxzd;
import protobuf.clazz.yxzd.yxzdkRsp.TableResponse_yxzd;

public class WSKHandlerCallBnakerYXZD extends WSKHandlerCallBnaker<WSKTable_YXZD> {

	@Override
	public void exe(WSKTable_YXZD table) {
		table.send_fan_pai_begin(GameConstants.INVALID_SEAT);
	}

	public boolean handler_call_banker(WSKTable_YXZD table, int seat_index, int call_action) {
		if (table._call_banker[seat_index] != -1 || table._game_status != WSKConstants.GS_YXZD_FAN_PAI) {
			return true;
		}

		if (call_action < -1 || call_action > 3) {
			return true;
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (call_action == table._call_banker[i]) {
				return true;
			}
		}
		table._call_banker[seat_index] = call_action;

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_FAN_RESPONSE);
		roomResponse.setGameStatus(table._game_status);
		FanPai_Response_yxzd.Builder fan_pai_response = FanPai_Response_yxzd.newBuilder();
		fan_pai_response.setControlIndex(seat_index);
		fan_pai_response.setCardIndex(call_action);
		for (int i = 0; i < 4; i++) {
			boolean is_fan = false;
			for (int j = 0; j < 4; j++) {
				if (table._call_banker[j] == i) {
					is_fan = true;
					break;
				}
			}
			if (is_fan) {
				fan_pai_response.addCardData(table._fan_pai_card[i]);
			} else {
				fan_pai_response.addCardData(GameConstants.BLACK_CARD);
			}
		}

		roomResponse.setCommResponse(PBUtil.toByteString(fan_pai_response));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		if (table._fan_pai_card[call_action] != 0x4F) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Player player = table.get_players()[i];
				if (player != null) {
					if (player.isRobot()) {
						boolean is_find = false;
						for (int x = 0; x < table.getTablePlayerNumber(); x++) {
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (x == table._call_banker[j]) {
									break;
								}
								if (j == table.getTablePlayerNumber() - 1) {
									handler_call_banker(table, i, x);
									is_find = true;
								}
							}
							if (is_find) {
								break;
							}
						}
						continue;
					}
				}
				if (table._call_banker[i] == -1) {
					return true;
				}
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._call_banker[i] != -1 && table._fan_pai_card[table._call_banker[i]] == 0x4F) {
				table._current_player = i;
				break;
			}
		}

		table.schedule(table.ID_TIMER_OUT_CARD, SheduleArgs.newArgs(), 1500);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_YXZD table, int seat_index) {
		if (table.GRR == null) {
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(WSKMsgConstants.RESPONSE_WSK_YXZD_RECONNECT_DATA);

		TableResponse_yxzd.Builder tableResponse = TableResponse_yxzd.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
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
			if (seat_index == i || (table._friend_seat[seat_index] == i && table.GRR._card_count[seat_index] == 0)) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else if (table.GRR._card_count[i] > 15) {
				tableResponse.addCardCount(-1);
			} else if (table.GRR._card_count[i] <= 15 && table.GRR._card_count[i] > 0) {
				tableResponse.addCardCount(-2);
			} else {
				tableResponse.addCardCount(0);
			}

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addCardType(table._cur_out_car_type[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			boolean is_out_finish = false;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table._chuwan_shunxu[j] == i) {
					tableResponse.addWinOrder(j);
					is_out_finish = true;
					break;
				}

			}
			if (!is_out_finish) {
				tableResponse.addWinOrder(GameConstants.INVALID_SEAT);
			}
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		table.send_fan_pai_begin(seat_index);
		return true;
	}
}
