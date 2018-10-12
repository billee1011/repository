package com.cai.game.hjk.handler.hjk;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerQuest;

public  class HJKHandlerQuest_hjk extends HJKHandlerQuest{

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
		case GameConstants.HJK_QUEST_BNAKER:{
			int i =1; 
			for(;i<table.getTablePlayerNumber();i++){
				int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
				if(table._player_status[temp_pop] == false )
				{
					continue;
				}
				if(table._open_card[i] == true)
				{
					continue;
				}
				if(table._make_pass_button[i] != true)
				{
					continue;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_TOU_XIANG);
				tempPlayerStatus.add_action(GameConstants.HJK_NO_TOU_XIANG);
			}
			if(i == table.getTablePlayerNumber()){
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
			}
			
			return true;
		}
		case GameConstants.HJK_NO_QUEST_BNAKER:{
			PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
			tempPlayerStatus.add_action(GameConstants.HJK_YAO);
			tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_TOU_XIANG:{
			table._open_card[seat_index] = true;
			for(int i= 0 ; i< table.getTablePlayerNumber();i++){
				table._temp_score[i] = 0;
			}
			table._temp_score[seat_index] = -table._add_Jetton[seat_index];
			table._temp_score[table._cur_banker] =  table._temp_score[seat_index]; 
			table.open_card_hjk(seat_index);
			int i =1; 
			for(;i<table.getTablePlayerNumber();i++){
				int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
				if(table._player_status[temp_pop] == false )
				{
					continue;
				}
				if(table._open_card[i] == true)
				{
					continue;
				}
				if(table._make_pass_button[i] != true)
				{
					continue;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_TOU_XIANG);
				tempPlayerStatus.add_action(GameConstants.HJK_NO_TOU_XIANG);
			}
			if(i == table.getTablePlayerNumber()){
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
			}
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_NO_TOU_XIANG:{
			int i =1; 
			for(;i<table.getTablePlayerNumber();i++){
				int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
				if(table._player_status[temp_pop] == false )
				{
					continue;
				}
				if(table._open_card[i] == true)
				{
					continue;
				}
				if(table._make_pass_button[i] != true)
				{
					continue;
				}
				PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
				tempPlayerStatus.add_action(GameConstants.HJK_TOU_XIANG);
				tempPlayerStatus.add_action(GameConstants.HJK_NO_TOU_XIANG);
			}
			if(i == table.getTablePlayerNumber()){
				PlayerStatus tempPlayerStatus = table._playerStatus[table._cur_banker]; 
				tempPlayerStatus.add_action(GameConstants.HJK_YAO);
				tempPlayerStatus.add_action(GameConstants.HJK_ALL_OPEN);
			}
			table.operate_card_date_hjk();
			return true;
		}
		case GameConstants.HJK_YAO:
		case GameConstants.HJK_PASS:
		{
			table.dispatch_operate_button(seat_index, operate_code);
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
	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
	
       

		return true;
	}

}
