package com.cai.game.wsk.handler.shangraodazha;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.wsk.handler.WSKHandlerCallBnaker;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gfWsk.gfWskRsp.CallBankerResponse_GF;
import protobuf.clazz.gfWsk.gfWskRsp.LiangPai_Result_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TableResponse_Wsk_GF;
import protobuf.clazz.gfWsk.gfWskRsp.TouXiang_Result_Wsk_GF;

public class WSKHandlerCallBnaker_ShangRaoDaZha extends WSKHandlerCallBnaker<WSKTable_ShangRaoDaZha> {

	@Override
	public void exe(WSKTable_ShangRaoDaZha table) {
	}

	public boolean handler_call_banker(WSKTable_ShangRaoDaZha table, int seat_index, int call_action) {
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
					table._cur_banker = next_player;
					table._current_player = next_player;
					break;
				}
			}
			outer: if (is_finish) {
				// 免打
				if (table.checkMianDa()) {
					break outer;
				}
				if (table.has_rule(GameConstants.GAME_RULE_WSK_GF_CALL_HONGTAO_SAN)) {
					table._jiao_pai_card = 0x23;
					table._cur_banker = GameConstants.INVALID_SEAT;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						for (int j = 0; j < table.GRR._card_count[i]; j++) {
							if (table._jiao_pai_card == table.GRR._cards_data[i][j] && table._cur_banker == GameConstants.INVALID_SEAT) {
								table._cur_banker = i;
								break;
							} else if (table._jiao_pai_card == table.GRR._cards_data[i][j]) {
								table._friend_seat[table._cur_banker] = i;
								table._friend_seat[i] = table._cur_banker;
							}
						}
					}
					if (table._friend_seat[table._cur_banker] == table._cur_banker) {
						table._friend_seat[table._cur_banker] = (table._cur_banker + 2) % table.getTablePlayerNumber();
						table._friend_seat[(table._cur_banker + 2) % table.getTablePlayerNumber()] = table._cur_banker;
					}
					
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i != table._cur_banker && i != table._friend_seat[table._cur_banker]) {
							for (int j = 0; j < table.getTablePlayerNumber(); j++) {
								if (j != table._cur_banker && j != table._friend_seat[table._cur_banker] && i != j) {
									table._friend_seat[i] = j;
								}
							}
						}
					}

					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_LIANG_PAI_RESULT);
					// 发送数据
					LiangPai_Result_Wsk_GF.Builder liang_pai_result = LiangPai_Result_Wsk_GF.newBuilder();
					liang_pai_result.setOpreatePlayer(seat_index);
					liang_pai_result.setCardData(table._jiao_pai_card);
					liang_pai_result.addSeatIndex(table._cur_banker);
					liang_pai_result.addSeatIndex(table._friend_seat[table._cur_banker]);
					roomResponse.setCommResponse(PBUtil.toByteString(liang_pai_result));
					table.GRR.add_room_response(roomResponse);
					table.send_response_to_room(roomResponse);
					
					table._game_status = GameConstants.GS_GFWSK_TOU_XIANG;
					table.set_timer(WSKTable_ShangRaoDaZha.TIME_OUT_NO_TOU_XIANG, 20);
					
//					table._game_status = GameConstants.GS_GFWSK_PLAY;
//					table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);
				} else {
					table._game_status = GameConstants.GS_GFWSK_LIANG_PAI;
					table._current_player = (table._current_player + 1) % table.getTablePlayerNumber();
				}
			}
		} else {
			// if(table.has_rule(GameConstants.GAME_RULE_WSK_GF_TOU_XIANG)){
			// table._game_status = GameConstants.GS_GFWSK_TOU_XIANG;
			// }else{
			table._game_status = GameConstants.GS_GFWSK_PLAY;
			// }
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._is_yi_da_san = true;
			table._is_tou_xiang[seat_index] = 3;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_CALL_BANKER_RESULT);
		// 发送数据
		CallBankerResponse_GF.Builder callbanker_result = CallBankerResponse_GF.newBuilder();
		callbanker_result.setBankerPlayer(table.GRR._banker_player);
		callbanker_result.setOpreateAction(call_action);
		callbanker_result.setCallPlayer(seat_index);
		callbanker_result.setCurrentPlayer(table._current_player);
		callbanker_result.setDisplayTime(10);
		callbanker_result.setRoomInfo(table.getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
		table.GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		table.send_response_to_room(roomResponse);
		// 一打三直接开看局
		if (table._is_yi_da_san) {
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}

		if (table._game_status == GameConstants.GS_GFWSK_TOU_XIANG) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//				if (i == table.GRR._banker_player) {
//					continue;
//				}
				roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_BEGIN);
				table.send_response_to_player(i, roomResponse);
			}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_ShangRaoDaZha table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_RECONNECT_DATA);

		TableResponse_Wsk_GF.Builder tableResponse = TableResponse_Wsk_GF.newBuilder();
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
			if (table.GRR._card_count[i] <= 5 || i == seat_index) {
				tableResponse.addCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse.addCardCount(-1);
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
			for (int j = 0; j < table._fei_wang_count[i]; j++) {
				wang_cards.addItem(table._fei_wang_card[i][j]);
			}
			if (table._jiao_pai_card != GameConstants.INVALID_CARD) {
				if (i == table._cur_banker) {
					tableResponse.addJiaoCardData(table._jiao_pai_card);
				} else if (table._friend_seat[table._cur_banker] == i) {
					tableResponse.addJiaoCardData(table._jiao_pai_card);
				} else {
					tableResponse.addJiaoCardData(GameConstants.INVALID_CARD);
				}
			} else {
				tableResponse.addJiaoCardData(GameConstants.INVALID_CARD);
			}

			tableResponse.addFeiWang(wang_cards);
			tableResponse.addTouXiangStatus(table._is_tou_xiang[i]);
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
		}
		tableResponse.setIsYiDaSan(table._is_yi_da_san);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);

		if (table._game_status == GameConstants.GS_GFWSK_TOU_XIANG) {
			if (table._is_yi_da_san) {
				if (seat_index != table.GRR._banker_player) {
					boolean wait_agree_tou_xiang = false;
					int tou_xiang_request_seat = GameConstants.INVALID_SEAT;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table.GRR._banker_player) {
							continue;
						}
						if (table._is_tou_xiang[i] == 1) {
							wait_agree_tou_xiang = true;
							tou_xiang_request_seat = i;
							break;
						}
					}
					if (wait_agree_tou_xiang) {
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (i == table.GRR._banker_player) {
								continue;
							}
							if (table._is_tou_xiang[i] == 0) {
								table.send_is_tou_xiang(tou_xiang_request_seat, true, i);
							}
						}
					} else {
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							if (i == table.GRR._banker_player) {
								continue;
							}
							roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_BEGIN);
							table.send_response_to_player(seat_index, roomResponse);
						}
					}
				}
			} else {
				if (table._is_tou_xiang[seat_index] == 1) {
					if (table._tou_xiang_times[seat_index] == 1) {
						roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_RESULT);
						// 发送数据
						TouXiang_Result_Wsk_GF.Builder tou_xiang_result = TouXiang_Result_Wsk_GF.newBuilder();
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						tou_xiang_result.setOpreateStr("你的搭档拒绝投降，你是否投降");
						tou_xiang_result.setIsOkCancel(1);
						tou_xiang_result.setOpreateSeatIndex(seat_index);
						roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_result));
						table.send_response_to_player(seat_index, roomResponse);
					}
				} else if (table._is_tou_xiang[table._friend_seat[seat_index]] == 1) {
					if (table._is_tou_xiang_agree[seat_index] == -1) {
						table.send_is_tou_xiang(table._friend_seat[seat_index], true, seat_index);
					}
				} else if (table._is_tou_xiang[seat_index] == 0) {
					roomResponse.setType(MsgConstants.RESPONSE_WSK_GF_TOUXIANG_BEGIN);
					table.send_response_to_player(seat_index, roomResponse);
				}
			}
		}
		if (table._game_status == GameConstants.GS_GFWSK_PLAY) {
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_GF_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}
		return true;
	}
}
