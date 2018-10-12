package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerDispatchCard;

import protobuf.clazz.Protocol.ButtonPop_HJK;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_HJK;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.SendCard_HJK;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.TableResponse_HJK;

public class HJKHandlerDispatchCard_hjk extends HJKHandlerDispatchCard{
	
	@Override
	public void exe(HJKTable table) {
		

		
	}
	/***
	 * //用户发牌
	 */
	public boolean handler_operate_button(HJKTable table, int seat_index, int operate_code) {
		if(seat_index <0 || seat_index>table.getTablePlayerNumber())
		{
			table.log_error("HJKHandlerDispatchCard_hjk 坐位号不对:"+ seat_index);
			return false;
		}
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HJKHandlerDispatchCard_hjk 出牌,玩家操作已失效");
			return true;
		}
		switch(operate_code){
		case GameConstants.HJK_YAO:
		{
			// 用户状态
			
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;
			table.GRR._cards_data[seat_index][table.GRR._card_count[seat_index]] = _send_card_data;
			table.GRR._card_count[seat_index]++;
			if(seat_index == table._cur_banker){
				int i = 0;
				
				if(table.is_open_card(seat_index)){
					table.yao_card_date_hjk(seat_index,_send_card_data);
					boolean open_card[] =  new boolean[GameConstants.GAME_PLAYER_HJK] ;
					for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
						table._temp_score[i] = 0;
						open_card[i] = false;
					}
					for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++)
					{
						if(i == table._cur_banker)
							continue;
						if(table._player_status[i] == false)
							continue;
						if(table._make_pass_button[i] ==false)
							continue;
						if(table._time == 1)
							table.caluate_score(i,false);
						else
							table.caluate_score(i,true);
						open_card[i] = true;
						table._open_card[i] = true;
					}
					table._open_card[table._cur_banker] = true;
					table.open_all_card_hjk(open_card);
					table.GRR._chi_hu_rights[seat_index].set_valid(true);
	
	
					
					table.countChiHuTimes(seat_index, true);
	
					int delay = 3;
				
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
							delay, TimeUnit.SECONDS);
					return true;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
				if(table.GRR._card_count[table._cur_banker] == 4)
				{
					
					tempPlayerStatus.add_action(GameConstants.HJK_QUEST_BNAKER);
					tempPlayerStatus.add_action(GameConstants.HJK_NO_QUEST_BNAKER);
				}else{
					for(int j = 0; j<table.getTablePlayerNumber();j++)
					{
						switch(j){
						case 0:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ZERO);
							break;
						case 1:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ONE);
							break;
						case 2:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_TWO);
							break;
						case 3:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_THREE);
							break;
						case 4:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_FOUR);
							break;
						}
					}
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
					tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
				}
				int sum[] = new int[3];
				int temp =  table._logic.get_card_sum(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], sum);
				table.GRR._card_value_count[seat_index] = 0;
				for(int j = 2; j>=0;j--)
				{
					if(sum[j]!=0&&sum[j]<=temp)
					{
						table.GRR._card_value[seat_index][table.GRR._card_value_count[seat_index]++] = sum[j];
					}
				}
				table.yao_card_date_hjk(seat_index,_send_card_data);
				table.operate_card_date_hjk();
				return true;
				
			}
			if(table.is_open_card(seat_index) == true)
			{
				int i;
				for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
					table._temp_score[i] = 0;
				}
				if(table._time == 1)
					table.caluate_score(seat_index,false);
				else
					table.caluate_score(seat_index,true);
				table._open_card[seat_index] = true;
		
				
				 
				boolean game_end = true;
				for(i = 1;i<GameConstants.GAME_PLAYER_HJK;i++){
					int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
					if(table._player_status[temp_pop] == false )
					{
						continue;
					}
					if(table._open_card[temp_pop] == true)
					{
						continue;
					}
					if(table._make_pass_button[temp_pop] == true)
					{
						game_end = false;
						continue;
					}
					PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
					tempPlayerStatus.add_action(GameConstants.HJK_PASS);
					
					
					break;
					
				}
				if(i == GameConstants.GAME_PLAYER_HJK){
					int sum[] = new int[3];
					int temp =  table._logic.get_card_sum(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], sum);
					table.GRR._card_value_count[seat_index] = 0;
					for(int j = 2; j>=0;j--)
					{
						if(sum[j]!=0&&sum[j]<=temp)
						{
							table.GRR._card_value[seat_index][table.GRR._card_value_count[seat_index]++] = sum[j];
						}
					}
					table.yao_card_date_hjk(seat_index,_send_card_data);	
					if(table._open_card[seat_index] == true)
						table.open_card_hjk(seat_index,true);
					table.open_card_hjk(table._cur_banker, false);
					PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
					for(int j = 0; j<table.getTablePlayerNumber();j++)
					{
						switch(j){
						case 0:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ZERO);
							break;
						case 1:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ONE);
							break;
						case 2:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_TWO);
							break;
						case 3:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_THREE);
							break;
						case 4:
							if(table._make_pass_button[j] == true)
								tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_FOUR);
							break;
						}
					}
					if(game_end == false){
						tempPlayerStatus.add_action(GameConstants.HJK_YAO);
						tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
					}
					else
					{
						
						table.countChiHuTimes(seat_index, true);
		
						int delay = 3;
					
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
								delay, TimeUnit.SECONDS);
						return true;
					}
				
					table.operate_card_date_hjk();
					return true;
				}
						
			}
			else if(table.is_pass_button(seat_index) == true)
			{
				PlayerStatus tempPlayerStatus = table._playerStatus[seat_index]; 
				tempPlayerStatus.add_action(GameConstants.HJK_PASS);
			}
			else{
				
				int temp_pop = seat_index;
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_PASS);	
			}
			int sum[] = new int[3];
			int temp =  table._logic.get_card_sum(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], sum);
			table.GRR._card_value_count[seat_index] = 0;
			for(int j = 2; j>=0;j--)
			{
				if(sum[j]!=0&&sum[j]<=temp)
				{
					table.GRR._card_value[seat_index][table.GRR._card_value_count[seat_index]++] = sum[j];
				}
			}
		table.yao_card_date_hjk(seat_index,_send_card_data);
		if(table._open_card[seat_index] == true)
			table.open_card_hjk(seat_index,true);
		table.operate_card_date_hjk();
		
			return true;
		}
		case GameConstants.HJK_PASS:
		{
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
		
			table._make_pass_button[seat_index] =true;
	
			int i =1; 
			for(;i<GameConstants.GAME_PLAYER_HJK;i++){
				int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
				if(table._player_status[temp_pop] == false )
				{
					continue;
				}
				if(table._open_card[temp_pop] == true)
				{
					continue;
				}
				if(table._make_pass_button[temp_pop] == true)
				{
					continue;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_PASS);
				break;
			}
			if(i == GameConstants.GAME_PLAYER_HJK){
				table.open_card_hjk(table._cur_banker, false);
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
				for(int j = 0; j<table.getTablePlayerNumber();j++)
				{
					switch(j){
					case 0:
						if(table._make_pass_button[j] == true)
							tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ZERO);
						break;
					case 1:
						if(table._make_pass_button[j] == true)
							tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_ONE);
						break;
					case 2:
						if(table._make_pass_button[j] == true)
							tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_TWO);
						break;
					case 3:
						if(table._make_pass_button[j] == true)
							tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_THREE);
						break;
					case 4:
						if(table._make_pass_button[j] == true)
							tempPlayerStatus.add_action(GameConstants.HJK_OPEN_CARD_FOUR);
						break;
					}
				}
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
			}
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_QUEST_BNAKER:
		case GameConstants.HJK_NO_QUEST_BNAKER:
		case GameConstants.HJK_TOU_XIANG:
		case GameConstants.HJK_NO_TOU_XIANG:
		{ 
			table.quest_operate_button(seat_index,operate_code);
			return true;
		}
		case GameConstants.HJK_OPEN_CARD:
		case GameConstants.HJK_ALL_OPEN:
		case GameConstants.HJK_OPEN_CARD_ZERO:
		case GameConstants.HJK_OPEN_CARD_ONE:
		case GameConstants.HJK_OPEN_CARD_TWO:
		case GameConstants.HJK_OPEN_CARD_THREE:
		case GameConstants.HJK_OPEN_CARD_FOUR:
		{
			table.opencard_operate_button(seat_index, operate_code);
			return true;
		}
		}
		return false;
	}
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HJKTable table,int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		
		


		return true;
	}
	
	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
		if(table._game_status != GameConstants.GS_HJK_YAO_CARD){
			table.log_error("游戏状态不对 "+ table._game_status + "用户开牌 :" + GameConstants.GS_HJK_YAO_CARD);
			return false;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_HJK.Builder tableResponse = TableResponse_HJK.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		
		int cur_opt_index  = -1;
		for(int i = 0 ; i< GameConstants.GAME_PLAYER_HJK;i++){
			PlayerStatus curPlayerStatus = table._playerStatus[i];
			if(curPlayerStatus._action_count > 0){
				cur_opt_index = i ; 
				break;
			}
		}
		ButtonPop_HJK.Builder button_pop = ButtonPop_HJK.newBuilder(); 
		for (int i = 0; i < GameConstants.GAME_PLAYER_HJK; i++) 
		{
			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			if(tempPlayerStatus._action_count>0)
				button_pop.addSeatIndex(i);
		}
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		tableResponse.setBankerPlayer(table._cur_banker);
		SendCard_HJK.Builder send_card = SendCard_HJK.newBuilder();
		for(int k = 0; k < GameConstants.GAME_PLAYER_HJK;k++)
		{
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder card_value = Int32ArrayResponse.newBuilder();
			if(table._player_status[k] != true)
			{
				for(int j = 0; j < GameConstants.MAX_COUNT_HJK;j++){
	            	cards.addItem(GameConstants.INVALID_CARD);
	            	
	            }
				
			}
			else{
				if(k==seat_index){
					for(int j = 0; j < table.GRR._card_count[k];j++){
		            	cards.addItem(table.GRR._cards_data[k][j]);
		            	
		            }
					if(table._open_card[k]  == false)
					{
						
						for(int j = 0; j< table.GRR._card_value_count[k];j++)
						{
							card_value.addItem(table.GRR._card_value[k][j]);
						}
						
					}
				}
				else if(table._open_card[k] == true || (k==table._cur_banker &&  cur_opt_index == table._cur_banker)){
					for(int j = 0; j < table.GRR._card_count[k];j++){
		            	cards.addItem(table.GRR._cards_data[k][j]);
		            	
		            }
					for(int j = 0; j< table.GRR._card_value_count[k];j++)
					{
						card_value.addItem(table.GRR._card_value[k][j]);
					}
					
				}
				else{
					for(int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT;j++){
		            	cards.addItem(GameConstants.BLACK_CARD);
		            	
		            }
					if(table.GRR._card_count[k] >GameConstants.FRIST_DISPATCH_COUNT )	
					{
						for(int j = GameConstants.FRIST_DISPATCH_COUNT; j< table.GRR._card_count[k];j++)
						{
							cards.addItem(table.GRR._cards_data[k][j]);
						}
					}
				}
			}
			if(table._open_card[k] == true)
				tableResponse.addCardType(table._card_type[k]);
			else
				tableResponse.addCardType(0);
			tableResponse.addIsTouXiang(table._no_tou_xiang_player[k]);
			tableResponse.addOpenCard(table._open_card[k]);
			tableResponse.addCardsData(k,cards);
			tableResponse.addCardValue(k,card_value);
			tableResponse.addCallBankerInfo(table._call_banker[k]);
			tableResponse.addAddJetter(table._add_Jetton[k]);
			tableResponse.addPlayerScore((int)(table.GRR._game_score[k]));
			if(table.get_players()[k] != null&&table._player_status[k] == false)
			{
				tableResponse.addWaitSeatIndex(1);
			}
			else
				tableResponse.addWaitSeatIndex(0);	
			
		}
		PlayerStatus curPlayerStatus = table._playerStatus[seat_index];
		for (int j = 0; j < curPlayerStatus._action_count; j++) {
			button_pop.addActions(curPlayerStatus._action[j]);
		} 
		roomResponse_hjk.setButtonPop(button_pop);	
		roomResponse_hjk.setSendCard(send_card);
		roomResponse_hjk.setTableResponse(tableResponse);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		table.send_response_to_player(seat_index, roomResponse);
		if(table.get_players()[seat_index] != null && table._player_status[seat_index] == false){
			RoomResponse.Builder roomResponsewait = RoomResponse.newBuilder();
			roomResponsewait.setType(MsgConstants.RESPONSE_WAIT_RESPONSE);
			RoomResponse_HJK.Builder roomResponsewait_hjk = RoomResponse_HJK.newBuilder();
			roomResponsewait_hjk.setWaitSeatIndex(seat_index);
			roomResponsewait.setRoomResponseHjk(roomResponsewait_hjk);
			table.send_response_to_other(seat_index, roomResponsewait);
		}


		return true;
	}

}
