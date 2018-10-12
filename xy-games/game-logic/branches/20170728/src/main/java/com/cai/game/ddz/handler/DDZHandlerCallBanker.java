package com.cai.game.ddz.handler;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DDZOutCardHandleRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.ddz.DDZTable;
import com.cai.game.mj.MJTable;

import protobuf.clazz.Protocol.CallBankerDDZ;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.RoomResponse_DDZ;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class DDZHandlerCallBanker extends DDZHandler{
	protected int _seat_index;
	protected int _game_status;
	//private int _current_player =MJGameConstants.INVALID_SEAT; 
	
	protected GangCardResult m_gangCardResult;
	
	public DDZHandlerCallBanker(){
		m_gangCardResult = new GangCardResult();
	}
	
	public void reset_status(int seat_index,int game_status){
		_seat_index = seat_index;
		_game_status= game_status;
	}
	
	@Override
	public void exe(DDZTable table) {
		_game_status = GameConstants.GS_MJ_CALL_BANKER;// 设置状态
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		int call_banker_type=0;
		if(table.has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU)){
			call_banker_type=1;
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		}
		else if(table.has_rule(GameConstants.GAME_RULE_JIAO_FEN)){
			call_banker_type=2;
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_JIAO_FEN);
		}
		else{
			call_banker_type=3;
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		}
		
		
		table.load_room_info_data(roomResponse);
		table.load_common_status(roomResponse);

		if (table._cur_round == 1) {
			// shuffle_players();
			table.load_player_info_data(roomResponse);
		}
		
		CallBankerDDZ.Builder call_banker_ddz = CallBankerDDZ.newBuilder();
		int temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
		table._current_player = temp;
		this._seat_index=table._current_player ;
		call_banker_ddz.setCallCurrentPlayer(table._current_player);
		call_banker_ddz.setQiangCurrentPlayer(GameConstants.INVALID_SEAT);
		call_banker_ddz.setCallType(call_banker_type);
		call_banker_ddz.setDisplayTime(10);
		
		RoomResponse_DDZ.Builder roomResponse_ddz = RoomResponse_DDZ.newBuilder();
		roomResponse_ddz.setCallBanker(call_banker_ddz);
		roomResponse.setRoomResponseDdz(roomResponse_ddz);
		table.send_response_to_room(roomResponse);
	}
	
	


	
	@Override
	public boolean handler_player_be_in_room(DDZTable table,int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		
		
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
				int_array.addItem(table.GRR._discard_cards[i][j]);
			}
			tableResponse.addDiscardCards(int_array);
		}
	
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
//		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		
	
		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);
		
		table.send_response_to_player(seat_index, roomResponse);
		return true;
	}
	
	/**
	 * @param get_seat_index
	 * @param call_banker -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public  boolean handler_call_banker(DDZTable table,int seat_index,int call_banker,int qiang_bangker)
	{
		int call_banker_type=0;
		if(call_banker != -1){
			table._playerStatus[seat_index]._call_banker = call_banker;
		}
		if(qiang_bangker != -1){
			table._playerStatus[seat_index]._qiang_banker = qiang_bangker;
		}
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		//刷新用户叫分
		table.operate_player_data();
		int qiang_player=GameConstants.INVALID_SEAT;
		int call_player=GameConstants.INVALID_SEAT;
		
		if(table.has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU)){
			//叫地主
			if(call_banker > 0){
				table.GRR._banker_player=table._banker_select=seat_index;
				table._current_player=GameConstants.INVALID_SEAT;
				for(int i=0;i<table.getTablePlayerNumber();i++){
					table._playerStatus[i]._call_banker=-1;
					table.operate_player_data();
				}
				//出牌操作延迟
				GameSchedule.put(new DDZOutCardHandleRunnable(table.getRoom_id(), table.GRR._banker_player,table,table._handler_out_card_operate ),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
			//判断是否还有人没有轮到叫地主操作
			boolean bhave_no_call=false;
			for(int i=0;i<table.getTablePlayerNumber();i++){
				if(table._playerStatus[i]._call_banker == -1){
					bhave_no_call=true;
				}
			}
			if(!bhave_no_call){
				//叫地主结束
				for(int i=0;i<table.getTablePlayerNumber();i++){
					table._playerStatus[i]._call_banker=-1;
					table.operate_player_data();
				}
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
			table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			call_player=table._current_player;
			call_banker_type=1;
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		}
		else if(table.has_rule(GameConstants.GAME_RULE_JIAO_FEN)){
			//用户叫分
			if(call_banker==3){
				//玩家叫3分该玩家即为地主
				table.GRR._banker_player=table._banker_select=seat_index;
				for(int i=0;i<table.getTablePlayerNumber();i++){
					table._playerStatus[i]._call_banker=-1;
					table.operate_player_data();
				}
				//出牌操作延迟
				GameSchedule.put(new DDZOutCardHandleRunnable(table.getRoom_id(), table.GRR._banker_player,table,table._handler_out_card_operate ),
						GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
				return true;
			}
			else{
				//玩家叫分
				if(call_banker != 0){
					if(table._banker_select != GameConstants.INVALID_SEAT){
						if(table._playerStatus[table._banker_select]._call_banker<call_banker){
							table._banker_select=seat_index;
						}
					}
					else{
						table._banker_select=seat_index;
					}
				}
				//判断是否还有人没有轮到叫地主操作
				boolean bhave_no_call=false;
				for(int i=0;i<table.getTablePlayerNumber();i++){
					if(table._playerStatus[i]._call_banker == -1){
						bhave_no_call=true;
					}
				}
				if(!bhave_no_call){
					//叫地主结束
					for(int i=0;i<table.getTablePlayerNumber();i++){
						table._playerStatus[i]._call_banker=-1;
						table.operate_player_data();
					}
					if(table._banker_select == GameConstants.INVALID_SEAT){
						//没有玩家叫地主
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
								GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
					}
					else{
						table.GRR._banker_player=table._banker_select;
						//出牌操作延迟
						GameSchedule.put(new DDZOutCardHandleRunnable(table.getRoom_id(), table.GRR._banker_player,table,table._handler_out_card_operate ),
								GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
					}
					
					return true;
				}
				table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				call_player=table._current_player;
			}
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_JIAO_FEN);
			call_banker_type=2;
		}
		else{
			//叫地主
			if(qiang_bangker==-1){
				//叫地主
				if(call_banker > 0){
					table._banker_select=seat_index;
					boolean have_qiang_palyer=false;
					//玩家如果之前没叫地主没有机会进行抢地主
					for(int i=0;i<table.getTablePlayerNumber()-1;i++){
						
						if(table._playerStatus[table._current_player]._call_banker == -1){
							table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
							qiang_player=table._current_player;
							have_qiang_palyer=true;
							break;
						}
					}
					
					if(!have_qiang_palyer){
						//叫地主结束
						table.GRR._banker_player=table._banker_select;
						//出牌操作延迟
						GameSchedule.put(new DDZOutCardHandleRunnable(table.getRoom_id(), table.GRR._banker_player,table,table._handler_out_card_operate ),
								GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
						return true;
					}
					roomResponse.setType(MsgConstants.RESPONSE_DDZ_QIANG_BANKER);
				}else{
					//判断是否还有人没有轮到叫地主操作
					boolean bhave_no_call=false;
					for(int i=0;i<table.getTablePlayerNumber();i++){
						if(table._playerStatus[i]._call_banker == -1){
							bhave_no_call=true;
						}
					}
					if(!bhave_no_call){
						//叫地主结束
						for(int i=0;i<table.getTablePlayerNumber();i++){
							table._playerStatus[i]._call_banker=-1;
							table.operate_player_data();
						}
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._banker_select, GameConstants.Game_End_NORMAL),
								GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
						return true;
					}
					roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
				}
				table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				call_player=table._current_player;

			}
			else{
				//抢地主
				if(qiang_bangker > 0){
					table._banker_select=seat_index;
				}
				boolean have_qiang_palyer=false;
				for(int i=0;i<table.getTablePlayerNumber()-1;i++){
					//没有不叫地主和没有进行抢地主的玩家才能抢地主
					if(table._playerStatus[table._current_player]._call_banker != 0 
							&& table._playerStatus[table._current_player]._qiang_banker==-1){
						table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
						qiang_player=table._current_player;
						have_qiang_palyer=true;
						break;
					}
				}
				if(!have_qiang_palyer){
					//叫地主结束
					table.GRR._banker_player=table._banker_select;
					//出牌操作延迟
					GameSchedule.put(new DDZOutCardHandleRunnable(table.getRoom_id(), table.GRR._banker_player,table,table._handler_out_card_operate ),
							GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);
					return true;
				}
				table._current_player=(table._current_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				call_player=table._current_player;
				roomResponse.setType(MsgConstants.RESPONSE_DDZ_QIANG_BANKER);
				
			}
			call_banker_type=3;
		}
		
		table.load_room_info_data(roomResponse);
		table.load_common_status(roomResponse);

		if (table._cur_round == 1) {
			table.load_player_info_data(roomResponse);
		}
		
		CallBankerDDZ.Builder call_banker_ddz = CallBankerDDZ.newBuilder();
		call_banker_ddz.setCallCurrentPlayer(call_player);
		call_banker_ddz.setQiangCurrentPlayer(qiang_player);
		call_banker_ddz.setCallType(call_banker_type);
		
		
		RoomResponse_DDZ.Builder roomResponse_ddz = RoomResponse_DDZ.newBuilder();
		roomResponse_ddz.setCallBanker(call_banker_ddz);
		roomResponse.setRoomResponseDdz(roomResponse_ddz);
		table.send_response_to_room(roomResponse);
		return true;
	}

}
