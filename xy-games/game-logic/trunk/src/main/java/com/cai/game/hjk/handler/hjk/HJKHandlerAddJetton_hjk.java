package com.cai.game.hjk.handler.hjk;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OpenCardRunnable;
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
			table.log_error("您下注已经越界了"+sub_jetton);
			return false;
		}
		if(table._jetton_info_cur[seat_index][sub_jetton] == 0)
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


		
		table._add_Jetton[seat_index] = table._jetton_info_cur[seat_index][sub_jetton];
		table.add_jetton_hjk(seat_index);
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
			table._last_banker = table._cur_banker;
			boolean open_card [] = new boolean[GameConstants.GAME_PLAYER_HJK];
			for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++ ){
				table._temp_score[i] = 0;
				open_card[i] = false;
			}
			
			if(table.is_open_card(table._cur_banker)){
				table._open_card[table._cur_banker] = true;
				
				for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++)
				{
					if(i == table._cur_banker)
						continue;
					if(table._player_status[i] == false)
						continue;
					table.is_open_card(i) ;
					table.caluate_score(i,false);
					table._open_card[i] = true;
					open_card [i] = true;
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
						table.caluate_score(i,false);
						table._open_card[i] = true;
						
					}
				}
				int j = 0;
				for(j = 1 ;j<GameConstants.GAME_PLAYER_HJK;j++){
					int temp_pop = (table._cur_banker + j)%GameConstants.GAME_PLAYER_HJK;
					if(table._player_status[temp_pop] == false )
					{
						continue;
					}
					if(table._open_card[temp_pop] == true)
					{
						continue;
					}
					PlayerStatus tempPlayerStatus = table._playerStatus[temp_pop]; 
					tempPlayerStatus.add_action(GameConstants.HJK_YAO);
					tempPlayerStatus.add_action(GameConstants.HJK_PASS);
					break;
				}
				if(j== GameConstants.GAME_PLAYER_HJK)
					table._open_card[table._cur_banker] = true;
			}
			for(int i = 0; i< table.getTablePlayerNumber();i++){
				if(table._player_status[i] == false)
					continue;
				int sum[] = new int[3];
				table._logic.get_card_sum(table.GRR._cards_data[i], table.GRR._card_count[i], sum);
				table.GRR._card_value_count[i] = 0;
				for(int j = 2; j>=0;j--)
				{
					if(sum[j]!=0)
					{
						table.GRR._card_value[i][table.GRR._card_value_count[i]++] = sum[j];
					}
				}
			}
			table.send_card_date_hjk();
			flag = true;
			for(int i = 0; i<table.getTablePlayerNumber();i++)
			{
			
				if(table._player_status[i] == false)
					continue;
					GameSchedule.put(new OpenCardRunnable(table.getRoom_id()),3, TimeUnit.SECONDS);
					flag = false;
					break;
			}
			if(flag == true)
				table.operate_card_date_hjk();
			
		}
		
		return true;
	}

	
	@Override
	public boolean handler_player_be_in_room(HJKTable table,int seat_index) {
		if(_game_status != GameConstants.GS_HJK_ADD_JETTON){
			table.log_error("游戏状态不对 "+ _game_status + "用户下注 :" + GameConstants.GS_HJK_ADD_JETTON);
			return false;
		}
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
		for(int i =0; i< GameConstants.GAME_PLAYER_HJK;i++){
			
			if((i == seat_index)&&(table._add_Jetton[i] == 0)&&(table._player_status[i] == true))
			{
				GameStart_HJK.Builder   game_start = GameStart_HJK.newBuilder();
				game_start.setCurBanker(table._cur_banker);
				
				if(seat_index != table._cur_banker)
				{
					for(int k = 0;k<table.getTablePlayerNumber();k++)
					{
						Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
						if((k!=table._cur_banker)&&(table._player_status[k] == true)&&(k==i))
						{
							for(int j = 0; j< 3; j++){
								cards.addItem(table._jetton_info_sever_ox[0][j]);
								table._jetton_info_cur[k][j] = table._jetton_info_sever_ox[0][j];
							}
							if(table._can_tuizhu_player[k]>0&&table._last_banker == table._cur_banker)
							{
								table._jetton_info_cur[k][3] = table._can_tuizhu_player[k];
								cards.addItem(table._jetton_info_cur[k][3]);
							}
							
						}
						game_start.addJettonCell(k,cards);
					}
				}
				roomResponse_hjk.setGameStart(game_start);
			}
			tableResponse.addAddJetter(table._add_Jetton[i]);
			tableResponse.addCallBankerInfo(table._call_banker[i]);
			if(table.get_players()[i] != null&&table._player_status[i] == false)
			{
				tableResponse.addWaitSeatIndex(1);
			}
			else
				tableResponse.addWaitSeatIndex(0);
		}
		
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
