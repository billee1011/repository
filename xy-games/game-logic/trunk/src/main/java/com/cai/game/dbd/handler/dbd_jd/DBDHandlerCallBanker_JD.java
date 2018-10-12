package com.cai.game.dbd.handler.dbd_jd;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DDZAutoCallbankerRunnable;
import com.cai.future.runnable.DDZOutCardHandleRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.dbd.handler.DBDHandlerCallBanker;
import com.cai.game.dbd.handler.DBDHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;

import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.dbd.DbdRsp.CallBankerResult;
import protobuf.clazz.dbd.DbdRsp.RoomInfoDbd;
import protobuf.clazz.dbd.DbdRsp.TableResponseDBD;

public class DBDHandlerCallBanker_JD extends DBDHandlerCallBanker<DBDTable_JD>{
	protected int _seat_index;
	protected int _game_status;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	

	
	public void reset_status(int seat_index,int game_status){
		_seat_index = seat_index;
		_game_status= game_status;
	}
	
	@Override
	public void exe(DBDTable_JD table) {
		table._game_status = GameConstants.GS_CALL_BANKER;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBD_CALL_BANKER);
		CallBankerResult.Builder call_banker_dbd = CallBankerResult.newBuilder();
		call_banker_dbd.setCallPlayer(GameConstants.INVALID_SEAT);
		call_banker_dbd.setCurrentPlayer(_seat_index);
		call_banker_dbd.setBankerPlayer(GameConstants.INVALID_SEAT);
		call_banker_dbd.setCallScoreMin(table._call_banker_score_min);
		call_banker_dbd.setCallScoreMax(table._call_banker_score_current_max);
		call_banker_dbd.setCallScore(0);
		call_banker_dbd.setTangZiScore(table._tang_zi_score);
		call_banker_dbd.setDiChiScore(table._di_chi_score);
		call_banker_dbd.setCallScoreCurrent(table._call_banker_score_current);
		
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_dbd));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
	}
	@Override
	public boolean handler_player_be_in_room(DBDTable_JD table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBD_RECONNECT_DATA);

		TableResponseDBD.Builder tableResponse_dbd = TableResponseDBD.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_dbd);
		RoomInfoDbd.Builder room_info = table.getRoomInfoDbd();
		tableResponse_dbd.setRoomInfo(room_info);
			
		tableResponse_dbd.setBankerPlayer(table.GRR._banker_player);
		tableResponse_dbd.setCurrentPlayer(table._current_player);
		tableResponse_dbd.setPrevPlayer(table._prev_palyer);
		
		

		for(int i=0;i<table.getTablePlayerNumber();i++){
			tableResponse_dbd.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_dbd.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
			for(int j=0;j<table.GRR._cur_round_count[i];j++){
				if(table.GRR._cur_round_count[i]>0){
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_round_data[i][j]);
				}
			}
			if(i == seat_index){
				for(int j=0;j<table.GRR._card_count[i];j++){
					cards_card.addItem(table.GRR._cards_data[i][j]);
				}
			}
			tableResponse_dbd.addHandCardCount(table.GRR._card_count[i]);
			tableResponse_dbd.addHandCardsData(cards_card);
			tableResponse_dbd.addOutCardsData(i,out_cards);
			tableResponse_dbd.addOutChangeCardsData(out_change_cards);
			tableResponse_dbd.addUserCallBankerScore(table._user_call_banker_socre[i]);
			
		}
		if(table._current_player == seat_index){
			tableResponse_dbd.setIsFirstOut(2);
			tableResponse_dbd.setCurPlayerYaPai(1);
		}else{
			tableResponse_dbd.setIsFirstOut(0);
			tableResponse_dbd.setCurPlayerYaPai(0);
		}
		// 手牌--将自己的手牌数据发给自己
		
		for(int i=0;i<table._turn_out_card_count;i++){
			if(table._turn_out_card_count>0){
				tableResponse_dbd.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_dbd.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		if(table._current_player == seat_index){
			tableResponse_dbd.setCallScoreMax(table._call_banker_score_current_max);
			tableResponse_dbd.setCallScoreMin(table._call_banker_score_min);
			tableResponse_dbd.setCallScoreCurrent(table._call_banker_score_current);
		}
		tableResponse_dbd.setDiCardCount(table._di_pai_card_count);
		tableResponse_dbd.setPrCardsCount(table._turn_out_card_count);
		tableResponse_dbd.setPrOutCardType(table._turn_out_card_type);
		tableResponse_dbd.setPrOutCardPlayer(table._out_card_player);
		tableResponse_dbd.setTangZiScore(table._tang_zi_score);
		tableResponse_dbd.setDiChiScore(table._di_chi_score);
		tableResponse_dbd.setBankerScore(table._banker_call_score);
		if(table._turn_out_card_count == 0){
			tableResponse_dbd.setIsFirstOut(1);
		}else{
			tableResponse_dbd.setIsFirstOut(0);
		}
		
		tableResponse_dbd.setDisplayTime(10);
			
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_dbd));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}
	
	/**
	 * @param get_seat_index
	 * @param call_banker -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public  boolean handler_call_banker(DBDTable_JD table,int seat_index,int call_banker_score)
	{		
		if((call_banker_score<table._call_banker_score_min && call_banker_score != 0) || table._current_player != seat_index || table._game_status != GameConstants.GS_CALL_BANKER){
			return false;
		}
		if(call_banker_score > table._call_banker_score_current_max || call_banker_score>table._di_chi_score){
			return false;
		}
		if(call_banker_score%(table._tang_zi_score/2) != 0){
			return false;
		}
		
		if(call_banker_score != table._call_banker_score_current_max){
			
			if(call_banker_score != 0){
				table._cur_banker=seat_index;
				
			}
			int next_player=(seat_index+1)%table.getTablePlayerNumber();
			if(table._user_call_banker_socre[next_player] != -1){
				table._current_player=table._cur_banker;
			}else{
				table._current_player=next_player;
			}
			
			
		}else{
			table._cur_banker=seat_index;
			table._current_player=seat_index;
		}

		
		table._user_call_banker_socre[seat_index]=call_banker_score;
		
		if(table._cur_banker == table._current_player && table._cur_banker != GameConstants.INVALID_SEAT){
			table.GRR._banker_player=table._cur_banker;
			for(int i=table._all_card_len-table.GRR._left_card_count;i<table._all_card_len;i++){
				table.GRR._cards_data[table.GRR._banker_player][table.GRR._card_count[table.GRR._banker_player]]=table._repertory_card[i];
				table.GRR._card_count[table.GRR._banker_player]++;
			}
			table._logic.SortCardList(table.GRR._cards_data[table.GRR._banker_player], table.GRR._card_count[table.GRR._banker_player]);
			table._game_status=GameConstants.GS_MJ_PLAY;
			
			table.set_timer(table.ID_TIMER_CALLBANKER_FINISH, 2);
		}
		if(call_banker_score > 0){
			int last_player=(seat_index+table.getTablePlayerNumber()-1)%table.getTablePlayerNumber();
			if(table._user_call_banker_socre[last_player] > 0){
				table._di_chi_score+=table.game_cell;
				table._player_result.game_score[last_player]-=table.game_cell;
				table._end_score[table._cur_round-1][last_player]-=table.game_cell;
				table._call_tang_score[last_player]+=table.game_cell;
			}
		}else{
			table._di_chi_score+=table.game_cell;
			table._player_result.game_score[seat_index]-=table.game_cell;
			table._end_score[table._cur_round-1][seat_index]-=table.game_cell;
			table._call_tang_score[seat_index]+=table.game_cell;
		}
		

		table.operate_player_data();
		
		
		//刷新叫分限制
		if(table._call_banker_score_max > table._di_chi_score){
			table._call_banker_score_current_max=table._di_chi_score-table._di_chi_score%(table._tang_zi_score/2);
		}else{
			table._call_banker_score_current_max=table._call_banker_score_max;
		}
		if(call_banker_score != 0){
			table._call_banker_score_min=call_banker_score+(table._tang_zi_score/2);
			if(table._call_banker_score_min > table._call_banker_score_current_max){
				table._call_banker_score_min=table._call_banker_score_current_max;
			}
			table._call_banker_score_current=table._call_banker_score_min;
			table._banker_call_score=call_banker_score;
		}
		
		for(int player_index=0;player_index<table.getTablePlayerNumber();player_index++){
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DBD_CALL_BANKER);
			CallBankerResult.Builder call_banker_dbd = CallBankerResult.newBuilder();
			call_banker_dbd.setCallPlayer(seat_index);
			call_banker_dbd.setCurrentPlayer(table._current_player);
			call_banker_dbd.setBankerPlayer(table.GRR._banker_player);
			call_banker_dbd.setCallScoreMin(table._call_banker_score_min);
			call_banker_dbd.setCallScoreMax(table._call_banker_score_current_max);
			call_banker_dbd.setCallScoreCurrent(table._call_banker_score_current);
			call_banker_dbd.setCallScore(call_banker_score);
			call_banker_dbd.setBankerScore(table._banker_call_score);
			if(table.GRR._banker_player != GameConstants.INVALID_SEAT){
				call_banker_dbd.setCardCount(table.GRR._left_card_count);
				for(int i=table._all_card_len-table.GRR._left_card_count;i<table._all_card_len;i++){
					call_banker_dbd.addCardsData(table._repertory_card[i]);
				}
				
			}
			for(int i=0;i<table.getTablePlayerNumber();i++){
				call_banker_dbd.addUserCardCount(table.GRR._card_count[i]);
				Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
				if(i == player_index){
					for(int j=0;j<table.GRR._card_count[i];j++){
						cards_card.addItem(table.GRR._cards_data[i][j]);
					}
				}
				call_banker_dbd.addUserCardsData(cards_card);
			}
			call_banker_dbd.setTangZiScore(table._tang_zi_score);
			call_banker_dbd.setDiChiScore(table._di_chi_score);
			call_banker_dbd.setDisplayTime(table._call_banker_timer);
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_dbd));
			table.send_response_to_player(player_index, roomResponse);
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DBD_CALL_BANKER);
		CallBankerResult.Builder call_banker_dbd = CallBankerResult.newBuilder();
		call_banker_dbd.setCallPlayer(_seat_index);
		call_banker_dbd.setCurrentPlayer(table._current_player);
		call_banker_dbd.setBankerPlayer(table.GRR._banker_player);
		call_banker_dbd.setCallScoreMin(table._call_banker_score_min);
		call_banker_dbd.setCallScoreMax(table._call_banker_score_max);
		call_banker_dbd.setCallScore(call_banker_score);
		call_banker_dbd.setTangZiScore(table._tang_zi_score);
		call_banker_dbd.setDiChiScore(table._di_chi_score);
		call_banker_dbd.setBankerScore(table._banker_call_score);
		if(table.GRR._banker_player != GameConstants.INVALID_SEAT){
			call_banker_dbd.setCardCount(table.GRR._left_card_count);
			for(int i=table._all_card_len-table.GRR._left_card_count;i<table._all_card_len;i++){
				call_banker_dbd.addCardsData(table._repertory_card[i]);
			}
		}
		for(int i=0;i<table.getTablePlayerNumber();i++){
			call_banker_dbd.addUserCardCount(table.GRR._card_count[i]);
			Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
			for(int j=0;j<table.GRR._card_count[i];j++){
				cards_card.addItem(table.GRR._cards_data[i][j]);
			}
			call_banker_dbd.addUserCardsData(cards_card);
		}
		call_banker_dbd.setDisplayTime(table._call_banker_timer);
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_dbd));
		table.GRR.add_room_response(roomResponse);
		return true;
	}

}
