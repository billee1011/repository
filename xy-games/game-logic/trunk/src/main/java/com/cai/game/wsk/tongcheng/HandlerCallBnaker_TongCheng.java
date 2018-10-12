package com.cai.game.wsk.tongcheng;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tcdg.TcdgRsp.CallBankerResponse_tcdg;
import protobuf.clazz.tcdg.TcdgRsp.TableResponse_tcdg;

public class HandlerCallBnaker_TongCheng extends AbstractHandler_TongCheng {
	@Override
	public void exe(Table_TongCheng table) {
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
	public boolean handler_call_banker(Table_TongCheng table, int seat_index, int call_action) {
		if (table._is_call_banker[seat_index] == 1 || table._current_player != seat_index) {
			return false;
		}

		table._is_call_banker[seat_index] = 1;

		boolean is_finish = true;

		if (call_action == 0) {
			int next_player = (table._current_player + 1) % table.getTablePlayerNumber();

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._is_call_banker[next_player] == 0) {
					is_finish = false;

					table._cur_banker = next_player;
					table._current_player = next_player;

					break;
				}
			}

			if (is_finish) {
				// 所有人已经操作了‘不独’之后，开始叫牌
				table._game_status = GameConstants.GS_TC_WSK_LIANG_PAI;
				table._current_player = (table._current_player + 1) % table.getTablePlayerNumber();
			}
		} else {
			// 如果有人点了‘独牌’。牌局的庄家和牌桌的当前玩家，变成点了‘独牌’操作的玩家
			table._game_status = GameConstants.GS_TC_WSK_PLAY;
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._is_yi_da_san = true;

			table._solo_times[table._current_player]++;
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_CALL_BANKER_RESULT);

		// 字段含义请参考tongchengWSK.proto文件
		CallBankerResponse_tcdg.Builder callbanker_result = CallBankerResponse_tcdg.newBuilder();
		if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
			callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
		} else {
			callbanker_result.setBankerPlayer(table.GRR._banker_player);
		}
		callbanker_result.setOpreateAction(call_action); // 0不叫-1叫
		callbanker_result.setCallPlayer(seat_index); // 操作玩家。如果为-1，表示开始叫庄
		if (is_finish || table._is_yi_da_san)
			callbanker_result.setCurrentPlayer(-1); // 发送给客户端，下一个需要操作叫庄的玩家。如果为-1，表示已经叫庄结束
		else
			callbanker_result.setCurrentPlayer(table._current_player); // 发送给客户端，下一个需要操作叫庄的玩家。如果为-1，表示已经叫庄结束
		callbanker_result.setDisplayTime(5);
		callbanker_result.setRoomInfo(table.getRoomInfo());

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		table.send_response_to_room(roomResponse);

		table.GRR.add_room_response(roomResponse);

		if (call_action == 1) {
			// 发送一个空的出牌消息，告诉客户端，开始打牌了
			table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.TCDG_CT_ERROR, GameConstants.INVALID_SEAT, false);
		}

		if (table._game_status == GameConstants.GS_TC_WSK_LIANG_PAI) {
			// 如果牌桌状态是叫牌状态了，需要服务端主动处理叫牌
			table.auto_liang_pai(table._current_player);
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TongCheng table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_RECONNECT_DATA);

		TableResponse_tcdg.Builder tableResponse = TableResponse_tcdg.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
			tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
			tableResponse.setBankerPlayer(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setCurrentPlayer(table.GRR._banker_player);
			tableResponse.setBankerPlayer(table.GRR._banker_player);
		}
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

		if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
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
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_CALL_BANKER_RESULT);

		CallBankerResponse_tcdg.Builder callbanker_result = CallBankerResponse_tcdg.newBuilder();
		callbanker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
		callbanker_result.setOpreateAction(GameConstants.INVALID_SEAT); // 0不叫-1叫
		callbanker_result.setCallPlayer(seat_index); // 操作玩家。如果为-1，表示开始叫庄
		if (table._is_yi_da_san) {
			callbanker_result.setCurrentPlayer(GameConstants.INVALID_SEAT); // 发送给客户端，下一个需要操作叫庄的玩家。如果为-1，表示已经叫庄结束
		} else {
			if (table._game_status == GameConstants.GS_TC_WSK_CALLBANKER) {
				callbanker_result.setCurrentPlayer(table._current_player); // 发送给客户端，下一个需要操作叫庄的玩家。如果为-1，表示已经叫庄结束
			} else {
				callbanker_result.setCurrentPlayer(GameConstants.INVALID_SEAT);
			}
		}

		callbanker_result.setDisplayTime(5);
		callbanker_result.setRoomInfo(table.getRoomInfo());

		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_result));

		table.send_response_to_player(seat_index, roomResponse);

		return true;
	}

}
