package com.cai.game.wsk.xiangtanxiaozao;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tcdg.TcdgRsp.CallBankerResponse_tcdg;
import protobuf.clazz.tcdg.TcdgRsp.TableResponse_tcdg;
import protobuf.clazz.xtxz.xtxzRsp.BiaoTaiResponse_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.CallBankerResponse_xtxz;
import protobuf.clazz.xtxz.xtxzRsp.TableResponse_xtxz;

public class HandlerBiaoTai_XTXZ extends AbstractHandler_XTXZ {
	@Override
	public void exe(Table_XTXZ table) {
		
	}

	/**
	 * 接收客户端发的独牌操作
	 * 
	 * @param table
	 * @param seat_index
	 * @param call_action
	 *            0不独，1独牌
	 * @return
	 */
	public boolean handler_bai_tai(Table_XTXZ table, int seat_index, int call_action) {
		if (table._is_biao_tai[seat_index] != -1 || table._current_player != seat_index) {
			return false;
		}

		table._is_biao_tai[seat_index] = call_action;
		
		table._player_result.biaoyan[seat_index] = call_action;

		boolean is_finish = true;

		int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._is_biao_tai[next_player] == -1) {
				is_finish = false;

				table._cur_banker = next_player;
				table._current_player = next_player;

				break;
			}
		}
		

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_BIAO_TAI);

		// 字段含义请参考tongchengWSK.proto文件
		BiaoTaiResponse_xtxz.Builder callbanker_result = BiaoTaiResponse_xtxz.newBuilder();
		callbanker_result.setOpreateAction(call_action); // 
		callbanker_result.setCallPlayer(seat_index); //
		if (is_finish)
			callbanker_result.setCurrentPlayer(-1); //
		else
			callbanker_result.setCurrentPlayer(table._current_player); //
		callbanker_result.setDisplayTime(10);
		callbanker_result.setRoomInfo(table.getRoomInfo());
		
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			callbanker_result.addBiaotaiAction(table._is_biao_tai[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		table.send_response_to_room(roomResponse);

		table.GRR.add_room_response(roomResponse);
		
		if (is_finish) {
			//所有人都操作了就，直接开始了
			table.set_handler(table._handler_out_card_operate);
			table._current_player = table.GRR._banker_player;
			table._game_status = GameConstants.GS_XTXZ_WSK_PLAY;
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.TCDG_CT_ERROR, GameConstants.INVALID_SEAT, false);
	
		}


		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_XTXZ table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_RECONNECT_DATA);

		TableResponse_xtxz.Builder tableResponse = TableResponse_xtxz.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());


		tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
		tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		
		tableResponse.setPrevPlayer(table._prev_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);

		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}

		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}

		if (table._out_card_ming_ji != GameConstants.INVALID_CARD && table.GRR._card_count[seat_index] == 0) {
			tableResponse.setFriendSeatIndex(table._friend_seat[seat_index]);
		} else {
			tableResponse.setFriendSeatIndex(GameConstants.INVALID_SEAT);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse.addCardCount(table.GRR._card_count[i]);

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();

			if (seat_index == i) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards.addItem(table.GRR._cards_data[i][j]);
				}
			}

			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}

			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			tableResponse.addWinOrder(table._chuwan_shunxu[i]);
		}

		if (table._game_status == GameConstants.GS_XTXZ_WSK_CALLBANKER) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				tableResponse.addIsCallBanker(table._is_call_banker[i] == 1 ? true : false);
			}
		}

		if (table._out_card_ming_ji == GameConstants.INVALID_CARD) {
			tableResponse.setBankerFriendSeat(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setBankerFriendSeat(table._friend_seat[table.GRR._banker_player]);
		}

		tableResponse.setJiaoCardData(table._jiao_pai_card);
		tableResponse.setIsYiDaSan(table._is_yi_da_san);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.send_response_to_player(seat_index, roomResponse);

		table.refresh_user_get_score(seat_index);

		roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_XTXZ_BIAO_TAI);

		// 字段含义请参考tongchengWSK.proto文件
		BiaoTaiResponse_xtxz.Builder baitai_result = BiaoTaiResponse_xtxz.newBuilder();
		baitai_result.setOpreateAction(-1); // 
		baitai_result.setCallPlayer(seat_index); //
		baitai_result.setCurrentPlayer(table._current_player); // 发送给客户端，下一个需要操作叫庄的玩家。如果为-1，表示已经叫庄结束
		baitai_result.setDisplayTime(10);
		baitai_result.setRoomInfo(table.getRoomInfo());
		for(int i = 0;i < table.getTablePlayerNumber();i++){
			baitai_result.addBiaotaiAction(table._is_biao_tai[i]);
		}
		
		roomResponse.setCommResponse(PBUtil.toByteString(baitai_result));

		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
