package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.TableResponseOX;

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
			table.log_error("HJKHandlerQuest_hjk 坐位号不对:"+ seat_index);
			return false;
		}

			
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.HJK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("HJKHandlerQuest_hjk 没有这个操作:" + operate_code);
			return false;
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HJKHandlerQuest_hjk 已经操作,玩家操作已失效");
			return false;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HJKHandlerQuest_hjk 询问,玩家已操作");
			return false;
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		switch(operate_code){
		case GameConstants.HJK_YAO:
		{
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;
			table.GRR._cards_data[seat_index][table.GRR._card_count[seat_index]] = _send_card_data;
			table.GRR._card_count[seat_index]++;
			if(seat_index == table._cur_banker){
				int i = 0;
				if(table.is_open_card(seat_index)){
					for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
						table._temp_score[i] = 0;
					}
					for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++)
					{
						if(i == table._cur_banker)
							continue;
						if(table._player_status[i] == false)
							continue;
						table.caluate_score(i);
						table._open_card[i] = true;
					}
					table.GRR._chi_hu_rights[seat_index].set_valid(true);
	
	
					
					table.countChiHuTimes(seat_index, true);
	
					int delay = GameConstants.GAME_FINISH_DELAY_FLS;
					if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
						delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
					}
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
							delay, TimeUnit.SECONDS);
				}
				if(table.GRR._card_count[table._cur_banker] == 4)
				{
					PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
					tempPlayerStatus.add_action(GameConstants.HJK_QUEST_BNAKER);
					tempPlayerStatus.add_action(GameConstants.HJK_NO_QUEST_BNAKER);
				}
			}
			if(table.is_open_card(seat_index) == true)
			{
				int i;
				for( i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
					table._temp_score[i] = 0;
				}
				table.caluate_score(seat_index);
				table._open_card[seat_index] = true;
		
				table.open_card_hjk(seat_index);
				 
				for(i = 1;i<GameConstants.GAME_PLAYER_HJK;i++){
					int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
					if(table._player_status[temp_pop] == false )
					{
						continue;
					}
					if(table._open_card[i] == true)
					{
						continue;
					}
					if(table._make_pass_button[i] == true)
					{
						continue;
					}
					PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				}
				if(i == GameConstants.GAME_PLAYER_HJK){
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
						
			}
			else if(table.is_pass_button(seat_index) == true)
			{
				PlayerStatus tempPlayerStatus = table._playerStatus[seat_index]; 
				tempPlayerStatus.add_action(GameConstants.HJK_PASS);
			}
			else{
				int i;
				for(i =1; i<GameConstants.GAME_PLAYER_HJK;i++){
					int temp_pop = (table._cur_banker + i)%GameConstants.GAME_TYPE_HJK;
					if(table._player_status[temp_pop] == false )
					{
						continue;
					}
					if(table._open_card[i] == true)
					{
						continue;
					}
					if(table._make_pass_button[i] == true)
					{
						continue;
					}
					PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
					tempPlayerStatus.add_action(GameConstants.HJK_PASS);
				}
				if(i == GameConstants.GAME_PLAYER_HJK){
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
			}
			int sum[] = new int[3];
			int temp = table._logic.get_card_sum(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], sum);
			table.GRR._card_value_count[seat_index] = 0;
			for(int j = 2; j>=0;j--)
			{
				if(temp > sum[j])
				{
					table.GRR._card_value[seat_index][table.GRR._card_value_count[seat_index]++] = sum[j];
				}
			}
			table.GRR._card_value[seat_index][table.GRR._card_value_count[seat_index]++] = temp;
		table.yao_card_date_hjk(seat_index,_send_card_data);	
		table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_PASS:
		{
			table._make_pass_button[seat_index] =true;
	
			int i =1; 
			for(;i<GameConstants.GAME_PLAYER_HJK;i++){
				int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
				if(table._player_status[temp_pop] == false )
				{
					continue;
				}
				if(table._open_card[i] == true)
				{
					continue;
				}
				if(table._make_pass_button[i] == true)
				{
					continue;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_PASS);
			}
			if(i == GameConstants.GAME_PLAYER_HJK){
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
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
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_OX.Builder roomResponse_ox = RoomResponse_OX.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponseOX.Builder tableResponse = TableResponseOX.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		
		
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		SendCard.Builder send_card = SendCard.newBuilder();
		for(int k = 0; k < GameConstants.GAME_PLAYER_OX;k++)
		{
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if(table._player_status[k] != true)
			{
				for(int j = 0; j < GameConstants.OX_MAX_CARD_COUNT;j++){
	            	cards.addItem(GameConstants.INVALID_CARD);
	            	
	            }
				
			}
			else{
				if(k==seat_index){
					for(int j = 0; j < GameConstants.OX_MAX_CARD_COUNT;j++){
		            	cards.addItem(table.GRR._cards_data[k][j]);
		            	
		            }
				}
				else if(table._open_card[k] == true){
					for(int j = 0; j < GameConstants.OX_MAX_CARD_COUNT;j++){
		            	cards.addItem(table.GRR._cards_data[k][j]);
		            	
		            }
				}
				else{
					for(int j = 0; j < GameConstants.OX_MAX_CARD_COUNT;j++){
		            	cards.addItem(GameConstants.BLACK_CARD);
		            	
		            }
				}
			}
			tableResponse.addOpenCard(table._open_card[k]);
			
			tableResponse.addCardsData(k,cards);
			tableResponse.addCallBankerInfo(table._call_banker[k]);
			tableResponse.addAddJetter(table._add_Jetton[k]);
		}
		roomResponse_ox.setSendCard(send_card);
		roomResponse_ox.setTableResponseOx(tableResponse);
		roomResponse.setRoomResponseOx(roomResponse_ox);
		table.send_response_to_player(seat_index, roomResponse);


		return true;
	}

}
