package com.cai.game.ddz.handler.jdddz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.ddz.DDZTable;
import com.cai.game.ddz.handler.DDZHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class DDZHandlerOutCardOperate_JD extends DDZHandlerOutCardOperate {
	@Override
	public void exe(DDZTable table) {
		// TODO Auto-generated method stub
		PlayerStatus playerStatus = table._playerStatus[_out_card_player];
		playerStatus.reset();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		table._playerStatus[i].clean_action();
		table._playerStatus[i].clean_status();
		
		}
		//玩家不出
		if(this._b_out_card == 0){
			table.GRR._cur_round_count[_out_card_player] = 0;
			table.GRR._cur_round_pass[_out_card_player]=1;
			
			for(int i=0;i<table.get_hand_card_count_max();i++){
				table.GRR._cur_round_data[_out_card_player][i]=GameConstants.INVALID_CARD;
			}

			//判断下一个玩家
			int next_player = (_out_card_player + table.getPlayerCount() + 1) % table.getPlayerCount();
			table._current_player = next_player;
			if(table._current_player== table._out_card_player){
				//出完一圈牌
				table._turn_out_card_count=0;
				for(int i=0;i<this._out_card_count;i++){
					table._turn_out_card_data[i]=GameConstants.INVALID_CARD;
				}
				Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
				Arrays.fill(table.GRR._cur_round_count, 0);
				Arrays.fill(table.GRR._cur_round_pass, 0);
			}
			
			// 显示出牌
			table.operate_out_card(table._out_card_player, 0, _out_cards_data, GameConstants.DDZ_CT_ERROR,
					GameConstants.INVALID_SEAT);
			
			
			//通知客户端出牌用户
			for(int i=0;i<table.getTablePlayerNumber();i++){
				if(i == table._current_player){
					continue;
				}
				table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
			}
			table._playerStatus[table._current_player].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			return;
		}

		int cbCardType=table._logic.GetCardType(this._out_cards_data,this._out_card_count);
		if(cbCardType == GameConstants.DDZ_CT_ERROR){
			return;
		}
		if(table._turn_out_card_count>0){
			if(!table._logic.CompareCard(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count)){
				return;
			}
		}
		if(!table._logic.remove_cards_by_data(table.GRR._cards_data[_out_card_player], table.GRR._card_count[_out_card_player], this._out_cards_data, this._out_card_count)){
			return;
		}
		//保存当前出牌玩家
		table._out_card_player=_out_card_player;
		//保存该轮出牌信息
		table.GRR._cur_round_pass[_out_card_player]=0;
		table.GRR._cur_round_count[_out_card_player] = this._out_card_count;
		for(int i=0;i<this._out_card_count;i++){
			table.GRR._cur_round_data[_out_card_player][i]=this._out_cards_data[i];
			//保存该次出牌数据
			table._turn_out_card_data[i]=this._out_cards_data[i];
		}
		table._turn_out_card_count=this._out_card_count;
		table.GRR._card_count[_out_card_player] -=this._out_card_count;
		
		// 刷新手牌
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
       
	
		//清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + table.getPlayerCount() + 1) % table.getPlayerCount();
		table._current_player = next_player;
		table.GRR._cur_round_count[table._current_player]=0;
		table.GRR._cur_round_pass[table._current_player]=0;
		for(int j=0;j<this._out_card_count;j++){
			table.GRR._cur_round_data[table._current_player][j]=GameConstants.INVALID_CARD;
		}
		//刷新玩家手牌
		table.operate_player_cards();
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data, cbCardType,
				GameConstants.INVALID_SEAT);
		
		table.exe_add_discard(_out_card_player, _out_card_count, _out_cards_data, false,1);
		if(0 == table.GRR._card_count[_out_card_player]){
			int delay = 1;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);
			for(int i=0;i<table.getTablePlayerNumber();i++){
				table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
				table.operate_player_status();
			}
			return;
		}
		
		
		for(int i=0;i<table.getTablePlayerNumber();i++){
			if(i == table._current_player){
				continue;
			}
			table._playerStatus[i].set_status(GameConstants.Player_Status_NULL);
		}
		table._playerStatus[table._current_player].set_status(GameConstants.Player_Status_OUT_CARD);
		table.operate_player_status();
		
	}


	@Override
	public boolean handler_player_be_in_room(DDZTable table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_out_card_player);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);
		table.istrustee[seat_index]=false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			//
			tableResponse.addWinnerOrder(0);
		}

		// 数据
		tableResponse.setSendCardData(0);
		int cards[] = new int[GameConstants.MAX_HH_COUNT];

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(cards[i]);
		}
		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
	

		return true;
	}
}
