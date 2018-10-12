package com.cai.game.ddz.handler.lps2ddz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.game.ddz.handler.DDZHandler;
import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomInfo;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.lpsddz.lpsDdzRsp.Rang_Pai_Result_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.Rang_Pai_lps;
import protobuf.clazz.lpsddz.lpsDdzRsp.TableResponseDDZ_lps;

/**
 * 飘分
 * 
 * @author admin
 *
 */
public class DDZHandlerRangPai_LPS2 extends DDZHandler<DDZ_LPS2_Table> {

	protected int _seat_index;

	public DDZHandlerRangPai_LPS2() {
		// TODO Auto-generated constructor stub
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
	}

	@Override
	public void exe(DDZ_LPS2_Table table) {
		table._game_status = GameConstants.GS_LPS3_RANG_PAI;// 设置状态
		int rang_action[] = new int[4];
		Arrays.fill(rang_action, -1);
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		Rang_Pai_lps.Builder rang = Rang_Pai_lps.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_RANG_PAI);
		int count = 0;
		if(table.has_rule(GameConstants.GAME_RULE_TAKE_2_LPS2)){
			count = 2;
			rang_action[0] = 0;
			rang_action[1] = 2;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_3_LPS2)){
			count = 3;
			rang_action[0] = 0;
			rang_action[1] = 2;
			rang_action[2] = 3;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_4_LPS2)){
			count = 4;
			rang_action[0] = 0;
			rang_action[1] = 2;
			rang_action[2] = 3;
			rang_action[3] = 4;
		}
		rang.setCurPlayer(table.GRR._banker_player);
		rang.setDisplayTime(5);
		for(int i = 0; i < count;i++){
			rang.addRangAction(rang_action[i]);
		}
		
		roomResponse.setCommResponse(PBUtil.toByteString(rang));
		table.send_response_to_room(roomResponse);
		
		table.GRR.add_room_response(roomResponse);
	}

	public void handler_rang_pai(DDZ_LPS2_Table table, int seat_index, int rang_index) {
		int rang_pai = 0;
		if(rang_index > 0){
			rang_pai = rang_index + 1;
		}
		
		if(table.has_rule(GameConstants.GAME_RULE_TAKE_2_LPS2) && rang_pai > 2){
			return;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_3_LPS2) && rang_pai > 3){
			return;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_4_LPS2) && rang_pai > 4){
			return;
		}
		
		table.m_rang_pai_count += rang_pai;
		int irangtimes = (int)(Math.pow(2, rang_pai));
		int temp_times = table._times*irangtimes;
		
		table.set_game_times(temp_times);
		
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			roomResponse.setType(MsgConstants.RESPONSE_DDZ_RANG_PAI_RESULT);
			Rang_Pai_Result_lps.Builder rangRequest = Rang_Pai_Result_lps.newBuilder();
			rangRequest.setRangPai(rang_pai);
			rangRequest.setCurPlayer(table.GRR._banker_player);
			rangRequest.setAllRangPai(table.m_rang_pai_count);
			roomResponse.setCommResponse(PBUtil.toByteString(rangRequest));
			table.send_response_to_player(i, roomResponse);
		}
		
		//回放
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_RANG_PAI_RESULT);
		Rang_Pai_Result_lps.Builder rangRequest = Rang_Pai_Result_lps.newBuilder();
		rangRequest.setRangPai(rang_pai);
		rangRequest.setAllRangPai(table.m_rang_pai_count);
		rangRequest.setCurPlayer(table.GRR._banker_player);
		roomResponse.setCommResponse(PBUtil.toByteString(rangRequest));
		table.GRR.add_room_response(roomResponse);
		
		//改变倍数
		table.send_last_card(true,true,false);
		
		//地主选择让牌，农民先出牌
		if(rang_index > 0){
			table._current_player = (table.GRR._banker_player + table.getPlayerCount() + 1) % table.getPlayerCount();
		}else{
			table._current_player = table.GRR._banker_player;
		}
		
		
		table.set_timer(DDZ_LPS2_Table.ID_TIMER_ADD_TIMES_TO_OUT_CARD, 2000);
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
		tableResponse_ddz.setDisplayTime(10);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
			tableResponse_ddz.addCanSelect(table.m_fen_luo[i]);
			tableResponse_ddz.addSelectUser(table._player_result.pao[i] == 1);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
		
		//刷一下手牌
		//table.operate_player_cards_flag(seat_index,false);
		
		//
		table.send_last_card(true,true,false);
		
		
		//发送让牌消息
		int rang_action[] = new int[4];
		Arrays.fill(rang_action, -1);
		RoomResponse.Builder roomResponse1 = RoomResponse.newBuilder();
		Rang_Pai_lps.Builder rang1 = Rang_Pai_lps.newBuilder();
		roomResponse1.setType(MsgConstants.RESPONSE_DDZ_RANG_PAI);
		int count = 0;
		if(table.has_rule(GameConstants.GAME_RULE_TAKE_2_LPS2)){
			count = 2;
			rang_action[0] = 0;
			rang_action[1] = 2;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_3_LPS2)){
			count = 3;
			rang_action[0] = 0;
			rang_action[1] = 2;
			rang_action[2] = 3;
		}else if(table.has_rule(GameConstants.GAME_RULE_TAKE_4_LPS2)){
			count = 4;
			rang_action[0] = 0;
			rang_action[1] = 2;
			rang_action[2] = 3;
			rang_action[3] = 4;
		}
		rang1.setCurPlayer(table.GRR._banker_player);
		rang1.setDisplayTime(5);
		for(int i = 0; i < count;i++){
			rang1.addRangAction(rang_action[i]);
		}
		roomResponse1.setCommResponse(PBUtil.toByteString(rang1));
		table.send_response_to_player(seat_index, roomResponse1);
		
		return true;
	}
}
