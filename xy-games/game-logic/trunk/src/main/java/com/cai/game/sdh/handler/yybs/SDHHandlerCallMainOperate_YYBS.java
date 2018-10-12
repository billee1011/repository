package com.cai.game.sdh.handler.yybs;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.sdh.SDHConstants_YYBS;
import com.cai.common.util.PBUtil;
import com.cai.game.sdh.handler.SDHHandler;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.sdh.SdhRsp.TableResponseSdh;

public class SDHHandlerCallMainOperate_YYBS<T extends SDHTable_YYBS> extends SDHHandler<T> {

	private boolean success = true;

	@Override
	public void exe(SDHTable_YYBS table) {

	}

	public boolean isSuccess() {
		return success;
	}

	@Override
	public boolean handler_player_be_in_room(SDHTable_YYBS table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SDHConstants_YYBS.RESPONSE_RECONNECT_DATA);
		roomResponse.setPao(table.disPlayerCardCount);

		TableResponseSdh.Builder tableResponseSdh = TableResponseSdh.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);
		if (table.disPatchCardEnd) { //还在发牌状态
			// 排序
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._logic.sortCardList(table.GRR._cards_data[i], 21);
			}
		} 

		if (table.GRR != null) {
			tableResponseSdh.setCurrentPlayer(table._current_player);
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponseSdh.addOutCardsCount(table.GRR._cur_round_count[i]);
				Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
				if (i == seat_index) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				} else {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(GameConstants.INVALID_CARD);
					}
				}
				// 发牌结束重连发送手牌
				if (table.disPatchCardEnd) {
					tableResponseSdh.addCardsData(i, cards);
				}
				tableResponseSdh.addCallBankerScore(table.callScore[i]);
			}
			for (int i = 0; i < SDHConstants_YYBS.SDH_DIPAI_COUNT; i++) {
				tableResponseSdh.addDiCardsData(seat_index == table._banker_select ? table.diPai[i] : -2);
			}
			tableResponseSdh.setDiCardCount(SDHConstants_YYBS.SDH_DIPAI_COUNT);

			tableResponseSdh.setStall(table.stall);
			tableResponseSdh.setRate(table.rate);
			if(table.lastCallMain != GameConstants.INVALID_SEAT){
				tableResponseSdh.setDifen(table.score);
			}else{
				tableResponseSdh.setDifen(1);
			}
			tableResponseSdh.setScore(table.allScore);
			tableResponseSdh.setBankerPlayer(table._cur_banker);
			tableResponseSdh.setMainColor( table._logic.m_cbMainColor);
			tableResponseSdh.setGameStatus(table._game_status);
			tableResponseSdh.setDiCardCount(0);

			for (int i = 0; i <= 3; i++) { // 可以叫的主花色及每个花色的数量
				tableResponseSdh.addMainColorList(i);
				tableResponseSdh.addMainColorNumber(table.colorNumber[i]);
			}
			tableResponseSdh.addMainColorNumber(table.ntMainCount[table._banker_select]);
			tableResponseSdh.setMainColorCount(5);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponseSdh));
		table.send_response_to_player(seat_index, roomResponse);

		if (!table.disPatchCardEnd) { //还在发牌状态
			table.switch_call_main(seat_index, GameConstants.INVALID_CARD);
		} else {
			if(table.call_main_after){ // 定主完的状态
				table.callAfterAction(table._cur_banker);
			}else{
				if(table.lastCallMain != GameConstants.INVALID_SEAT){
					table.sendMainColor(table.lastCallMain, table._logic.m_cbMainColor,seat_index,true);
				}
				if(!table.giveUpFanMian[seat_index]){ //定主状态：当前玩家
					table.switch_call_main_back(seat_index);
				}
			}
		}
		

		return true;
	}

}
