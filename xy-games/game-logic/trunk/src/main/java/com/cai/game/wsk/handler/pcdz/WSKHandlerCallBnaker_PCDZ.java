package com.cai.game.wsk.handler.pcdz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.wsk.handler.WSKHandlerCallBnaker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.pcWsk.pcWskRsp.CallBankerResponse_PC;
import protobuf.clazz.pcWsk.pcWskRsp.LiangPai_Result_Wsk_PC;
import protobuf.clazz.pcWsk.pcWskRsp.TableResponse_Wsk_PC;

public class WSKHandlerCallBnaker_PCDZ extends WSKHandlerCallBnaker<WSKTable_PCDZ> {

	@Override
	public void exe(WSKTable_PCDZ table) {
	}

	public boolean handler_call_banker(WSKTable_PCDZ table, int seat_index, int call_action) {
		if (table._is_call_banker[seat_index] == 1 ) {
			return false;
		}
		if(table._game_status != GameConstants.GS_PCWSK_CALLBANKER)
			return false;
		table._is_call_banker[seat_index] = 1;
		if (call_action == 0) {
			boolean is_finish = true;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._is_call_banker[i] == 0) {
					is_finish = false;

					break;
				}
			}
			if (is_finish) {
			
				table._game_status = GameConstants.GS_PCWSK_LIANG_PAI;
				table._current_player = table._first_banker ;
				
			}
		} else {
			// if(table.has_rule(GameConstants.GAME_RULE_WSK_PC_TOU_XIANG)){
			// table._game_status = GameConstants.GS_PCWSK_TOU_XIANG;
			// }else{
			table._game_status = GameConstants.GS_PCWSK_PLAY;
			// }
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._is_yi_da_san = true;
			table._is_tou_xiang[seat_index] = 3;
			table._cur_banker = seat_index ;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_CALL_BANKER_RESULT);
		// 发送数据
		CallBankerResponse_PC.Builder callbanker_result = CallBankerResponse_PC.newBuilder();
		callbanker_result.setBankerPlayer(table.GRR._banker_player);
		callbanker_result.setOpreateAction(call_action);
		callbanker_result.setCallPlayer(seat_index);
		callbanker_result.setCurrentPlayer(table._current_player);
		callbanker_result.setDisplayTime(table._display_timer);
		callbanker_result.setRoomInfo(table.getRoomInfo());
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));
		table.GRR.add_room_response(roomResponse);
		// 自己才有牌数据
		if(call_action == 0 && table._game_status != GameConstants.GS_PCWSK_LIANG_PAI)
		{
			callbanker_result.setBankerPlayer(-1);
			callbanker_result.setCurrentPlayer(-1);
			table.send_response_to_room(roomResponse);
		}
		else
			table.send_response_to_room(roomResponse);
		// 一打三直接开看局
		if (table._is_yi_da_san) {
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_PC_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}
		if(table._game_status == GameConstants.GS_PCWSK_LIANG_PAI){
			table.deal_liang_pai(table._current_player, 0);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_PCDZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_PC_RECONNECT_DATA);

		TableResponse_Wsk_PC.Builder tableResponse = TableResponse_Wsk_PC.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());
		
		tableResponse.setBankerPlayer(-1);
		if(table._is_call_banker[seat_index] != 1)
		{	
			tableResponse.setCurrentPlayer(seat_index);
			
		}
		else{
			tableResponse.setCurrentPlayer(-1);
		}
			
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
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
			tableResponse.addCallBanker(table._is_call_banker[i]);
			
		}
		tableResponse.setLiangCard(table._jiao_pai_card);
		tableResponse.setIsYiDaSan(table._is_yi_da_san);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);
		if (table.istrustee[seat_index] == true)
			table.handler_request_trustee(seat_index, false, 0);
		if (table._game_status == GameConstants.GS_PCWSK_PLAY) {
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.WSK_PC_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}
		return true;
	}
}
