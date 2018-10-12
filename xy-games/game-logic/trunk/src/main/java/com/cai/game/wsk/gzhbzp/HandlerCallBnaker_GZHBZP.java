package com.cai.game.wsk.gzhbzp;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gzhbzp.gzhbzpRsp.CallBankerResponse_gzhbzp;
import protobuf.clazz.gzhbzp.gzhbzpRsp.TableResponse_gzhbzp;


public class HandlerCallBnaker_GZHBZP extends AbstractHandler_GZHBZP {
	@Override
	public void exe(Table_GZHBZP table) {
	}

	/**
	 * 接收客户端发的独牌操作
	 * 
	 * @param table
	 * @param seat_index
	 * @param call_action
	 *            0不独，1独牌
	 * @return
	 */
	public boolean handler_call_banker(Table_GZHBZP table, int seat_index, int call_action) {
		if (table._is_call_banker[seat_index] == 1 || table._current_player != seat_index) {
			return false;
		}

		table._is_call_banker[seat_index] = 1;
		table._call_banker_opreate[seat_index] = call_action;
		boolean is_finish = true;
        boolean flag = false;
		if (call_action == 0) {
			int next_player = (table._current_player + 1) % table.getTablePlayerNumber();

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._is_call_banker[next_player] == 0) {
					is_finish = false;

					table._cur_banker = next_player;
					table._current_player = next_player;

					break;
				}
			}

			if (is_finish) {
				// 所有人已经操作了‘不独’之后，开始叫牌
				table._game_status = GameConstants.GS_TC_WSK_LIANG_PAI;
				table._current_player = (table._current_player + 1) % table.getTablePlayerNumber();
				table.GRR._banker_player = table._current_player;
				table._cur_banker = table._current_player;
			}
			else{
				table._current_player = next_player;
				flag = true;

			}
		} else if(call_action == 1){
			// 如果有人点了‘独牌’。牌局的庄家和牌桌的当前玩家，变成点了‘独牌’操作的玩家

			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;		
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
				if (table._is_call_banker[next_player] == 0) {
					is_finish = false;

					break;
				}
			}

			if (is_finish) {
				int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
				table._is_call_banker[next_player] = 0;
				table._call_banker_opreate[next_player] = -1;
				table._current_player = next_player;
				flag = true;
			}
			else{
				int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				flag = true;
			}

			
		} else if(call_action == 2){
			
			int count = 3;
			for(int k = 0; k < table.getTablePlayerNumber();k++){
				if(table.GRR._banker_player == k)
					continue;
				if(table._current_player == k)
					continue;
				table._chuwan_shunxu[count--] = k;
				
			}
			table._game_status = GameConstants.GS_TC_WSK_PLAY;
			table._is_yi_da_yi = true;
			table._current_player  = table.GRR._banker_player;
			table._cur_banker = table.GRR._banker_player; 
			// 发送一个空的出牌消息，告诉客户端，开始打牌了
			table.set_handler(table._handler_out_card_operate);
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.TCDG_CT_ERROR, GameConstants.INVALID_SEAT, false);
		
			
		} else if(call_action == 3){
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
				if (table._is_call_banker[next_player] == 0) {
					is_finish = false;

					break;
				}
			}

			if (is_finish) {
				// 所有人已经操作了‘不独’之后，开始叫牌
				table._game_status = GameConstants.GS_TC_WSK_PLAY;
				table._current_player = table.GRR._banker_player;	
				table._is_yi_da_san = true;
				// 发送一个空的出牌消息，告诉客户端，开始打牌了
				table.set_handler(table._handler_out_card_operate);
				table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.TCDG_CT_ERROR, GameConstants.INVALID_SEAT, false);
			}
			else{
				int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
				table._current_player = next_player;
				flag = true;

			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_CALL_BANKER_RESULT);

		// 字段含义请参考tongchengWSK.proto文件
		CallBankerResponse_gzhbzp.Builder callbanker_result = CallBankerResponse_gzhbzp.newBuilder();
		if (table._game_status == GameConstants.GS_GZH_BZP_CALLBANKER) {
			callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
		} else {
			callbanker_result.setBankerPlayer(table.GRR._banker_player);
		}
		callbanker_result.setOpreateAction(call_action); // 0不叫-1叫
		callbanker_result.setCallPlayer(seat_index); // 操作玩家。如果为-1，表示开始叫庄

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		table.send_response_to_room(roomResponse);

		table.GRR.add_room_response(roomResponse);
		if(flag == true)
		{
			for(int i = 0; i< table.getTablePlayerNumber();i++)
				table.update_button(i,true);
		}
		if (table._game_status == GameConstants.GS_TC_WSK_LIANG_PAI&&table.getRuleValue(GameConstants.GAME_RULE_BZP_GZH_ZD_JP)==1) {
			// 如果牌桌状态是叫牌状态了，需要服务端主动处理叫牌
			table.deal_liang_pai(table._current_player);
		}
		else if(table._game_status == GameConstants.GS_TC_WSK_LIANG_PAI&&table.getRuleValue(GameConstants.GAME_RULE_BZP_GZH_SD_JP) == 1){
			table.update_button(table._current_player, true);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_GZHBZP table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_RECONNECT_DATA);

		TableResponse_gzhbzp.Builder tableResponse = TableResponse_gzhbzp.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
			tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setCurrentPlayer(table.GRR._banker_player);
			tableResponse.setBankerPlayer(table.GRR._banker_player);
		}
		tableResponse.setPrevPlayer(table._prev_player);
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

		if (table._out_card_ming_ji != GameConstants.INVALID_CARD && table.GRR._card_count[seat_index] == 0) {
			tableResponse.setFriendSeatIndex(table._friend_seat[seat_index]);
		} else {
			tableResponse.setFriendSeatIndex(GameConstants.INVALID_SEAT);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addCardCount(table.GRR._card_count[i]);

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();

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
		}

		if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse.addIsCallBanker(table._is_call_banker[i] == 1 ? true : false);
			}
		}

		if (table._out_card_ming_ji == GameConstants.INVALID_CARD) {
			tableResponse.setBankerFriendSeat(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setBankerFriendSeat(table._friend_seat[table.GRR._banker_player]);
		}

		tableResponse.setJiaoCardData(table._jiao_pai_card);
		tableResponse.setIsYiDaSan(table._is_yi_da_san);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.send_response_to_player(seat_index, roomResponse);

		table.refresh_user_get_score(seat_index);

		roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_CALL_BANKER_RESULT);

		CallBankerResponse_gzhbzp.Builder callbanker_result = CallBankerResponse_gzhbzp.newBuilder();
		callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
		callbanker_result.setOpreateAction(GameConstants.INVALID_SEAT); // 0不叫-1叫
		callbanker_result.setCallPlayer(seat_index); // 操作玩家。如果为-1，表示开始叫庄


		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		table.send_response_to_player(seat_index, roomResponse);
		table.update_button(table._current_player,false);
		return true;
	}

}
