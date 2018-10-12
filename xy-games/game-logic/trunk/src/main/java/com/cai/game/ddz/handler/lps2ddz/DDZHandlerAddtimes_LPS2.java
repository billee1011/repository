package com.cai.game.ddz.handler.lps2ddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lpsddz.lpsDdzRsp.AddTimesDDZResult_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;


public class DDZHandlerAddtimes_LPS2 extends DDZHandlerAddtimes<DDZ_LPS2_Table> {
	protected int _seat_index;
	protected int _game_status;

	public DDZHandlerAddtimes_LPS2() {
	}

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}

	@Override
	public void exe(DDZ_LPS2_Table table) {
		table._game_status = GameConstants.GS_LPS3_ADD_TIMES;// 设置状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._current_player = i;
			table.send_add_times(GameConstants.INVALID_SEAT, i);
		}
	}

	@Override
	public boolean handler_player_be_in_room(DDZ_LPS2_Table table, int seat_index) {
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
			//Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			//tableResponse_ddz.addCardsData(i, cards_card);
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

		
		for(int i = 0;i < table.getTablePlayerNumber(); i++){
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			//if(i == table.GRR._banker_player){
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.BLACK_CARD);
				}
			/*}else{
				for(int j = 0;j < table.m_rang_pai_count;j++){
					cards_card.addItem(GameConstants.RANG_CARD);
				}
				for(int j = 0;j < table.GRR._card_count[i] - table.m_rang_pai_count;j++){
					cards_card.addItem(GameConstants.BLACK_CARD);
				}
			}*/
			tableResponse_ddz.addCardsData(cards_card);
		}
		
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
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
		}
		tableResponse_ddz.setCurrAddPlayer(-1);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		table.send_last_card(false,true,false);
		//发加倍
		//table.send_add_times(seat_index, GameConstants.INVALID_SEAT);
		
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
	public boolean handler_call_banker(DDZ_LPS2_Table table, int seat_index, int addtimes) {
		if (table._add_times_operate[seat_index] || table._game_status != GameConstants.GS_LPS3_ADD_TIMES) {
			return false;
		}
		
		if (table._auto_add_time_scheduled[seat_index] != null) {
			table._auto_add_time_scheduled[seat_index].cancel(false);
			table._auto_add_time_scheduled[seat_index] = null;
		}
		if (table._trustee_auto_opreate_scheduled[seat_index] != null) {
			table._trustee_auto_opreate_scheduled[seat_index].cancel(false);
			table._trustee_auto_opreate_scheduled[seat_index] = null;
		}
		table._current_player = seat_index;
		table._add_times_operate[seat_index] = true;
		table._add_times[seat_index] = addtimes;
		if (addtimes == 1) {
			int temp_times = table._times*2;
			table.set_game_times(temp_times);
			table.send_last_card(true,true,false);
			//table._user_times[seat_index]++;
			// 比赛场重置数据
			PlayerServiceImpl.getInstance().updateRoomInfo(table.getRoom_id());
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES_RESULE);
		AddTimesDDZResult_lps.Builder add_time_result = AddTimesDDZResult_lps.newBuilder();
		add_time_result.setAddtimesaction(addtimes);
		add_time_result.setOpreatePlayer(seat_index);

		for (int j = 0; j < table.getTablePlayerNumber(); j++) {
			add_time_result.addDifenBombDes(table.get_boom_difen_des(j));
		}
		roomResponse.setCommResponse(PBUtil.toByteString(add_time_result));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		
		//自由加倍
		// 等所有操作完才返回结果给所有人
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (!table._add_times_operate[i]) { 
				return true;
			}
		}
		
		
		for (int i = 0; i < table._di_pai_card_count; i++) {
			table.GRR._cards_data[table.GRR._banker_player][i + table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
		}
		table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
		table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player], table.GRR._card_count[table.GRR._banker_player]);
		table._di_pai_type = table._logic.GetDipaiType(table._di_pai_card_data,table._di_pai_card_count);
		int type_times = table._logic.get_type_times(table._di_pai_type);
		if(type_times != -1 && table.has_rule(GameConstants.GAME_RULE_LSAT_TIMES_LPS2)){
			table._di_pai_time = type_times;
			int itemp_times = table._times *= type_times;
			table.set_game_times(itemp_times);
		}
		
		table.send_last_card(true,true,true);
		table.operate_player_cards_flag(table.GRR._banker_player,true);
		
		if(table.has_rule(GameConstants.GAME_RULE_NOT_TAKE_LPS2)){
			table._current_player = table.GRR._banker_player;
			table.set_timer(DDZ_LPS2_Table.ID_TIMER_ADD_TIMES_TO_OUT_CARD, 2000);
		}else{
			table.exe_rang_pai();
		}
		
		return true;
	}

}
