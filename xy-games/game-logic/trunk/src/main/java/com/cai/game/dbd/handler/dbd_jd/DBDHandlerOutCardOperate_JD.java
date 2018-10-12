package com.cai.game.dbd.handler.dbd_jd;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.GameDescUtil;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.LengTuoZiRunnable;
import com.cai.game.dbd.handler.DBDHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.dbd.DbdRsp.RoomInfoDbd;
import protobuf.clazz.dbd.DbdRsp.TableResponseDBD;

public class DBDHandlerOutCardOperate_JD extends DBDHandlerOutCardOperate<DBDTable_JD> {
	
	@Override
	public void exe(DBDTable_JD table) {
		table._game_status=GameConstants.GS_MJ_PLAY;
		if(_out_card_player != table._current_player){
			return;
		}
		//玩家不出
		if(_out_type == 0){
			user_pass_card(table);
			return;	
		}
		//判断玩家出牌是否合法
		int card_type=adjust_outcard_type(table);
		if(card_type == GameConstants.DBD_CT_ERROR){
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		//所有玩家本轮出牌数据
		table.GRR._cur_round_count[_out_card_player]=0;
		Arrays.fill(table.GRR._cur_round_data[_out_card_player],GameConstants.INVALID_CARD);
		Arrays.fill(table.GRR._cur_change_round_data[_out_card_player],GameConstants.INVALID_CARD);
		table.GRR._cur_round_pass[_out_card_player]=0;
		for(int i=0;i<_out_card_count;i++){
			table.GRR._cur_round_data[_out_card_player][i]=_out_cards_data[i];
			table.GRR._cur_change_round_data[_out_card_player][i]=_out_change_cards_data[i];
		}
		table.GRR._cur_round_count[_out_card_player]=_out_card_count;
		table._out_card_times[_out_card_player]++;
		table._turn_out_card_type=card_type;
		//炸弹限制倍数
		if(card_type == GameConstants.DBD_CT_BOMB_3){
			table._boom_score+=table.game_cell;
		}else if(card_type == GameConstants.DBD_CT_BOMB_4_RUAN){
			table._boom_score+=2*table.game_cell;
		}else if(card_type == GameConstants.DBD_CT_BOMB_4_YING){
			table._boom_score+=3*table.game_cell;
		}
		if(table._turn_out_card_count != 0){
			Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
			for(int i=0;i<_out_card_count;i++){
				table._turn_out_card_data[i]=_out_change_cards_data[i];
				table._turn_out_real_card_data[i]=_out_cards_data[i];
			}
			table._turn_out_card_count=_out_card_count;	
		}else{
			Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_out_real_card_data,GameConstants.INVALID_CARD);
			for(int i=0;i<_out_card_count;i++){
				table._turn_out_card_data[i]=_out_change_cards_data[i];
				table._turn_out_real_card_data[i]=_out_cards_data[i];
			}
			table._turn_out_card_count=_out_card_count;
		}
		table.GRR._card_count[_out_card_player]-=_out_card_count;
		table._current_player=(_out_card_player+1)%table.getTablePlayerNumber();
		
		Arrays.fill(table.GRR._cur_round_data[table._current_player],GameConstants.INVALID_CARD);
		Arrays.fill(table.GRR._cur_change_round_data[table._current_player],GameConstants.INVALID_CARD);
		table.GRR._cur_round_count[table._current_player]=0;
		table.GRR._cur_round_pass[table._current_player]=0;
		if(table.GRR._card_count[_out_card_player] == 0){
			Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_round_data[_out_card_player],GameConstants.INVALID_CARD);
			Arrays.fill(table.GRR._cur_change_round_data[_out_card_player],GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_out_real_card_data,GameConstants.INVALID_CARD);
			table.GRR._cur_round_count[_out_card_player]=0;
			table._turn_out_card_count=0;
			table._cur_banker=_out_card_player;
			table._current_player = GameConstants.INVALID_SEAT;
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, card_type, GameConstants.INVALID_SEAT);
			table._out_card_player=_out_card_player;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
						2, TimeUnit.SECONDS);
		}else{
			table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, card_type, GameConstants.INVALID_SEAT);
			table._out_card_player=_out_card_player;
		}
	}
	
	public int adjust_outcard_type(DBDTable_JD table){
		//全是癞子不能出
		if(table._logic.isAllMagic(_out_cards_data, _out_card_count)){
			if(table.GRR._card_count[_out_card_player] != _out_card_count){
				return GameConstants.DBD_CT_ERROR;
			}
			if(table._turn_out_card_count != 0){
				return GameConstants.DBD_CT_ERROR;
			}
		}
		//判断癞子变牌是否合理
		if(!table._logic.is_have_card(_out_cards_data,_out_change_cards_data,_out_card_count)){
			return GameConstants.DBD_CT_ERROR;
		}
		//判断出牌牌型
		int card_type=table._logic.GetCardType_DBD(_out_change_cards_data,_out_cards_data, _out_card_count);
		if(card_type == GameConstants.DBD_CT_ERROR){
			return GameConstants.DBD_CT_ERROR;
		}
		if(table._turn_out_card_count != 0){
			if(!table._logic.comparecarddata(_out_change_cards_data,_out_cards_data, _out_card_count, table._turn_out_card_data,table._turn_out_real_card_data, table._turn_out_card_count)){
				return GameConstants.DBD_CT_ERROR;
			}
		}

		if(!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player],_out_cards_data, _out_card_count)){
			return GameConstants.DBD_CT_ERROR;
		}
		return card_type;
	}

	public void user_pass_card(DBDTable_JD table){
		if(table._turn_out_card_count==0){
			return;
		}
		//保存所有玩家本轮出牌数据
		table._current_player=(_out_card_player+1)%table.getTablePlayerNumber();
		table.GRR._cur_round_pass[_out_card_player]=1;
		table.GRR._cur_round_count[_out_card_player]=0;
		if(table._current_player == table._out_card_player){
			Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_out_real_card_data,GameConstants.INVALID_CARD);
			for(int i=0;i<table.getTablePlayerNumber();i++){
				table.GRR._cur_round_pass[i]=0;
				table.GRR._cur_round_count[i]=0;
				Arrays.fill(table.GRR._cur_round_data[i],GameConstants.INVALID_CARD);
				Arrays.fill(table.GRR._cur_change_round_data[i],GameConstants.INVALID_CARD);
			}
			table._turn_out_card_count=0;
		}
		
		Arrays.fill(table.GRR._cur_round_data[table._current_player],GameConstants.INVALID_CARD);
		Arrays.fill(table.GRR._cur_change_round_data[table._current_player],GameConstants.INVALID_CARD);
		table.operate_out_card(_out_card_player, _out_card_count, _out_cards_data, _out_type, GameConstants.INVALID_SEAT);
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
		tableResponse_dbd.setBankerScore(table._banker_call_score);
		tableResponse_dbd.setBombScore(table._boom_score);
		tableResponse_dbd.setDiChiScore(table._di_chi_score);
		

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
			tableResponse_dbd.addHandCardsData(cards_card);
			tableResponse_dbd.addHandCardCount(table.GRR._card_count[i]);
			tableResponse_dbd.addOutCardsData(i,out_cards);
			tableResponse_dbd.addOutChangeCardsData(out_change_cards);
		}
		
		// 手牌--将自己的手牌数据发给自己
		
		Int32ArrayResponse.Builder cards_card=Int32ArrayResponse.newBuilder();
		for(int j=0;j<table.GRR._card_count[seat_index];j++){
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		for(int i=0;i<table._turn_out_card_count;i++){
			if(table._turn_out_card_count>0){
				tableResponse_dbd.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_dbd.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		for(int i=0;i<table._di_pai_card_count;i++){
			tableResponse_dbd.addDiCardsData(table._di_pai_card_data[i]);
		}
		tableResponse_dbd.setDiCardCount(table._di_pai_card_count);
		tableResponse_dbd.setPrCardsCount(table._turn_out_card_count);
		tableResponse_dbd.setPrOutCardType(table._turn_out_card_type);
		tableResponse_dbd.setPrOutCardPlayer(table._out_card_player);
		if(table._current_player == seat_index){
			if(table._turn_out_card_count == 0){
				tableResponse_dbd.setCurPlayerYaPai(1);
				tableResponse_dbd.setIsFirstOut(1);
			}else{
				if(table._logic.search_card_data(table._turn_out_card_data,table._turn_out_real_card_data,table._turn_out_card_count, table.GRR._cards_data[table._current_player], table.GRR._card_count[table._current_player])){
					tableResponse_dbd.setCurPlayerYaPai(1);
				}else{
					tableResponse_dbd.setCurPlayerYaPai(0);
				}
				tableResponse_dbd.setIsFirstOut(0);
			}
			
		}else{
			tableResponse_dbd.setIsFirstOut(0);
			tableResponse_dbd.setCurPlayerYaPai(0);
		}
		
		
		tableResponse_dbd.setDisplayTime(10);
			
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_dbd));
		table.send_response_to_player(seat_index, roomResponse);
		
		
		
		
		return true;
	}
	
}
