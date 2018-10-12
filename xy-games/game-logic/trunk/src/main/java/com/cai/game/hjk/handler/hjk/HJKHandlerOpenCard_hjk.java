package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerOpenCard;

import protobuf.clazz.Protocol.ButtonPop_HJK;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_HJK;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.SendCard_HJK;
import protobuf.clazz.Protocol.TableResponseOX;
import protobuf.clazz.Protocol.TableResponse_HJK;

public class HJKHandlerOpenCard_hjk extends HJKHandlerOpenCard{

	
	@Override
	public void exe(HJKTable table) {
		
	}
	
	public boolean handler_operate_button(HJKTable table,int seat_index, int operate_code) {
		if(seat_index <0 || seat_index>table.getTablePlayerNumber())
		{
			table.log_error("HJKHandlerOpenCard_hjk 坐位号不对:"+ seat_index);
			return false;
		}
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "HJKHandlerOpenCard_hjk 出牌,玩家操作已失效");
			return true;
		}
		// 效验操作
//		if ((operate_code != GameConstants.HJK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
//			table.log_error("HJKHandlerQuest_hjk 没有这个操作:" + operate_code);
//			return false;
//		}
//		// 是否已经响应
//		if (playerStatus.has_action() == false) {
//			table.log_player_error(seat_index, "HJKHandlerQuest_hjk 已经操作,玩家操作已失效");
//			return true;
//		}
//		// 是否已经响应
//		if (playerStatus.is_respone()) {
//			table.log_player_error(seat_index, "HJKHandlerQuest_hjk 询问,玩家已操作");
//			return true;
//		}
		if( seat_index != table._cur_banker)
		{
			table.log_error("0用户是庄家不能开牌");
			return true;
		}
		// 用户状态
		table._playerStatus[seat_index].clean_action();
		table._playerStatus[seat_index].clean_status();
		switch(operate_code){
		case GameConstants.HJK_QUEST_BNAKER:
		case GameConstants.HJK_NO_QUEST_BNAKER:
		case GameConstants.HJK_TOU_XIANG:
		case GameConstants.HJK_NO_TOU_XIANG:{
		    table.quest_operate_button(seat_index, operate_code);
			return true;
		}
		case GameConstants.HJK_YAO:
		case GameConstants.HJK_PASS:
		{
			table.dispatch_operate_button(seat_index, operate_code);
			return true;
		}
		case GameConstants.HJK_ALL_OPEN:
		{	
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			int i ;
			boolean open_card [] = new boolean[GameConstants.GAME_PLAYER_HJK];
			table._open_card[seat_index] = true;
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
				table._open_card[i] = true;
				open_card[i] = true;
				
			}
			table.open_all_card_hjk(open_card);
	
			//游戏结束

			table.countChiHuTimes(seat_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);
			return true;
			
		}
		case GameConstants.HJK_OPEN_CARD_ZERO:
		{
		
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			if(table._time == 1)
				table.caluate_score(0,false);
			else
				table.caluate_score(0,true);
			table.open_card_hjk(0,true);
			table._open_card[0] = true;
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(i == table._cur_banker)
						continue;
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table._open_card[table._cur_banker] = true;
				table.GRR._chi_hu_rights[seat_index].set_valid(true);


			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				return true;

			}
			table._make_pass_button[0] = false;
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
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_ONE:
		{
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			if(table._time == 1)
				table.caluate_score(1,false);
			else
				table.caluate_score(1,true);
			table.open_card_hjk(1,true);
			table._open_card[1] = true;
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(i == table._cur_banker)
						continue;
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table._open_card[table._cur_banker] = true;
				table.GRR._chi_hu_rights[seat_index].set_valid(true);



			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				return true;
			}
			table._make_pass_button[1] = false;
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
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_TWO:
		{
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			if(table._time == 1)
				table.caluate_score(2,false);
			else
				table.caluate_score(2,true);
			table.open_card_hjk(2,true);
			table._open_card[2] = true;
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(i == table._cur_banker)
						continue;
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table._open_card[table._cur_banker] = true;
				table.GRR._chi_hu_rights[seat_index].set_valid(true);
		
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				return true;

			}
			table._make_pass_button[2] = false;
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
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_THREE:
		{
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			if(table._time == 1)
				table.caluate_score(3,false);
			else
				table.caluate_score(3,true);
			table.open_card_hjk(3,true);
			table._open_card[3] = true;
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(i == table._cur_banker)
						continue;
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table._open_card[table._cur_banker] = true;
				table.GRR._chi_hu_rights[seat_index].set_valid(true);



			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				return true;

			}
			table._make_pass_button[3] = false;
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
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_FOUR:
		{
			// 用户状态
			table._playerStatus[seat_index].clean_action();
			table._playerStatus[seat_index].clean_status();
			
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			if(table._time == 1)
				table.caluate_score(4,false);
			else
				table.caluate_score(4,true);
			table.open_card_hjk(4,true);
			table._open_card[4] = true;
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(i == table._cur_banker)
						continue;
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table._open_card[table._cur_banker] = true;
				table.GRR._chi_hu_rights[seat_index].set_valid(true);
			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
				return true;

			}
			table._make_pass_button[4] = false;
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
			table.operate_card_date_hjk();
			return true;
		}
		}
		return false;
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
				else if(table._open_card[k] == true||(k==table._cur_banker &&  cur_opt_index == table._cur_banker)){
					for(int j = 0; j < table.GRR._card_count[k];j++){
		            	cards.addItem(table.GRR._cards_data[k][j]);
		            	
		            }
					
				}
				else{
					for(int j = 0; j < GameConstants.FRIST_DISPATCH_COUNT;j++){
		            	cards.addItem(GameConstants.BLACK_CARD);
		            	
		            }
					if(table.GRR._card_count[k] >=GameConstants.FRIST_DISPATCH_COUNT )	
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
