package com.cai.game.wsk.handler.hts;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.game.wsk.handler.WSKHandlerOutCardOperate;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.hts.htsRsp.TableResponse_hts;
import protobuf.clazz.sxth.SxthRsp.TableResponse_Sxth;

public class WSKHandlerOutCardOperate_HTS extends WSKHandlerOutCardOperate<WSKTable_HTS> {

	@Override
	public void exe(WSKTable_HTS table) {
		if (_out_card_player != table._current_player) {
			return;
		}
		
		//第三游最后一首不要直接结算
		if(table.last_out_finish == true && _out_type == 0){
			int delay = 1;
			if(table.second_winer != -1 && table._turn_have_score > 0) {
				table._get_score[table.second_winer] += table._turn_have_score;
				table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);
			}
			table._current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
					GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			
			user_pass_card(table,true);
			return;
		}
		
		// 玩家不出
		if (_out_type == 0) {
			user_pass_card(table,false);
			return;
		}
		
		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_VALUE);
		// 出牌判断
		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.HTS_CT_ERROR) {
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}
		
		//暗三出黑桃三
		if(card_type == GameConstants.HTS_CT_HTS && !table.have_chengbao){
			if(table.has_rule(GameConstants.GAME_RULE_AN_HTS)){
				if(table.GRR._banker_player < 0){
					table.GRR._banker_player = _out_card_player;
				}
				table.show_hts_player = true;
				table.send_to_friend(3);
				table.send_texiao(1,table.GRR._banker_player);
				table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);
			}
		}

			
		_out_type = card_type;

		
		// 桌面牌分
		table_pai_socre(table);

		table.GRR._card_count[_out_card_player] -= _out_card_count;

		table._turn_have_score += table._logic.GetCardScore(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;
		table._out_card_player = _out_card_player;
		table._prev_palyer = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		table._cur_out_car_type[_out_card_player] = card_type;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
		for (int i = 0; i < _out_card_count; i++) {
			table._turn_out_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = table._turn_out_card_data[i];

		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
				}
				// 显示出牌
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				table._cur_out_car_type[next_player] = GameConstants.HTS_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		table._cur_out_car_type[next_player] = GameConstants.HTS_CT_ERROR;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);
		if(table.GRR._card_count[_out_card_player] == 0){
			table.win_order[_out_card_player] = table.winer_index++;
		}
		
		//第三游压完最后一首直接结算，有分要给三游
		if(table.last_out_finish){
			table._get_score[table._out_card_player] += table._turn_have_score;
			if (table._turn_have_score > 0) {table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);}
			int delay = 1;
			table._current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
					GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
		}
		
		//判断游戏是否结束
		if(table.have_chengbao){
			if (table.GRR._card_count[_out_card_player] == 0) {
				int delay = 2;
				table._current_player = GameConstants.INVALID_SEAT;
				table._get_score[_out_card_player] += table._turn_have_score;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
						GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
			}
		}else{
			if(_out_card_player == table.GRR._banker_player){
				if (table.GRR._card_count[_out_card_player] == 0 ) {
					if(table.winer_index == 1){
						int delay = 2;
						table._current_player = GameConstants.INVALID_SEAT;
						table._get_score[_out_card_player] += table._turn_have_score;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
								GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
					}else if(table.winer_index == 2){
						table.second_winer = _out_card_player;
						table.last_out_finish = true;
					}
				}
			}else{
				int out_over = 0;
				for(int i = 0;i < table.getTablePlayerNumber();i++){
					if(table.GRR._card_count[i] == 0){
						out_over++;
					}
				}
				if(out_over == 2){
					table.second_winer = _out_card_player;
					table.last_out_finish = true;
				}
				if(out_over > 2){
					//int delay = 3;
					//table._current_player = GameConstants.INVALID_SEAT;
					//table._get_score[_out_card_player] += table._turn_have_score;
					//GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
					//		GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);
				}
			}
		}
		
		table._logic.sort_card_date_list_by_type(_out_cards_data, _out_card_count,_out_type,table._turn_three_link_num);
		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
				table._turn_out_card_type, GameConstants.INVALID_SEAT, false);
		
	}

	public boolean table_pai_socre(WSKTable_HTS table) {
		int pai_score = 0;
		int remove_count = 0;
		boolean have_score = false;
		for (int i = 0; i < _out_card_count; i++) {
			int value = table._logic.GetCardLogicValue(_out_cards_data[i]);
			if (value == 5 || value == 10 || value == 13) {
				table.list_cur_score_card.add(_out_cards_data[i]);
				//if(table.cur_score_card_count < 12)
				//	table.cur_score_card[table.cur_score_card_count++] = _out_cards_data[i];
				have_score = true;
			}
			if (value == 5) {
				pai_score += 5;
			} else if (value == 10 || value == 13) {
				pai_score += 10;
			}
		}
		table.table_score += pai_score;
		table._pai_score_count -= remove_count;
		table._pai_score -= pai_score;
		return have_score;
	}

	public void user_pass_card(WSKTable_HTS table,boolean finish) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (next_player == table._out_card_player) {
					break;
				}
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
				}
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		table._cur_out_car_type[_out_card_player] = _out_type;
		// 一轮不出
		if (next_player == table._out_card_player) {
			
			// 清空桌面牌分
			table._get_score[table._out_card_player] += table._turn_have_score;
			if (table._turn_have_score > 0) {
				for(int j = 0;j < table.list_cur_score_card.size();j++){
					table.list_score_card[table._out_card_player].add(table.list_cur_score_card.get(j));
				}
				table.list_cur_score_card.clear();
				
				//for(int j = 0; j < table.cur_score_card_count;j++){
				//	table.score_card[table._out_card_player][table.score_card_count[table._out_card_player] + j] = table.cur_score_card[j];
				//	table.cur_score_card[j] = GameConstants.INVALID_CARD;
				//}
				//table.score_card_count[table._out_card_player] += table.cur_score_card_count;
				//table.cur_score_card_count = 0;
				table.Refresh_user_get_score(GameConstants.INVALID_SEAT, true);
			}
			
			table._turn_have_score = 0;
			table._turn_out_card_count = 0;
			table.table_score = 0;
			table._turn_out_card_type = GameConstants.HTS_CT_ERROR;
			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_real_card_data, GameConstants.INVALID_CARD);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				table._cur_out_car_type[i] = GameConstants.HTS_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_CARD, false);
				}

				next_player = (next_player + 1) % table.getTablePlayerNumber();
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.GRR._card_count[next_player] == 0) {
						next_player = (next_player + 1) % table.getTablePlayerNumber();
					} else {
						break;
					}
				}

				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				table._cur_out_car_type[next_player] = GameConstants.HTS_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

			} else {
				table._current_player = next_player;
				table._prev_palyer = _out_card_player;
				table._cur_out_card_count[table._current_player] = 0;
				table._cur_out_car_type[table._current_player] = GameConstants.HTS_CT_ERROR;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}

		} else {
			table._current_player = next_player;
			table._prev_palyer = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			table._cur_out_car_type[table._current_player] = GameConstants.HTS_CT_ERROR;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		if(finish){
			table._current_player = GameConstants.INVALID_SEAT;
		}
		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.HTS_CT_PASS, GameConstants.INVALID_SEAT,false);

	}

	public int adjust_out_card_right(WSKTable_HTS table) {
		
		boolean isLast = table.GRR._card_count[_out_card_player] == _out_card_count ? true : false;
		table._logic.SortCardList(_out_cards_data, _out_card_count, GameConstants.WSK_ST_VALUE);
		int card_type = GameConstants.HTS_CT_ERROR;
		card_type = table._logic.GetCardType(_out_cards_data, _out_card_count);
		if (card_type == GameConstants.HTS_CT_ERROR) {
			return card_type;
		}
		if(!isLast && (card_type == GameConstants.HTS_CT_THREE || card_type == GameConstants.HTS_CT_PLANE_LOSS)){
			return GameConstants.HTS_CT_ERROR;
		}
		
		//首出红桃三
		if(table.find_specified_card_by_specified_player(_out_card_player,table.CARD_HONG_TAO_3)){
			boolean hong_tao_3 = false;
			for(int j = 0;j < _out_card_count;j++){
				if(table.CARD_HONG_TAO_3 == _out_cards_data[j]){
					hong_tao_3 = true;
				}
			}
			if(!hong_tao_3){
				//table.send_error_notify(_out_card_player, 2, "首出必须出红桃三!");
				return GameConstants.HTS_CT_ERROR;
			}
		}

		table._turn_three_link_num = table._logic.get_three_link_count(_out_cards_data, _out_card_count, card_type);
		if (table._turn_out_card_count == 0
				&& (card_type == GameConstants.HTS_CT_PLANE_LOSS || card_type == GameConstants.HTS_CT_THREE)) {
			table._turn_three_link_num = table._logic.get_three_link_count(_out_cards_data, _out_card_count, card_type);

			int turn_link_num = table._logic.get_three_link_count(table._turn_out_card_data, table._turn_out_card_count, table._turn_out_card_type);
			table._turn_three_link_num =  turn_link_num; 
		}
		table._logic.sort_card_date_list_by_type(_out_cards_data, _out_card_count, card_type,table._turn_three_link_num);
		
		if (table._turn_out_card_count != 0) {
			if (!table._logic.CompareCard_WSK(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count,
					_out_card_count,table._turn_three_link_num)) {
				return GameConstants.HTS_CT_ERROR;
			}
		}
		if (!table._logic.RemoveCard(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.HTS_CT_ERROR;
		}

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(WSKTable_HTS table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_HTS_RECONNECT_DATA);

		TableResponse_hts.Builder tableResponse = TableResponse_hts.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_palyer);

		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addOpreateType(table._player_result.pao[i]);
			tableResponse.addOutCardType(table._cur_out_car_type[i]);
			tableResponse.addCardCount(table.GRR._card_count[i]);
			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for(int j = 0;j < table.GRR._card_count[i];j++){
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
		}
		if(table.show_hts_player){
			tableResponse.setHtsPlayer(table.hei_san_player);
		}else{
			tableResponse.setHtsPlayer(-1);
		}
		tableResponse.setZuoFei(table._logic.hts_zuo_fei);
		tableResponse.setLipaiType(table.blipai[seat_index]);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));
		table.send_response_to_player(seat_index, roomResponse);

		table.Refresh_user_get_score(seat_index, false);
		

		if (table._game_status == GameConstants.GS_HTS_PLAY) {
			// 显示出牌
			table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_out_card_data,
					table._turn_out_card_type, seat_index, false);
		}
		

		//发朋友标志
		if(table.have_chengbao){
			table.send_to_friend(1);
		}else if(table.has_rule(GameConstants.GAME_RULE_MING_HTS)){
			table.send_to_friend(2);
		}else if(table.has_rule(GameConstants.GAME_RULE_AN_HTS)){
			table.send_to_friend(3);
		}

		return true;
	}

}
