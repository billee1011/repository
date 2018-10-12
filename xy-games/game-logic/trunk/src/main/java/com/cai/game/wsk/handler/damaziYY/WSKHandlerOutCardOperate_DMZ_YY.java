package com.cai.game.wsk.handler.damaziYY;

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
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;
import protobuf.clazz.dmz.DmzRsp.RoomInfoDmz;
import protobuf.clazz.dmz.DmzRsp.TableResponse_Dmz;
import protobuf.clazz.dmz.DmzRsp.TouXiang_Anser_Dmz;

public class WSKHandlerOutCardOperate_DMZ_YY extends WSKHandlerOutCardOperate<WSKTable_DMZ_YY> {
	
	@Override
	public void exe(WSKTable_DMZ_YY table) {
		if(_out_card_player != table._current_player){
			return;
		}
		//玩家不出
		if(_out_type == 0){
			user_pass_card(table);
			return;
		}
		
		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_ORDER);
		//出牌判断
		int card_type=adjust_out_card_right(table);
		if(card_type == GameConstants.DMZ_CT_ERROR){	
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		//桌面牌分
		table_pai_socre(table);
		

		if(table.GRR._card_count[_out_card_player]>5 && table.GRR._card_count[_out_card_player]-_out_card_count<=5){
			table._is_bao_jing[_out_card_player]=1;
		}
		table.GRR._card_count[_out_card_player]-=_out_card_count;
		//喜钱
		if(card_type>=GameConstants.DMZ_CT_BOMB_6 && table.has_rule(GameConstants.GAME_RULE_DMZ_YOU_XIQIAN)){
			table._xi_qian_times[_out_card_player]++;
			table._xi_qian_score[_out_card_player]+=table.game_cell*(table.getTablePlayerNumber()-1);
			long game_score=table.get_players()[_out_card_player].getGame_score()+(long)table.game_cell*(table.getTablePlayerNumber()-1);
			table.get_players()[_out_card_player].setGame_score(game_score);
			//table._player_result.game_score[_out_card_player]+=table.game_cell*(table.getTablePlayerNumber()-1);
			for(int i=0;i<table.getTablePlayerNumber();i++){
				if(i == _out_card_player){
					continue;
				}
				table._xi_qian_score[i]-=table.game_cell;
			}
			table.operate_player_data();
		}
		
		table._turn_have_score+=table._logic.GetCardScore(_out_cards_data,_out_card_count);
		table._turn_out_card_type=card_type;
		table._turn_out_card_count=_out_card_count;
		table._out_card_player=_out_card_player;
		table._prev_palyer=_out_card_player;
		table._cur_out_card_count[_out_card_player]=_out_card_count;
		Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
		for(int i=0;i<_out_card_count;i++){
			table._turn_out_card_data[i]=_out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i]=_out_cards_data[i];
		}
		//清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(table.GRR._card_count[next_player] == 0){
				// 显示出牌
				table._current_player=next_player;
				table._cur_out_card_count[next_player]=0;
				Arrays.fill(table._cur_out_card_data[next_player],GameConstants.INVALID_CARD);
				table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, table._turn_out_card_type,
							GameConstants.INVALID_SEAT,true);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			}else{
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player]=0;
		Arrays.fill(table._cur_out_card_data[next_player],GameConstants.INVALID_CARD);
		
		
		
		
		if(table.GRR._card_count[_out_card_player] == 0){
			for(int i=0;i<table.getTablePlayerNumber();i++){
				if(table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT){
					if(i == 0 && table.has_rule(GameConstants.GAME_RULE_DMZ_ZUODUI)){
						table._shangyou_account_id=table.get_players()[_out_card_player].getAccount_id();
					}
					table._chuwan_shunxu[i]=_out_card_player;
					break;
				}
			}
		}
		
		
		if(table.GRR._card_count[(_out_card_player+1)%table.getTablePlayerNumber()] == 0 && table.GRR._card_count[(_out_card_player+3)%table.getTablePlayerNumber()] == 0){
			table._current_player=GameConstants.INVALID_SEAT;
			int delay=3;
			for(int j=0;j<table.getTablePlayerNumber();j++){
				if(table.GRR._card_count[j] != 0){
					for(int i=0;i<table.getTablePlayerNumber();i++){
						if(table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT){
							table._chuwan_shunxu[i]=j;
							break;
						}
					}
				}
				
				table._cur_out_card_count[j]=0;
				Arrays.fill(table._cur_out_card_data[j],GameConstants.INVALID_CARD);
			}
			table._get_score[table._out_card_player]+=table._turn_have_score;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);
				

		}
		
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, table._turn_out_card_type,
				GameConstants.INVALID_SEAT,false);
		table._is_shou_chu=0;
		
		table.Refresh_pai_score(GameConstants.INVALID_SEAT);
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);

	}
	public void table_pai_socre(WSKTable_DMZ_YY table){
		int pai_score=0;
		int remove_card[]=new int[table.get_hand_card_count_max()];
		int remove_count=0;
		for(int i=0;i<_out_card_count;i++){
			int value=table._logic.GetCardLogicValue(_out_cards_data[i]);
			if(value == 5 || value == 10 || value == 13){
				remove_card[remove_count++]=_out_cards_data[i];
			}
			if(value == 5){
				pai_score+=5;
			}else if(value == 10 || value == 13){
				pai_score+=10;
			}
		}
		if(!table._logic.RemoveCard(remove_card, remove_count, table._pai_score_card, table._pai_score_count)){
//			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
//			return;	
		}
		table._pai_score_count-=remove_count;
		table._pai_score-=pai_score;
	}
	public void user_pass_card(WSKTable_DMZ_YY table){
		if(table._turn_out_card_count == 0){
			return;
		}
		//清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player){
				// 显示出牌
				table._current_player=next_player;
				table._cur_out_card_count[next_player]=0;
				Arrays.fill(table._cur_out_card_data[next_player],GameConstants.INVALID_CARD);
				table.operate_out_card(next_player, 0, null, table._turn_out_card_type,
							GameConstants.INVALID_SEAT,true);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			}else{
				break;
			}
		}
		
		//一轮不出
		if(next_player == table._out_card_player){
			//清空桌面牌分
			table._get_score[table._out_card_player]+=table._turn_have_score;
			table._turn_have_score=0;
			table._turn_out_card_count=0;
			
			Arrays.fill(table._turn_out_card_data,GameConstants.INVALID_CARD);
			if(table.GRR._card_count[next_player] == 0){
				table._current_player=next_player;
				table._cur_out_card_count[next_player]=0;
				Arrays.fill(table._cur_out_card_data[next_player],GameConstants.INVALID_CARD);
				table.operate_out_card(next_player, 0, null, table._turn_out_card_type,
						GameConstants.INVALID_SEAT,true);
				
				
				
				if(table.GRR._card_count[(next_player+2)%table.getTablePlayerNumber()] == 0){
					table._current_player=GameConstants.INVALID_SEAT;
					int delay=3;
					if(table.GRR._card_count[(next_player+2)%table.getTablePlayerNumber()] == 0){
						
						for(int j=0;j<table.getTablePlayerNumber();j++){
							if(table.GRR._card_count[j] != 0){
								for(int i=0;i<table.getTablePlayerNumber();i++){
									if(table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT){
										table._chuwan_shunxu[i]=j;
										break;
									}
								}
							}
							table._cur_out_card_count[j]=0;
							Arrays.fill(table._cur_out_card_data[j],GameConstants.INVALID_CARD);
						}
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL),
								delay, TimeUnit.SECONDS);
					}
				}else{
					next_player = (next_player + 1) % table.getTablePlayerNumber();
					for(int i=0;i<table.getTablePlayerNumber();i++){
						if(table.GRR._card_count[next_player] == 0){
							next_player = (next_player + 1) % table.getTablePlayerNumber();
						}else{
							break;
						}
					}
					table._current_player=next_player;
				}
				
			}else{
				table._current_player=next_player;
				table._prev_palyer=_out_card_player;
				table._cur_out_card_count[table._current_player]=0;
				Arrays.fill(table._cur_out_card_data[table._current_player],GameConstants.INVALID_CARD);
			}
		}else{
			table._current_player = next_player;
			table._prev_palyer=_out_card_player;
			table._cur_out_card_count[table._current_player]=0;
			Arrays.fill(table._cur_out_card_data[table._current_player],GameConstants.INVALID_CARD);
		}
		
		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null,GameConstants.DMZ_CT_PASS,
				GameConstants.INVALID_SEAT,false);
		
		if(table._turn_out_card_count == 0){
			table._is_shou_chu=1;
		}
		table.Refresh_user_get_score(GameConstants.INVALID_SEAT);
	}
	public int adjust_out_card_right(WSKTable_DMZ_YY table){
		int card_type=table._logic.GetCardType_DMZ(_out_cards_data, _out_card_count);
		if(card_type == GameConstants.DMZ_CT_ERROR){
			return GameConstants.DMZ_CT_ERROR;
		}
		if(table._turn_out_card_count!=0){
			if(!table._logic.CompareCard_DMZ(table._turn_out_card_data,_out_cards_data, table._turn_out_card_count,  _out_card_count)){
				return GameConstants.DMZ_CT_ERROR;
			}
		}
		if(!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player])){
			return GameConstants.DMZ_CT_ERROR;
		}
		
		return card_type;
	}
	
	@Override
	public boolean handler_player_be_in_room(WSKTable_DMZ_YY table,int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DMZ_RECONNECT_DATA);

		TableResponse_Dmz.Builder tableResponse_dmz = TableResponse_Dmz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_dmz);
		RoomInfoDmz.Builder room_info = table.getRoomInfoDmz();
		tableResponse_dmz.setRoomInfo(room_info);
			
		tableResponse_dmz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_dmz.setCurrentPlayer(table._current_player);
		tableResponse_dmz.setPrevPlayer(table._prev_palyer);
		tableResponse_dmz.setPrOutCardPlayer(table._out_card_player);
		tableResponse_dmz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_dmz.setPrOutCardType(table._turn_out_card_type);
		if(table._turn_out_card_count == 0 && seat_index == table._current_player){
			tableResponse_dmz.setIsFirstOut(1);
		}else{
			tableResponse_dmz.setIsFirstOut(0);
		}
		for(int i=0;i<table._turn_out_card_count;i++){
			tableResponse_dmz.addPrCardsData(table._turn_out_card_data[i]);
		}
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(table.GRR._card_count[i] <= 5 || i == seat_index){
				tableResponse_dmz.addCardCount(table.GRR._card_count[i]);
			}else{
				tableResponse_dmz.addCardCount(-1);
			}
			
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if(seat_index == i || table._is_ming_pai[i] == 1 || (table.GRR._card_count[seat_index] == 0 && i == (seat_index+2)%table.getTablePlayerNumber())){
				for(int j=0;j<table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}else if(table._is_ming_pai[seat_index]==1 && i == (seat_index+2)%table.getTablePlayerNumber() ){
				for(int j=0;j<table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for(int j=0;j<table._cur_out_card_count[i];j++){
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse_dmz.addOutCardsData(cur_out_cards);
			tableResponse_dmz.addCardsData(cards);
			tableResponse_dmz.addMingPaiStatus(table._is_ming_pai[i]);
			tableResponse_dmz.addWinOrder(table._chuwan_shunxu[i]);
		}
		if(table._is_ming_pai[(seat_index+2)%table.getTablePlayerNumber()] == 0){
			
		}
		if(table._is_ming_pai[(seat_index+2)%table.getTablePlayerNumber()] == 0){
			tableResponse_dmz.setMingPaiStr("对家请求明牌是否同意？");
		}else{
			tableResponse_dmz.setMingPaiStr("");
		}
		Int32ArrayResponse.Builder friend_cards_card=Int32ArrayResponse.newBuilder();
		if(table._is_ming_pai[seat_index] == 1 || table._is_ming_pai[(seat_index+2)%table.getTablePlayerNumber()] == 1){
			for(int j=0;j<table.GRR._card_count[(seat_index+2)%table.getTablePlayerNumber()];j++){
				friend_cards_card.addItem(table.GRR._cards_data[(seat_index+2)%table.getTablePlayerNumber()][j]);
			}
		}
		tableResponse_dmz.setFriendCardsData(friend_cards_card);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_dmz));
		table.send_response_to_player(seat_index, roomResponse);
		
		table.Refresh_pai_score(seat_index);
		table.Refresh_user_get_score(seat_index);
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(table._is_tou_xiang[i] == 1){
				roomResponse.setType(MsgConstants.RESPONSE_DMZ_TOUXIANG_ANSER);
				// 发送数据
				TouXiang_Anser_Dmz.Builder tou_xiang_anser = TouXiang_Anser_Dmz.newBuilder();
				tou_xiang_anser.setOpreateSeatIndex(seat_index);
				if(seat_index == (i+2)%table.getTablePlayerNumber()){
					tou_xiang_anser.setOpreateStr("您的对家["+table.get_players()[i].getNick_name()+"]请求投降，您是否同意投降(投降输一分)");
				}else{
					tou_xiang_anser.setOpreateStr("您的对手["+table.get_players()[i].getNick_name()+"]请求投降，您是否接受对手的投降(接受投降赢一分)");
				}
				roomResponse.setCommResponse(PBUtil.toByteString(tou_xiang_anser));
				// 自己才有牌数据
				table.send_response_to_player(seat_index, roomResponse);
				break;
			}
		}
		
		
		return true;
	}
	
}
