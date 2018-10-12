package com.cai.game.wsk.handler.hts;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.game.wsk.handler.AbstractWSKHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.hts.htsRsp.TableResponse_hts;
import protobuf.clazz.hts.htsRsp.call_banker_response_hts;
import protobuf.clazz.sxth.SxthRsp.TableResponse_Sxth;

public class HandlerCallBnaker_HTS extends AbstractWSKHandler<WSKTable_HTS> {

	protected int _cur_player = -1;
	
	public void reset_status(int seat_index) {
		_cur_player = seat_index;
	}
	
	@Override
	public void exe(WSKTable_HTS table) {
		if(_cur_player == -1){
			return ;
		}
		table._game_status = GameConstants.GS_HTS_CHENG_BAO;
		
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			table._player_result.pao[i] = -1;
		}
		table._current_player = _cur_player;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_CHENG_BAO);
		roomResponse.setGameStatus(table._game_status);
		call_banker_response_hts.Builder call_banker = call_banker_response_hts.newBuilder();
		call_banker.setCallSeat(-1);
		call_banker.setCurSeat(_cur_player);
		call_banker.setCallAction(-1);
		call_banker.setDisplayTime(10);
		call_banker.setZuoFei(table._logic.hts_zuo_fei);
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

	}
	

	public  boolean handler_call_banker(WSKTable_HTS table,int seat_index,int call_action){
		if(seat_index != _cur_player)
			return false;
		if(table._player_result.pao[seat_index] != -1){
			return false;
		}
		table._player_result.pao[seat_index] = call_action;
	
		//有人承包，结束承包
		if(call_action == 1){
			table.have_chengbao = true;
			table._cur_banker = seat_index;
			table.GRR._banker_player = seat_index;
			table.cheng_bao_player = seat_index;
			if(table.hei_san_player == table.cheng_bao_player){
				table._logic.hts_zuo_fei = true;
			}else{
				table._logic.hts_zuo_fei = false;
			}
			//发送承包消息
			RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
			roomResponse1.setType(MsgConstants.RESPONSE_WSK_HTS_CHENG_BAO);
			roomResponse1.setGameStatus(table._game_status);
			call_banker_response_hts.Builder call_banker = call_banker_response_hts.newBuilder();
			call_banker.setCallSeat(_cur_player);
			call_banker.setCurSeat(-1);
			call_banker.setCallAction(call_action);
			call_banker.setDisplayTime(10);
			call_banker.setZuoFei(table._logic.hts_zuo_fei);
			roomResponse1.setCommResponse(PBUtil.toByteString(call_banker));
			table.send_response_to_room(roomResponse1);
			table.GRR.add_room_response(roomResponse1);
			
			table.send_to_friend(1);
			table.out_card_begin();
			
			//黑桃三排在左边
			if(table.blipai[seat_index] == 1){
				table._logic.SortCardList(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], GameConstants.WSK_ST_510K);
			}else{
				table._logic.SortCardList(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], GameConstants.WSK_ST_VALUE);
			}
			table.RefreshCard(seat_index);
			
			return true;

		}else if(call_action == 0){
			int next_player = (_cur_player + 1) % table.getTablePlayerNumber(); 
			table._current_player = next_player;
			if(table._player_result.pao[next_player] != -1 ){
				table._logic.hts_zuo_fei = false;
				
				if(table.has_rule(GameConstants.GAME_RULE_MING_HTS)){
					table.show_hts_player = true;
					table.GRR._banker_player = table.hei_san_player;
					table.send_texiao(1, table.GRR._banker_player);
					table.send_to_friend(2);
				}else if(table.has_rule(GameConstants.GAME_RULE_AN_HTS)){
					table.GRR._banker_player = -1;
				}
				//发送承包消息
				RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
				roomResponse1.setType(MsgConstants.RESPONSE_WSK_HTS_CHENG_BAO);
				roomResponse1.setGameStatus(table._game_status);
				call_banker_response_hts.Builder call_banker = call_banker_response_hts.newBuilder();
				call_banker.setCallSeat(_cur_player);
				call_banker.setCurSeat(-1);
				call_banker.setCallAction(call_action);
				call_banker.setDisplayTime(10);
				call_banker.setZuoFei(table._logic.hts_zuo_fei);
				roomResponse1.setCommResponse(PBUtil.toByteString(call_banker));
				table.send_response_to_room(roomResponse1);
				table.GRR.add_room_response(roomResponse1);

				table.out_card_begin();
				
				return true;
			}
			//发送承包消息
			RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
			roomResponse1.setType(MsgConstants.RESPONSE_WSK_HTS_CHENG_BAO);
			roomResponse1.setGameStatus(table._game_status);
			call_banker_response_hts.Builder call_banker = call_banker_response_hts.newBuilder();
			call_banker.setCallSeat(_cur_player);
			call_banker.setCurSeat(next_player);
			call_banker.setCallAction(call_action);
			call_banker.setDisplayTime(10);
			roomResponse1.setCommResponse(PBUtil.toByteString(call_banker));
			table.send_response_to_room(roomResponse1);
			table.GRR.add_room_response(roomResponse1);
			_cur_player = next_player;
		}
		return true;
	}
	@Override
	public boolean handler_player_be_in_room(WSKTable_HTS table,int seat_index) {	
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_RECONNECT_DATA);
		TableResponse_hts.Builder tableResponse = TableResponse_hts.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_palyer);
		
		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addOpreateType(table._player_result.pao[i]);
			tableResponse.addOutCardType(table._cur_out_car_type[i]);
			tableResponse.addCardCount(table.GRR._card_count[i]);
			tableResponse.addOutCardCount(table._cur_out_card_count[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for(int j = 0;j < table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
		}
		if(table.show_hts_player){
			tableResponse.setHtsPlayer(table.hei_san_player);
		}else{
			tableResponse.setHtsPlayer(-1);
		}
		tableResponse.setZuoFei(table._logic.hts_zuo_fei);
		tableResponse.setLipaiType(table.blipai[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		
		
		//发送承包消息
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_WSK_HTS_CHENG_BAO);
		roomResponse1.setGameStatus(table._game_status);
		call_banker_response_hts.Builder call_banker = call_banker_response_hts.newBuilder();
		call_banker.setCallSeat(-1);
		call_banker.setCurSeat(_cur_player);
		call_banker.setCallAction(-1);
		call_banker.setDisplayTime(10);
		call_banker.setZuoFei(table._logic.hts_zuo_fei);
		roomResponse1.setCommResponse(PBUtil.toByteString(call_banker));
		table.send_response_to_room(roomResponse1);
		return true;
	}

}
