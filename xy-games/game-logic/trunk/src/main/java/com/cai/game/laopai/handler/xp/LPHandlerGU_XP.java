package com.cai.game.laopai.handler.xp;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.laopai.handler.AbstractLPHandler;


import protobuf.clazz.Protocol.GameStartResponse;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class LPHandlerGU_XP extends AbstractLPHandler<LPTable_XP> {
	private int _seat_index;
	private int _send_card_data;
	@Override
	public void exe(LPTable_XP table) {
		
		table._game_status = GameConstants.GS_MJ_NAO;// 设置状态
		// TODO Auto-generated method stub
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
		table.load_room_info_data(roomResponse);
		
		//有上庄
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._player_result.nao[i]=-1;
		}
		table.operate_player_data();
//		for (int i = 0; i < MJtable.getTablePlayerNumber(); i++) {
//			handler_pao_qiang(table,i,0,0);
//		}
		
		
//		return;
		// 发送数据
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if(i == table.GRR._banker_player){
				roomResponse.setGameStatus(GameConstants.GS_MJ_WAIT_GU_CHOU);
			}else{
				roomResponse.setGameStatus(table._game_status);
			}
			roomResponse.setTarget(i);
			roomResponse.setNao(table._player_result.nao[i]);//table._player_result.pao[i]
			roomResponse.setNaodes("当前可以闹庄");
			table.send_response_to_player(i, roomResponse);
		}
		
		
	}
	public void SetSeatIndex(int seat_index,int card_data){
		_seat_index=seat_index;
		_send_card_data=card_data;
	}
	public boolean handler_nao(LPTable_XP table,int seat_index, int nao) {
		if(table._game_status != GameConstants.GS_MJ_NAO || seat_index == table.GRR._banker_player){
			return true;
		}
		for(int i = 0; i < table.getTablePlayerNumber(); i++){
			if(table._player_result.nao[i]>0){

				return true;
			}
		}
		table._playerStatus[seat_index]._is_nao_zhuang = true;
		int n = table._player_result.nao[seat_index];
		
		table._player_result.nao[seat_index] = nao;
		
		if(n!=nao){
			table.operate_player_data();
		}

		


		if(nao>0){
			table._game_status = GameConstants.GS_MJ_PLAY;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
			table.load_room_info_data(roomResponse);
			roomResponse.setGameStatus(table._game_status);
			table.send_response_to_room(roomResponse);
			table.set_handler_out_card_operate();
			if(seat_index == table._current_player){
				if(table._player_result.nao[seat_index] > 0){
					int next_player =  (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					//发牌
					table.exe_dispatch_card(next_player,GameConstants.WIK_NULL,GameConstants.DELAY_SEND_CARD_DELAY);
					return true;
				}
			}
			

			// 设置变量
			table._provide_card = _send_card_data;// 提供的牌
	
			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			if(curPlayerStatus.has_action()){//有动作
				//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				if(curPlayerStatus.has_zi_mo() ){
					table.GRR._chi_hu_rights[_seat_index].set_valid(true);
					
					table.GRR._chi_hu_card[_seat_index][0] = _send_card_data;
					table.process_chi_hu_player_operate(_seat_index, _send_card_data,true);
					table.process_chi_hu_player_score_xp(_seat_index,_seat_index,_send_card_data, true);
					// 记录
					table._player_result.zi_mo_count[_seat_index]++;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
							GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				}else{
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index,false);
				}
				
			}else{
				//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}else{
			for(int i = 0; i < table.getTablePlayerNumber(); i++){
				if(!table._playerStatus[i]._is_nao_zhuang && i != table.GRR._banker_player){
					RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
					roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
					table.load_room_info_data(roomResponse);
					roomResponse.setGameStatus(GameConstants.GS_MJ_WAIT_GU_CHOU);
					table.send_response_to_player(seat_index, roomResponse);
					return true;
				}
			}
			table._game_status = GameConstants.GS_MJ_PLAY;
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
			table.load_room_info_data(roomResponse);
			roomResponse.setGameStatus(table._game_status);
			table.send_response_to_room(roomResponse);
			table.set_handler_out_card_operate();
			if(table._player_result.nao[_seat_index] > 0){
				int next_player =  (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				//发牌
				table.exe_dispatch_card(next_player,GameConstants.WIK_NULL,GameConstants.DELAY_SEND_CARD_DELAY);
			}

			// 设置变量
			table._provide_card = _send_card_data;// 提供的牌

			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			if(curPlayerStatus.has_action()){//有动作
				//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				if(curPlayerStatus.has_zi_mo() ){
					table.GRR._chi_hu_rights[_seat_index].set_valid(true);
					
					table.GRR._chi_hu_card[_seat_index][0] = _send_card_data;
					table.process_chi_hu_player_operate(_seat_index, _send_card_data,true);
					table.process_chi_hu_player_score_xp(_seat_index,_seat_index,_send_card_data, true);
					// 记录
					table._player_result.zi_mo_count[_seat_index]++;
					GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
							GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				}else{
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index,false);
				}
				
			}else{
				//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}
		}
		
		
		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(LPTable_XP table,int seat_index) {

		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		if(seat_index == table.GRR._banker_player || table._player_result.nao[seat_index]>-1){
			table._game_status=GameConstants.GS_MJ_WAIT_GU_CHOU;
		}	
		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);

		table.load_common_status(roomResponse);
		table._game_status=GameConstants.GS_MJ_NAO;
		
		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		//tableResponse.setActionMask((_response[seat_index] == false) ? _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addTrustee(false);// 是否托管
			// 剩余牌数
			tableResponse.addDiscardCount(table.GRR._discard_count[i]);
			Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < 55; j++) {
				if(table._logic.is_magic_card(table.GRR._discard_cards[i][j])){
					//癞子
					int_array.addItem(table.GRR._discard_cards[i][j]+GameConstants.CARD_ESPECIAL_TYPE_HUN);
				}else{
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
			}
			tableResponse.addDiscardCards(int_array);

			// 组合扑克
			tableResponse.addWeaveCount(table.GRR._weave_count[i]);
			WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
			for (int j = 0; j < GameConstants.MAX_WEAVE_LAOPAI; j++) {
				WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
				if(table._logic.is_magic_card(table.GRR._weave_items[i][j].center_card)){
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card+GameConstants.CARD_ESPECIAL_TYPE_HUN);
				}else{
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player+ GameConstants.WEAVE_SHOW_DIRECT);
				weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
				weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);

			//
			tableResponse.addWinnerOrder(0);

			// 牌
			
			if(i == _seat_index){
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i])-1);
			}else{
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
			}
			
			
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_card_count=table._logic.get_card_count_by_index(table.GRR._cards_index[seat_index]);
		int cards[] = new int[hand_card_count];
		table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
		

		//如果断线重连的人是自己
		if(seat_index == _seat_index){
			table._logic.remove_card_by_data(cards, _send_card_data);
			hand_card_count--;
		}



		
		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(cards[i]);
		}

		roomResponse.setTable(tableResponse);
		table.send_response_to_player(seat_index, roomResponse);
		
		//癞子
		int real_card = _send_card_data;
		if(table._logic.is_magic_card(_send_card_data)){
			real_card+=GameConstants.CARD_ESPECIAL_TYPE_HUN;
		}
		//摸牌
		table.operate_player_get_card(_seat_index, 1, new int[]{real_card},seat_index);
		
		
		//听牌显示
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		


		return true;
	}
	
	private void player_reconnect(LPTable_XP table,int seat_index){
		if(table._playerStatus[seat_index]._is_nao_zhuang == true || seat_index == table.GRR._banker_player){
			return;
		}
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_NAO_ACTION);
		table.load_room_info_data(roomResponse);
		// 发送数据
		
		roomResponse.setTarget(seat_index);
		roomResponse.setNao(table._player_result.nao[seat_index]);//table._player_result.pao[i]
		roomResponse.setNaodes("当前可以闹庄");
		table.send_response_to_player(seat_index, roomResponse);
		
		//table.load_room_info_data(roomResponse);
		//table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);	
		table.send_response_to_player(seat_index, roomResponse);
	}

}
