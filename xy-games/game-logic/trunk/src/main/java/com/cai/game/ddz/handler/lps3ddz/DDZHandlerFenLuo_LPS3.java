package com.cai.game.ddz.handler.lps3ddz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.dictionary.SysParamServerDict;
import com.cai.game.ddz.handler.DDZHandler;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lpsddz.lpsDdzRsp.Fen_Luo_Result_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.Fen_Luo_Send_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;

/**
 * 分摞
 * 
 * @author admin
 *
 */
public class DDZHandlerFenLuo_LPS3 extends DDZHandler<DDZ_LPS3_Table> {

	protected int _seat_index;
	protected int _game_status;

	public DDZHandlerFenLuo_LPS3() {
		// TODO Auto-generated constructor stub
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(DDZ_LPS3_Table table) {
		table._game_status = GameConstants.GS_LPS3_FEN_LUO;// 设置状态
		Arrays.fill(table._player_result.pao, -1);
		int temp = GameConstants.INVALID_SEAT;
		if(_seat_index == GameConstants.INVALID_SEAT){
			temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
		}else{
			temp = _seat_index;
		}
		_seat_index = table._banker_select  = table._current_player = temp;
		
		fen_luo(table,_seat_index);
	}

	public void handler_fen_luo(DDZ_LPS3_Table table, int seat_index, int luo_index) {
		if(table._player_result.pao[seat_index] != -1){
			return;
		}
		if(luo_index < 0 || luo_index > table.getTablePlayerNumber()){
			return ;
		}
		
		
		table.m_fen_luo[luo_index] = true;
		table._player_result.pao[seat_index] = 1;
		
		boolean bnextstep = false;
		int select_count = 0;
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			if(table._player_result.pao[i] == -1){
				select_count++;
			}
		}
		if(select_count <= 1)
			bnextstep = true;
		
		//发牌给对应的玩家
		for(int i = 0;i < table.getTablePlayerNumber(); i++){
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_FEN_LUO_RESULE);
			Fen_Luo_Result_lps.Builder fenluo_result = Fen_Luo_Result_lps.newBuilder();
			fenluo_result.setControlPlayer(seat_index);
			fenluo_result.setChooseIsfinish(false);
			fenluo_result.setLuoIndex(luo_index);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if(i == seat_index){
				for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
					cards_card.addItem(table.GRR._cards_data[seat_index][j]);
				}
			}else{
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			}
			fenluo_result.setUserCardCount(table.GRR._card_count[seat_index]);
			fenluo_result.setUserCardsData(cards_card);
			roomResponse.setCommResponse(PBUtil.toByteString(fenluo_result));
			table.send_response_to_player(i, roomResponse);
		}
		
		//回放
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_DDZ_FEN_LUO_RESULE);
		Fen_Luo_Result_lps.Builder fenluo_result1 = Fen_Luo_Result_lps.newBuilder();
		fenluo_result1.setControlPlayer(seat_index);
		fenluo_result1.setChooseIsfinish(false);
		fenluo_result1.setLuoIndex(luo_index);
		fenluo_result1.setUserCardCount(table.GRR._card_count[seat_index]);
		Int32ArrayResponse.Builder cards_card1 = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card1.addItem(table.GRR._cards_data[seat_index][j]);
		}
		fenluo_result1.setUserCardsData(cards_card1);
		roomResponse1.setCommResponse(PBUtil.toByteString(fenluo_result1));
		table.GRR.add_room_response(roomResponse1);
		
		

		if(bnextstep){
			int last_send_player = GameConstants.INVALID_SEAT;
			int ll = 0;
			for(int i = 0;i < table.getTablePlayerNumber();i++){
				if(table._player_result.pao[i] == -1){
					last_send_player = i;
					//break;
				}
				if(table.m_fen_luo[i] == false){
					ll = i;
				}
			}
			if(last_send_player == GameConstants.INVALID_SEAT){
				table.log_error("fenluo error" + last_send_player);
				return;
			}
			table._player_result.pao[last_send_player] = 1;
			table.m_fen_luo[ll] = true;
			for(int i = 0;i < table.getTablePlayerNumber(); i++){
				RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
				roomResponse.setType(MsgConstants.RESPONSE_DDZ_FEN_LUO_RESULE);
				Fen_Luo_Result_lps.Builder fenluo_result = Fen_Luo_Result_lps.newBuilder();
				fenluo_result.setControlPlayer(last_send_player);
				fenluo_result.setChooseIsfinish(false);
				fenluo_result.setLuoIndex(ll);
				Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
				if(i == last_send_player){
					for (int j = 0; j < table.GRR._card_count[last_send_player]; j++) {
						cards_card.addItem(table.GRR._cards_data[last_send_player][j]);
					}
				}else{
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards_card.addItem(GameConstants.INVALID_CARD);
					}
				}
				fenluo_result.setUserCardCount(table.GRR._card_count[last_send_player]);
				fenluo_result.setUserCardsData(cards_card);
				roomResponse.setCommResponse(PBUtil.toByteString(fenluo_result));
				table.send_response_to_player(i, roomResponse);
			}
			
			//回放
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setType(MsgConstants.RESPONSE_DDZ_FEN_LUO_RESULE);
			Fen_Luo_Result_lps.Builder fenluo_result2 = Fen_Luo_Result_lps.newBuilder();
			fenluo_result2.setControlPlayer(last_send_player);
			fenluo_result2.setChooseIsfinish(false);
			fenluo_result2.setLuoIndex(ll);
			fenluo_result2.setUserCardCount(table.GRR._card_count[seat_index]);
			Int32ArrayResponse.Builder cards_card2 = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[last_send_player]; j++) {
				cards_card2.addItem(table.GRR._cards_data[last_send_player][j]);
			}
			fenluo_result2.setUserCardsData(cards_card2);
			roomResponse2.setCommResponse(PBUtil.toByteString(fenluo_result2));
			table.GRR.add_room_response(roomResponse2);
			
			table.set_timer(DDZ_LPS3_Table.ID_TIMER_FENLUO_TO_CALL_BANKER, 2000);
			return ;
		}
		
		int next_seat = (seat_index + table.getPlayerCount() + 1) % table.getPlayerCount();
		
		fen_luo(table,next_seat);
		
	}
	
	public void fen_luo(DDZ_LPS3_Table table,int seat_index){
		
		table._current_player = _seat_index = seat_index;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_FEN_LUO);
		Fen_Luo_Send_lps.Builder fenluo = Fen_Luo_Send_lps.newBuilder();
		fenluo.setDisplayTime(10);
		fenluo.setCurPlayer(seat_index);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			fenluo.addCanSelect(table.m_fen_luo[i]);
			fenluo.addSelectUser(table._player_result.pao[i] == 1);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(fenluo));
		table.send_response_to_room(roomResponse);
		
		table.GRR.add_room_response(roomResponse);
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
			if(table._player_result.pao[seat_index] == 1)
				tableResponse_ddz.addCardCount(table.GRR._card_count[i]);
			else
				tableResponse_ddz.addCardCount(0);
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

		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		//选择分摞,才发牌
		if(table._player_result.pao[seat_index] == 1){
			tableResponse_ddz.setCardsData(seat_index, cards_card);
		}
		
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
		tableResponse_ddz.setDisplayTime(10);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
			tableResponse_ddz.addCanSelect(table.m_fen_luo[i]);
			tableResponse_ddz.addSelectUser(table._player_result.pao[i] == 1);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
		
		fen_luo(table,table._current_player);

		return true;
	}
}
