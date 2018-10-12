package com.cai.game.ddz.handler.lps3ddz;

import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.ddz.handler.DDZHandler;


import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lpsddz.lpsDdzRsp.Swap_Card_Result_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.Swap_Card_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;

/**
 * 换三张
 * 
 * @author admin
 *
 */
public class DDZHandlerSawpCard_LPS3 extends DDZHandler<DDZ_LPS3_Table> {

	protected int _seat_index;
	protected int _game_status;

	public DDZHandlerSawpCard_LPS3() {
		// TODO Auto-generated constructor stub
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(DDZ_LPS3_Table table) {
		table._game_status = GameConstants.GS_LPS3_SWAP_CARD;// 设置状态
		Arrays.fill(table._player_result.qiang, -1);
		
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_SWAP);
		Swap_Card_lps.Builder swap_card = Swap_Card_lps.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			swap_card.addSwapState(table._player_result.qiang[i] == 1);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(swap_card));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		
	}

	public void handler_swap_card(DDZ_LPS3_Table table, int seat_index, List<Integer> list, int card_count) {
		if(table._player_result.qiang[seat_index] != -1){
			table.send_error_notify(seat_index, 2, "您已经选择过了");
			return;
		}
		if(card_count < 3 || list.size()==0 ){
			table.send_error_notify(seat_index, 2, "请选择正确的牌");
		}
		
		int swap_cards[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			swap_cards[i] = list.get(i);
			table.m_swap_card[seat_index][i] = list.get(i);
		}
		
		table._player_result.qiang[seat_index] = 1;
		
		if (!table._logic.remove_cards_by_data(table.GRR._cards_data[seat_index],table.GRR._card_count[seat_index], swap_cards, 3)) {
			table.send_error_notify(seat_index, 2, "请选择正确的牌");
			return ;
		}
		table.GRR._card_count[seat_index] -= 3;
		//发牌给对应的玩家
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_SWAP_RESULT);
			Swap_Card_Result_lps.Builder swap = Swap_Card_Result_lps.newBuilder();
			swap.setCurPlayer(seat_index);
			swap.setUserCardCount(table.GRR._card_count[seat_index]);
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			if(i == seat_index){
				for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
					cards_card.addItem(table.GRR._cards_data[seat_index][j]);
				}
			}else{
				for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
					cards_card.addItem(GameConstants.INVALID_CARD);
				}
			}
			swap.setSwapSeq(-1);
			swap.setSwapIsfinish(false);
			swap.setUserCardsData(cards_card);
			
			for(int k = 0; k < table.getTablePlayerNumber();k++){
				Int32ArrayResponse.Builder swap_card11 = Int32ArrayResponse.newBuilder();
				for(int j = 0;j < 3;j++){
					swap_card11.addItem(GameConstants.INVALID_CARD);
				}
				swap.addSwapCards(swap_card11);
			}
			Int32ArrayResponse.Builder swap_card111 = Int32ArrayResponse.newBuilder();
			for(int j = 0;j < 3;j++){
				swap_card111.addItem(table.m_swap_card[i][j]);
			}
			for(int j = 0;j < 3;j++){
				swap.addChangeOutCard(GameConstants.INVALID_CARD);
			}
			swap.setSwapCards(i, swap_card111);
			
			roomResponse.setCommResponse(PBUtil.toByteString(swap));
			table.send_response_to_player(i, roomResponse);
		}
		
		//回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_SWAP_RESULT);
		Swap_Card_Result_lps.Builder swap = Swap_Card_Result_lps.newBuilder();
		swap.setCurPlayer(seat_index);
		swap.setUserCardCount(table.GRR._card_count[seat_index]);
		//for(int i = 0;i < table.getTablePlayerNumber();i++){
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
				cards_card.addItem(table.GRR._cards_data[seat_index][j]);
			}
		//}
		swap.setUserCardsData(cards_card);
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			Int32ArrayResponse.Builder swap_card1 = Int32ArrayResponse.newBuilder();
			for(int j = 0;j < 3;j++){
				swap_card1.addItem(GameConstants.INVALID_CARD);
			}
			swap.addSwapCards(swap_card1);
		}
		for(int i = 0;i < 3;i++){
			swap.addChangeOutCard(swap_cards[i]);
		}
		swap.setSwapSeq(-1);
		swap.setSwapIsfinish(false);

		roomResponse.setCommResponse(PBUtil.toByteString(swap));
		table.GRR.add_room_response(roomResponse);
		
		boolean bnextstep = true;
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			if(table._player_result.qiang[i] == -1)
				bnextstep = false;
		}
		if(bnextstep){
			table.set_timer(DDZ_LPS3_Table.ID_TIMER_SWAP_TO_CALL_BANKER, 1000);
			return ;
		}
		
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
		tableResponse_ddz.setDisplayTime(10);


		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
		
		//发换三张的消息
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_DDZ_SWAP);
		Swap_Card_lps.Builder swap_card1 = Swap_Card_lps.newBuilder();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			swap_card1.addSwapState(table._player_result.qiang[i] == 1);
		}
		roomResponse1.setCommResponse(PBUtil.toByteString(swap_card1));
		table.send_response_to_player(seat_index, roomResponse1);
		return true;
	}
}
