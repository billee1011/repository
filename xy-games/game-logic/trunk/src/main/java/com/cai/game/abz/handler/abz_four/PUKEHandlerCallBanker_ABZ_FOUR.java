package com.cai.game.abz.handler.abz_four;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.abz.PUKETable;
import com.cai.game.abz.handler.PUKEHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.abz.AbzRsp.CallBankerResponse_ABZ;
import protobuf.clazz.abz.AbzRsp.TableResponse_Abz;

public class PUKEHandlerCallBanker_ABZ_FOUR extends PUKEHandlerCallBanker{
	protected int _seat_index;
	protected int _game_status;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	
	public PUKEHandlerCallBanker_ABZ_FOUR(){
	}
	
	public void reset_status(int seat_index,int game_status){
		_seat_index = seat_index;
		_game_status= game_status;
	}
	
	@Override
	public void exe(PUKETable table) {
		
		table._game_status = GameConstants.GS_ABZ_CALLBANKER;
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_CALL_BANKER);
		CallBankerResponse_ABZ.Builder call_banker = CallBankerResponse_ABZ.newBuilder();
		call_banker.setCurrentPlayer(table._current_player);
		call_banker.setCallPlayer(GameConstants.INVALID_SEAT);
		call_banker.setBankerPlayer(GameConstants.INVALID_SEAT);
		call_banker.setDisplayTime(10);
		call_banker.setBaoNum(table._bao_num);
		call_banker.setRoomInfo(table.getRoomInfo());
	
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		
		
//		table._auto_call_banker_scheduled=GameSchedule.put(new DDZAutoCallbankerRunnable(table.getRoom_id(), table,table._current_player,1),
//				dealy, TimeUnit.SECONDS);
	}
	
	


	
	@Override
	public boolean handler_player_be_in_room(PUKETable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_RECONNECT);

		TableResponse_Abz.Builder tableResponse = TableResponse_Abz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setBaoNum(table._bao_num);
		
		for(int i=0;i<table.getTablePlayerNumber();i++){
			tableResponse.addCallBankerAction(table._select_bao[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			if(i == seat_index){
				for(int j=0;j<table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
				
			}else if(table.GRR._banker_player != GameConstants.INVALID_SEAT && 
					table._select_bao[table.GRR._banker_player] == 1){
				for(int j=0;j<table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			
			if(i == seat_index){
				tableResponse.addCardCount(table.GRR._card_count[i]);
			}else{
				tableResponse.addCardCount(table.GRR._card_count[i]);
			}
			tableResponse.addChangCardData(table._chang_card[i]);
			tableResponse.addCardsData(cards);
		}
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setIsFirstOut(0);
		tableResponse.setIsCurrentYaPai(0);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}
	
	/**
	 * @param get_seat_index
	 * @param call_banker -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public  boolean handler_call_banker(PUKETable table,int seat_index,int call_action)
	{
		if(table._is_call_actin[seat_index]){
			return false;
		}
		if(!table.has_rule(GameConstants.GAME_RULE_ABZ_HAVE_BAI_BAO) && call_action == 1){
			return false;
		}
		boolean is_change=false;
		
		
		table._is_call_actin[seat_index]=true;
		table._select_bao[seat_index]=call_action;
		
		if(call_action != 0){
			table.GRR._banker_player=seat_index;
			table._current_player=GameConstants.INVALID_SEAT;
			table._bao_num=5;
			table._game_status = GameConstants.GS_ABZ_PLAY;
		}else{
			int next_player=(table._current_player+1)%table.getTablePlayerNumber();
			for(int i=0;i<table.getTablePlayerNumber();i++){
				if(table._is_call_actin[next_player]){
					next_player=(next_player+1)%table.getTablePlayerNumber();
				}
			}
			if(next_player == table._cur_banker){
				is_change =true;
				table._current_player=GameConstants.INVALID_SEAT;
			}else{
				table._current_player = next_player;
			}
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_ABZ_CALL_BANKER);
		CallBankerResponse_ABZ.Builder call_banker = CallBankerResponse_ABZ.newBuilder();
		call_banker.setCurrentPlayer(table._current_player);
		call_banker.setBankerPlayer(table.GRR._banker_player);
		call_banker.setCallPlayer(seat_index);
		call_banker.setBaoNum(table._bao_num);
		call_banker.setDisplayTime(10);
		call_banker.setOpreateAction(call_action);
		call_banker.setRoomInfo(table.getRoomInfo());
		
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		
		if(is_change){
			for(int i=0;i<table.getTablePlayerNumber();i++){
				table._select_bao[i]=-1;
			}
			table._current_player = table._cur_banker;
			if(table._handler_change_card != null){
				table._handler = table._handler_change_card;
				table._handler_change_card.exe(table);
			}
		}else if(table.GRR._banker_player != GameConstants.INVALID_SEAT){
			
			table._current_player=table.GRR._banker_player;
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.ABZ_CT_ERROR, GameConstants.INVALID_SEAT);
			table._handler=table._handler_out_card_operate;
//			for(int i=0;i<table.getTablePlayerNumber();i++){
//				table._select_bao[i]=-1;
//			}
		}
		return true;
	}

}
