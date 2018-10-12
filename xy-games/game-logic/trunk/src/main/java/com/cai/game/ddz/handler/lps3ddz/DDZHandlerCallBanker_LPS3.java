package com.cai.game.ddz.handler.lps3ddz;

import java.util.Arrays;
import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.define.EGameType;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamServerDict;
import com.cai.game.ddz.handler.DDZHandlerCallBanker;


import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lpsddz.lpsDdzRsp.CallBankerDDZ_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.CallBankerResult_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;

public class DDZHandlerCallBanker_LPS3 extends  DDZHandlerCallBanker<DDZ_LPS3_Table> {
	protected int _seat_index;
	protected int _game_status;

	public DDZHandlerCallBanker_LPS3() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(DDZ_LPS3_Table table) {

		int qiang_action[] = new int[2];
		int call_action[] = new int[4];
		Arrays.fill(qiang_action, -1);
		Arrays.fill(call_action, -1);
		table._game_status = GameConstants.GS_LPS3_CALL_BANKER;// 设置状态
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		int call_banker_type = 0;
		call_banker_type = 2;
		call_action[0] = 1;
		call_action[1] = 1;
		call_action[2] = 1;
		call_action[3] = 1;
		table._call_banker_type = call_banker_type;
		table._call_banker_status = 1;
		int temp = 0;
		table._banker_select = table.m_turn_banker;
		if (table._banker_select == GameConstants.INVALID_SEAT) {
			temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
		} else {
			temp = table._banker_select;
			table._banker_select = GameConstants.INVALID_SEAT;
		}
		table._current_player = temp;
		this._seat_index = table._current_player;
		
		//两王对二必叫
		if(table.has_rule(GameConstants.GAME_RULE_MUST_CALL_LPS3) && 
				table._logic.is_must_call(table.GRR._cards_data[_seat_index],table.GRR._card_count[_seat_index])){
			call_action[0] = 0;
			call_action[1] = 0;
			call_action[2] = 0;
			call_action[3] = 1;
		}

		CallBankerDDZ_lps.Builder call_banker_ddz = CallBankerDDZ_lps.newBuilder();
		call_banker_ddz.setCallCurrentPlayer(table._current_player);
		call_banker_ddz.setQiangCurrentPlayer(GameConstants.INVALID_SEAT);
		call_banker_ddz.setCallType(call_banker_type);
		call_banker_ddz.setDisplayTime(SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(9).getVal3() / 1000);
		for (int i = 0; i < 4; i++) {
			table._call_action[i] = call_action[i];
			call_banker_ddz.addCallCurrentAction(call_action[i]);
		}
		for (int i = 0; i < 2; i++) {
			table._qiang_action[i] = qiang_action[i];
			call_banker_ddz.addQiangCurrentAction(qiang_action[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		int dealy = 0;
		if (table.istrustee[table._current_player]) {
			dealy = 1;
		} else {
			dealy = 10;
		}
		// table._auto_call_banker_scheduled=GameSchedule.put(new
		// DDZAutoCallbankerRunnable(table.getRoom_id(),
		// table,table._current_player,1),
		// dealy, TimeUnit.SECONDS);
	}

	@Override
	public boolean handler_player_be_in_room(DDZ_LPS3_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_RECONNECT_DATA);
		TableResponseDDZ_lps.Builder tableResponse_ddz = TableResponseDDZ_lps.newBuilder();
		table.load_player_info_data_reconnect_lps(tableResponse_ddz);
		RoomInfo.Builder room_info = table.getRoomInfo();
		tableResponse_ddz.setRoomInfo(room_info);
		tableResponse_ddz.setBankerPlayer(table.GRR._banker_player);

		tableResponse_ddz.setCurrentPlayer(-1);
		tableResponse_ddz.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_ddz.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_round_data[i][j]);
				}
			}
			tableResponse_ddz.addCardCount(table.GRR._card_count[i]);
			tableResponse_ddz.addCardType(table.GRR._cur_card_type[i]);
			tableResponse_ddz.addOutCardsData(i, out_cards);
			tableResponse_ddz.addChangeCardsData(out_change_cards);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			tableResponse_ddz.addCardsData(i, cards_card);
			tableResponse_ddz.addQiangBanker(table._qiang_banker[i]);
			tableResponse_ddz.addCallBanker(table._call_banker[i]);
			tableResponse_ddz.addAddTimes(table._add_times[i]);
			tableResponse_ddz.addDifenBombDes(table.get_boom_difen_des(i));
		}
		tableResponse_ddz.addQiangAction(table._qiang_action[0]);
		tableResponse_ddz.addQiangAction(table._qiang_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[0]);
		tableResponse_ddz.addCallAction(table._call_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[2]);
		tableResponse_ddz.addCallAction(table._call_action[3]);

		for (int i = 0; i < table._di_pai_card_count; i++) {
			tableResponse_ddz.addDiCardsData(table._di_pai_card_data[i]);
		}
		tableResponse_ddz.setDiCardCount(table._di_pai_card_count);
		tableResponse_ddz.setDiCardsType(table._di_pai_type);

		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		tableResponse_ddz.setCardsData(seat_index, cards_card);
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_ddz.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_ddz.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_ddz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_ddz.setPrOutCardType(table._turn_out_card_type);
		tableResponse_ddz.setPrOutCardPlayer(table._turn_out__player);
		if (table._turn_out_card_count == 0) {
			tableResponse_ddz.setIsFirstOut(1);
		} else {
			tableResponse_ddz.setIsFirstOut(0);
		}
		tableResponse_ddz.setCallType(table._call_banker_type);
		tableResponse_ddz.setDisplayTime(10);
		tableResponse_ddz.setCurPlayerYaPai(1);
		
		if(table.has_rule(GameConstants.GAME_RULE_FREE_TIMES_LPS3)){
			tableResponse_ddz.setCurrAddPlayer(-1);
		}else if(table.has_rule(GameConstants.GAME_RULE_FARMERS_FIRST_LPS3)){
			tableResponse_ddz.setCurrAddPlayer(table._current_player);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
		
		
		//再发一次叫地主
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		CallBankerDDZ_lps.Builder call_banker_ddz1 = CallBankerDDZ_lps.newBuilder();
		call_banker_ddz1.setCallCurrentPlayer(table._current_player);
		call_banker_ddz1.setQiangCurrentPlayer(GameConstants.INVALID_SEAT);
		call_banker_ddz1.setCallType(table._call_banker_type);
		call_banker_ddz1.setDisplayTime(SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(9).getVal3() / 1000);
		for (int i = 0; i < 4; i++) {
			call_banker_ddz1.addCallCurrentAction(table._call_action[i]);
		}
		for (int i = 0; i < 2; i++) {
			call_banker_ddz1.addQiangCurrentAction(table._qiang_action[i]);
		}
		roomResponse1.setCommResponse(PBUtil.toByteString(call_banker_ddz1));
		table.send_response_to_player(seat_index, roomResponse1);
		return true;
	}

	/**
	 * @param get_seat_index
	 * @param call_banker
	 *            -1为没有进行叫地主操作，0为不叫地主，大于0为叫地主
	 * @param qiang_bangker
	 *            -1为没有进行抢地主操作，0为不抢地主，大于0为抢地主
	 * @return
	 */
	public boolean handler_call_banker(DDZ_LPS3_Table table, int seat_index, int call_banker, int qiang_bangker) {
		if (table._call_banker_status == 0 || table._current_player != seat_index) {
			return true;
		} else if (table._call_banker_status == 1 && qiang_bangker != -1) {
			return true;
		} else if (table._call_banker_status == 2 && call_banker != -1) {
			return true;
		}
		if (table._auto_call_banker_scheduled != null) {
			table._auto_call_banker_scheduled.cancel(false);
			table._auto_call_banker_scheduled = null;
		}

		if (call_banker != -1) {
			table._playerStatus[seat_index]._call_banker = call_banker;
		}
		if (qiang_bangker != -1) {
			table._playerStatus[seat_index]._qiang_banker = qiang_bangker;
		}
		table._qiang_banker[seat_index] = qiang_bangker;
		table._call_banker[seat_index] = call_banker;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
		// 刷新用户叫分
		table.operate_player_data();
		int qiang_player = GameConstants.INVALID_SEAT;
		int call_player = GameConstants.INVALID_SEAT;
		int cur_qiang_player = GameConstants.INVALID_SEAT;
		int cur_call_player = GameConstants.INVALID_SEAT;
		int qiang_action = -1;
		int call_action = -1;
		int call_banker_type = 1;
		int qiang_action_current[] = new int[2];
		int call_action_current[] = new int[4];
		Arrays.fill(qiang_action_current, -1);
		Arrays.fill(call_action_current, 0);
		{
			call_action = call_banker;
			call_player = seat_index;
			// 用户叫分
			if (call_banker == 3) {
				// 玩家叫3分该玩家即为地主
				table.GRR._banker_player = table._banker_select = seat_index;
				//table.m_turn_banker = table._banker_select;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table._playerStatus[i]._call_banker = -1;
					table.operate_player_data();
				}
				table.game_cell = call_banker;
				table._times = call_banker;
				for(int i = 0;i < table.getTablePlayerNumber();i++){
					table._user_times[i] = call_banker;
				}

				// 可叫分动作
				for (int i = 0; i < 4; i++) {
					table._call_action[i] = call_action_current[i];
				}
				for (int i = 0; i < 2; i++) {
					table._qiang_action[i] = qiang_action_current[i];
				}

				send_callbanker_to_player(table,qiang_player,call_player,call_action,qiang_action);
				
				//不能加倍
				if(table.has_rule(GameConstants.GAME_RULE_NOT_TIMES_LPS3)){
					table._current_player = table.GRR._banker_player;
					table._game_status = GameConstants.GS_LPS3_PLAY;// 设置状态
					for (int i = 0; i < table._di_pai_card_count; i++) {
						table.GRR._cards_data[table.GRR._banker_player][i + table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
					}
					table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
					table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player], table.GRR._card_count[table.GRR._banker_player]);
					table._di_pai_type = table._logic.GetDipaiType(table._di_pai_card_data,table._di_pai_card_count);
					int type_times = table._logic.get_type_times(table._di_pai_type);
					if(type_times != -1){
						table._times *= type_times;
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							table._user_times[i] *= type_times;
						}
					}
					
					table.send_last_card(true);
					table.operate_player_cards_flag(table.GRR._banker_player);
					table._handler = table._handler_out_card_operate;
					table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,GameConstants.INVALID_SEAT);
					
					//发送记牌器
					if(table.has_rule(GameConstants.GAME_RULE_MEMORY_ROBOT_LPS3))
						table.send_record_cards();

				}else{
					table._game_status = GameConstants.GS_LPS3_ADD_TIMES;
					table.set_timer(DDZ_LPS3_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 3000);
				}
				return true;
			} else {
				// 玩家叫分
				if (call_banker != 0) {
					//当前叫的分不能少于上一次叫的分
					if (table._call_banker_score >= call_banker) {
						return true;
					}
					table._call_banker_score = call_banker;
					table.game_cell = call_banker;
					for(int i = 0;i < table.getTablePlayerNumber();i++){
						table._user_times[i] = call_banker;
					}
					table._times = call_banker;
					if (table._banker_select != GameConstants.INVALID_SEAT) {
						if (table._playerStatus[table._banker_select]._call_banker < call_banker) {
							table._banker_select = seat_index;
						}
					} else {
						table._banker_select = seat_index;
					}
				}
				// 判断是否还有人没有轮到叫地主操作
				boolean bhave_no_call = false;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._playerStatus[i]._call_banker == -1) {
						bhave_no_call = true;
					}
				}
				if (!bhave_no_call) {
					// 叫地主结束
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						table._playerStatus[i]._call_banker = -1;
						table.operate_player_data();
					}
					if (table._banker_select == GameConstants.INVALID_SEAT) {
						CallBankerResult_lps.Builder call_banker_result = CallBankerResult_lps.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						call_banker_result.setCallType(table._call_banker_type);
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						call_banker_result.setBankerPlayer(GameConstants.INVALID_SEAT);

						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.send_response_to_room(roomResponse);
						table.GRR.add_room_response(roomResponse);
						//没有玩家叫地主 重新发牌
						table.exe_dispath();
						
						return true;
	
					} else {
						//table.m_turn_banker = table._banker_select;
						table.GRR._banker_player = table._banker_select;
						
						table._call_banker_status = 2;
						
						send_callbanker_to_player(table,qiang_player,call_player,call_action,qiang_action);
						
						table._current_player = table.GRR._banker_player;
						
						if(table.has_rule(GameConstants.GAME_RULE_NOT_TIMES_LPS3)){
							table._current_player = table.GRR._banker_player;
							table._game_status = GameConstants.GS_LPS3_PLAY;// 设置状态
							for (int i = 0; i < table._di_pai_card_count; i++) {
								table.GRR._cards_data[table.GRR._banker_player][i + table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
							}
							table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
							table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player], table.GRR._card_count[table.GRR._banker_player]);
							table._di_pai_type = table._logic.GetDipaiType(table._di_pai_card_data,table._di_pai_card_count);
							int type_times = table._logic.get_type_times(table._di_pai_type);
							if(type_times != -1){
								table._times *= type_times;
								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									table._user_times[i] *= type_times;
								}
							}
							table.send_last_card(true);
							table.operate_player_cards_flag(table.GRR._banker_player);
							table._handler = table._handler_out_card_operate;
							table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,GameConstants.INVALID_SEAT);
							//发送记牌器
							if(table.has_rule(GameConstants.GAME_RULE_MEMORY_ROBOT_LPS3))
								table.send_record_cards();
							
						}else{
							table._game_status = GameConstants.GS_LPS3_ADD_TIMES;
							table.set_timer(DDZ_LPS3_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 3000);
						}	
					}
					return true;
				}
				
				// 可叫地主动作
				int can_call = table._call_banker_score;
				int index = 0;
				call_action_current[index++] = 0;
				for(int i = 0;i < 4;i++){
					if(i > can_call)
						call_action_current[i] = 1;
				}
				call_action_current[0] = 1;

				call_banker_type = 2;
				table._current_player = (table._current_player + table.getTablePlayerNumber() + 1)
						% table.getTablePlayerNumber();
				cur_call_player = table._current_player;
				
				//两王对二必叫
				if(table.has_rule(GameConstants.GAME_RULE_MUST_CALL_LPS3) && 
						table._logic.is_must_call(table.GRR._cards_data[cur_call_player],table.GRR._card_count[cur_call_player])){
					call_action_current[0] = 0;
					call_action_current[1] = 0;
					call_action_current[2] = 0;
					call_action_current[3] = 1;
				}
				
				for (int i = 0; i < 4; i++) {
					table._call_action[i] = call_action_current[i];
				}
				for (int i = 0; i < 2; i++) {
					table._qiang_action[i] = qiang_action_current[i];
				}
			}
		}
		table._call_banker_type = call_banker_type;
		if (cur_call_player != GameConstants.INVALID_SEAT) {
			table._call_banker[cur_call_player] = -1;
		}
		if (cur_qiang_player != GameConstants.INVALID_SEAT) {
			table._qiang_banker[cur_qiang_player] = -1;
		}

		CallBankerResult_lps.Builder call_banker_result = CallBankerResult_lps.newBuilder();
		call_banker_result.setQiangPlayer(qiang_player);
		call_banker_result.setCallPlayer(call_player);
		call_banker_result.setCallAction(call_action);
		call_banker_result.setQiangAction(qiang_action);
		call_banker_result.setCallType(table._call_banker_type);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
		}
		call_banker_result.setBankerPlayer(GameConstants.INVALID_SEAT);

		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		CallBankerDDZ_lps.Builder call_banker_ddz = CallBankerDDZ_lps.newBuilder();
		call_banker_ddz.setCallCurrentPlayer(cur_call_player);
		call_banker_ddz.setQiangCurrentPlayer(cur_qiang_player);
		call_banker_ddz.setCallType(call_banker_type);
		call_banker_ddz.setDisplayTime(SysParamServerDict.getInstance()
				.getSysParamModelDictionaryByGameId(EGameType.DT.getId()).get(9).getVal3() / 1000);
		for (int i = 0; i < 4; i++) {
			call_banker_ddz.addCallCurrentAction(call_action_current[i]);
		}
		for (int i = 0; i < 2; i++) {
			call_banker_ddz.addQiangCurrentAction(qiang_action_current[i]);
		}

		//roomResponse.clearCommResponse();
		roomResponse1.setCommResponse(PBUtil.toByteString(call_banker_ddz));
		table.send_response_to_room(roomResponse1);
		table.GRR.add_room_response(roomResponse1);
		return true;
	}
	
	public void send_callbanker_to_player(DDZ_LPS3_Table table,int qiang_player,int call_player,int call_action,int qiang_action){
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
		for (int index = 0; index < table.getTablePlayerNumber(); index++) {
			CallBankerResult_lps.Builder call_banker_result = CallBankerResult_lps.newBuilder();
			call_banker_result.setQiangPlayer(qiang_player);
			call_banker_result.setCallPlayer(call_player);
			call_banker_result.setCallAction(call_action);
			call_banker_result.setQiangAction(qiang_action);
			call_banker_result.setBankerPlayer(table.GRR._banker_player);
			call_banker_result.setCallType(table._call_banker_type);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if (i == index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards_card.addItem(table.GRR._cards_data[i][j]);
					}

				}
				call_banker_result.addUserCardCount(table.GRR._card_count[i]);
				call_banker_result.addUserCardsData(cards_card);
			}
			call_banker_result.setBankerPlayer(table.GRR._banker_player);
			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
			table.send_response_to_player(index, roomResponse);
		}

		// 回放
		CallBankerResult_lps.Builder call_banker_result = CallBankerResult_lps.newBuilder();
		call_banker_result.setQiangPlayer(qiang_player);
		call_banker_result.setCallPlayer(call_player);
		call_banker_result.setCallAction(call_action);
		call_banker_result.setQiangAction(qiang_action);
		call_banker_result.setBankerPlayer(table.GRR._banker_player);
		call_banker_result.setCallType(table._call_banker_type);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[i]; j++) {
				cards_card.addItem(table.GRR._cards_data[i][j]);
			}
			call_banker_result.addUserCardCount(table.GRR._card_count[i]);
			call_banker_result.addUserCardsData(cards_card);
		}
		call_banker_result.setBankerPlayer(table.GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
		table.GRR.add_room_response(roomResponse);
	}

}
