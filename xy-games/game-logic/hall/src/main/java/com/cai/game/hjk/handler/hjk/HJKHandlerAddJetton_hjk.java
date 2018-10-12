package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hjk.HJKTable;
import com.cai.game.hjk.handler.HJKHandlerAddJetton;

import protobuf.clazz.Protocol.GameStart_HJK;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_HJK;
import protobuf.clazz.Protocol.TableResponse_HJK;

public class HJKHandlerAddJetton_hjk extends HJKHandlerAddJetton{

	
	@Override
	public void exe(HJKTable table) {
		
	}
	/***
	 * 用户下注
	 * 
	 * @param seat_index
	 * @param open_flag
	 */
	public boolean handler_add_jetton(HJKTable table, int seat_index, int sub_jetton) {
		
		if(_game_status != GameConstants.GS_OX_ADD_JETTON){
			table.log_error("游戏状态不对 "+ _game_status + "用户下注 :" + GameConstants.GS_OX_ADD_JETTON);
			return false;
		}
		if(table._add_Jetton[seat_index] != 0){
			table.log_error("你已经开牌操作了 ");
			return false;
		}
		if(sub_jetton<0||sub_jetton>3)
		{
			table.log_error("您下注已经越界了");
			return false;
		}
		if(table._jetton_info_cur[sub_jetton] == 0)
		{
			table.log_error("您下注为0 了sub_jetton"+sub_jetton);
			return false;
		}
		if(seat_index == table._cur_banker){
			table.log_error("庄家不用下注");
			return false;
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}
		if(sub_jetton == 3){
			table._can_tuizhu_player[seat_index] =-1;
		}
		else{
			table._can_tuizhu_player[seat_index] = 0;
		}
		table._add_Jetton[seat_index] = table._jetton_info_cur[sub_jetton];
		table.add_jetton_hjk(seat_index);
		if(table.is_open_card(seat_index) == true)
		{
			table._open_card[seat_index] = true;
		}

		if(table._open_card[seat_index]== true)
		{
			table.caluate_score(seat_index);			
		}
		boolean flag = true;
		for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
			if(table._player_status[i] == true)
			{
				if(i == table._cur_banker)
					continue;
				if(table._add_Jetton[i] == 0)
					flag = false;
			}
		}
		//游戏结束
		if(flag == true){
			if(table.is_open_card(table._cur_banker)){
				table._open_card[table._cur_banker] = true;
				for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
					table._temp_score[i] = 0;
				}
				for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++)
				{
					if(i == table._cur_banker)
						continue;
					if(table._player_status[i] == false)
						continue;
					table.caluate_score(i);
					table._open_card[i] = true;
				}
			}else
			{
				for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
					table._temp_score[i] = 0;
				}
				for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++)
				{
					if(i == table._cur_banker)
						continue;
					if(table._player_status[i] == false)
						continue;
					if(table.is_open_card(i) == true)
					{
						table.caluate_score(i);
						table._open_card[i] = true;
						
					}
				}
				for(int i = 1 ;i<GameConstants.GAME_PLAYER_HJK;i++){
					int temp_pop = (table._cur_banker + i)%GameConstants.GAME_PLAYER_HJK;
					if(table._player_status[temp_pop] == false )
					{
						continue;
					}
					if(table._open_card[i] == true)
					{
						continue;
					}
					PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
					tempPlayerStatus.add_action(GameConstants.HJK_PASS);
				}
			}
			if(table._open_card[table._cur_banker] != true){
			
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++){
				if(table._player_status[i] == false)
					continue;
				int sum[] = new int[3];
				int temp = table._logic.get_card_sum(table.GRR._cards_data[i], table.GRR._card_count[i], sum);
				table.GRR._card_value_count[i] = 0;
				for(int j = 2; j>=0;j--)
				{
					if(temp > sum[j])
					{
						table.GRR._card_value[i][table.GRR._card_value_count[i]++] = sum[j];
					}
				}
				table.GRR._card_value[i][table.GRR._card_value_count[i]++] = temp;
			}
			table.send_card_date_hjk();
			table.operate_card_date_hjk();
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				if(table._open_card[i] == true)
				{
					table.open_card_hjk(i);
				}
			}
			if(table._open_card[table._cur_banker] == true){
				table.GRR._chi_hu_rights[seat_index].set_valid(true);


			
				table.countChiHuTimes(seat_index, true);

				int delay = GameConstants.GAME_FINISH_DELAY_FLS;
				if (table.GRR._chi_hu_rights[seat_index].type_count > 2) {
					delay += table.GRR._chi_hu_rights[seat_index].type_count - 2;
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), seat_index, GameConstants.Game_End_NORMAL),
						delay, TimeUnit.SECONDS);
			}

		}
		
		return true;
	}

	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		RoomResponse_HJK.Builder roomResponse_hjk = RoomResponse_HJK.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse_HJK.Builder tableResponse = TableResponse_HJK.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		
		
		// 游戏变量

		tableResponse.setCellScore(1);
		tableResponse.setSceneInfo(table._game_status);
		tableResponse.setBankerPlayer(table._cur_banker);
		tableResponse.setPlayerStatus(table._player_status[seat_index]);
		for(int i =0; i< GameConstants.GAME_PLAYER_OX;i++){
			
			if((i == seat_index)&&(table._add_Jetton[i] == 0)&&(table._player_status[i] == true))
			{
				GameStart_HJK.Builder   game_start = GameStart_HJK.newBuilder();
				game_start.setCurBanker(table._cur_banker);
				
				for(int k = 0;k<table.getTablePlayerNumber();k++)
				{
					Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
					if((k!=table._cur_banker)&&(table._player_status[k] == true))
					{
						for(int j = 0; j< 4; j++){
							cards.addItem(table._jetton_info_sever_ox[0][j]);
							table._jetton_info_cur[j] = table._jetton_info_sever_ox[0][j];
						}
						if(table._can_tuizhu_player[k]>0)
						{
							table._jetton_info_cur[4] = table._can_tuizhu_player[k];
							cards.addItem(table._jetton_info_cur[4]);
						}
						
					}
					game_start.addJettonCell(k,cards);
				}
				roomResponse_hjk.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
			tableResponse.addCallBankerInfo(table._call_banker[i]);
		}
	
		roomResponse_hjk.setTableResponse(tableResponse);
		roomResponse.setRoomResponseHjk(roomResponse_hjk);
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}
		
}
