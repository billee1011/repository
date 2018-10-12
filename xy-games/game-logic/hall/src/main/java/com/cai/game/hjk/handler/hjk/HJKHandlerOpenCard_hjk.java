package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerOpenCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_OX;
import protobuf.clazz.Protocol.SendCard;
import protobuf.clazz.Protocol.TableResponseOX;

public class HJKHandlerOpenCard_hjk extends HJKHandlerOpenCard{

	
	@Override
	public void exe(HJKTable table) {
		
	}
	
	public boolean handler_operate_button(HJKTable table,int seat_index, int operate_code) {
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
			return true;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "HJKHandlerQuest_hjk 询问,玩家已操作");
			return true;
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
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
			for(int i = 0; i< GameConstants.GAME_PLAYER_HJK;i++)
			{
				if(table._player_status[i]){
					table._open_card[seat_index] = true;
				}
			}
			table.open_card_all_hjk(seat_index);
			boolean flag = true;
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
				if(table._player_status[i] == true)
				{
					if(table._open_card[i] == false)
						flag = false;
				}
			}
			//游戏结束
			if(flag == true){
				table.GRR._chi_hu_rights[seat_index].set_valid(true);


				table.process_ox_calulate_end();
				table.process_chi_calulate_score_ox();

			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

			}
		}
		case GameConstants.HJK_OPEN_CARD_ZERO:
		{
			if( 0 != table._cur_banker)
			{
				table.log_error("0用户是庄家不能开牌");
				return true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			table.caluate_score(0);
			table.open_card_hjk(0);
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


				table.process_ox_calulate_end();
				table.process_chi_calulate_score_ox();

			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

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
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_ONE:
		{
			if( 0 != table._cur_banker)
			{
				table.log_error("0用户是庄家不能开牌");
				return true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			table.caluate_score(0);
			table.open_card_hjk(0);
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


				table.process_ox_calulate_end();
				table.process_chi_calulate_score_ox();

			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

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
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_TWO:
		{
			if( 0 != table._cur_banker)
			{
				table.log_error("0用户是庄家不能开牌");
				return true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			table.caluate_score(0);
			table.open_card_hjk(0);
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
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_THREE:
		{
			if( 0 != table._cur_banker)
			{
				table.log_error("0用户是庄家不能开牌");
				return true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			table.caluate_score(0);
			table.open_card_hjk(0);
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


				table.process_ox_calulate_end();
				table.process_chi_calulate_score_ox();

			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

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
			return true;
		}
		case GameConstants.HJK_OPEN_CARD_FOUR:
		{
			if( 0 != table._cur_banker)
			{
				table.log_error("0用户是庄家不能开牌");
				return true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				table._temp_score[i] = 0;
			}
			table.caluate_score(0);
			table.open_card_hjk(0);
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


				table.process_ox_calulate_end();
				table.process_chi_calulate_score_ox();

			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);

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
			return true;
		}
		}
		return false;
	}
	
	
	/***
	 * 用户开牌
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	@Override
	public boolean handler_open_cards(HJKTable table, int seat_index,boolean open_flag) {
		if(_game_status != GameConstants.GS_OX_OPEN_CARD){
			table.log_error("游戏状态不对 "+ _game_status + "用户开牌 :" + GameConstants.GS_OX_OPEN_CARD);
			return false;
		}
		if(table._open_card[seat_index] == true){
			table.log_error("你已经开牌操作了 ");
			return false;
		}
		if(table._player_status[seat_index] == false){
			table.log_error("不能参与 这局游戏"+seat_index);
			return false;
		}
		if(open_flag == true)
			table._open_card[seat_index] = open_flag;
		else {
			table.log_error("open_flag 数据错误");
			return true;
		}
		if(seat_index != table._cur_banker)
		{
			table.open_card_hjk(seat_index);
		}
		else{
			for(int i = 0; i< GameConstants.GAME_PLAYER_HJK;i++)
			{
				if(table._player_status[i]){
					table._open_card[seat_index] = true;
				}
			}
			table.open_card_all_hjk(seat_index);
		}
	
		boolean flag = true;
		for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
			if(table._player_status[i] == true)
			{
				if(table._open_card[i] == false)
					flag = false;
			}
		}
		//游戏结束
		if(flag == true){
			table.GRR._chi_hu_rights[seat_index].set_valid(true);


			table.process_ox_calulate_end();
			table.process_chi_calulate_score_ox();

		
			table.countChiHuTimes(seat_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

		}
		
		return false;
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
