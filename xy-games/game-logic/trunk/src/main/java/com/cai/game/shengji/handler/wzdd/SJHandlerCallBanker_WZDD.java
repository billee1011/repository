package com.cai.game.shengji.handler.wzdd;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.handler.SJHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.wzdd.wzddRsp.CallBankerResponse_Wzdd;
import protobuf.clazz.wzdd.wzddRsp.TableResponse_Wzdd;

public class SJHandlerCallBanker_WZDD extends SJHandlerCallBanker<SJTable_WZDD> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public SJHandlerCallBanker_WZDD() {
		_seat_index = GameConstants.INVALID_SEAT;
	}

	public void reset_status(int seat_index, int call_action) {
		_seat_index = seat_index;
		_call_action = call_action;
	}

	@Override
	public void exe(SJTable_WZDD table) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_CALL_BANKER);
		roomResponse.setRoomInfo(table.getRoomInfo());
		// 发送数据
		CallBankerResponse_Wzdd.Builder callbanker_response = CallBankerResponse_Wzdd.newBuilder();
		callbanker_response.setRoomInfo(table.getRoomInfo());
		callbanker_response.setCallPlayer(GameConstants.INVALID_SEAT);
		callbanker_response.setBankerPlayer(table.GRR._banker_player);
		callbanker_response.setCurrentPlayer(table._current_player);
		callbanker_response.setDangMin(table._min_dang);
		callbanker_response.setDangMax(table._max_dang);
		callbanker_response.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_response));

		table.send_response_to_room(roomResponse);

	}

	@Override
	public boolean handler_player_be_in_room(SJTable_WZDD table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_RECONNECT_DATA);
		// 发送数据
		TableResponse_Wzdd.Builder tableResponse = TableResponse_Wzdd.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		if (table._game_status != GameConstants.GS_WZDD_PLAY) {
			tableResponse.setCurrentPlayer(GameConstants.INVALID_SEAT);
		} else {
			tableResponse.setCurrentPlayer(table._current_player);
		}
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		table.load_player_info_data_reconnect(tableResponse);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_cards_card = Int32ArrayResponse.newBuilder();
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					cards_card.addItem(table.GRR._cards_data[i][j]);
				}
			}
			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				out_cards_card.addItem(table._cur_out_card_data[i][j]);
			}
			tableResponse.addOutCardsType(table._cur_out_card_type[i]);
			tableResponse.addOutCardsCount(table._cur_out_card_count[i]);
			tableResponse.addOutCardsData(out_cards_card);
			tableResponse.addCardsData(cards_card);
			tableResponse.addCardCount(table.GRR._card_count[i]);
			if (table._game_status == GameConstants.GS_WZDD_CALL_BANKER) {
				tableResponse.addSelectDang(table._select_dang[i]);
			} else {
				tableResponse.addSelectDang(-1);
			}

		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_out_card_data[i]);
		}
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse.setIsFirstOut(1);
			} else {
				tableResponse.setIsFirstOut(0);
			}
		} else {
			tableResponse.setIsFirstOut(0);
		}
		tableResponse.setPrOutCardType(table._turn_out_card_type);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setZhuType(table._zhu_type);
		if (table.GRR._banker_player == GameConstants.INVALID_SEAT) {
			tableResponse.setCallDang(-1);
		} else {
			tableResponse.setCallDang(table._select_dang[table.GRR._banker_player]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		// 自己才有牌数据
		table.send_response_to_player(seat_index, roomResponse);
		table.send_zhu_pai_type(seat_index);
		if (table._game_status == GameConstants.GS_WZDD_PLAY) {
			table.send_zhu_count(seat_index);
		}
		if (table._game_status == GameConstants.GS_WZDD_CALL_BANKER) {
			roomResponse.setType(MsgConstants.RESPONSE_WZDD_CALL_BANKER);
			roomResponse.setRoomInfo(table.getRoomInfo());
			// 发送数据
			CallBankerResponse_Wzdd.Builder callbanker_response = CallBankerResponse_Wzdd.newBuilder();
			callbanker_response.setRoomInfo(table.getRoomInfo());
			callbanker_response.setCallPlayer(_seat_index);
			callbanker_response.setBankerPlayer(table.GRR._banker_player);
			callbanker_response.setCurrentPlayer(table._current_player);
			callbanker_response.setDangMin(table._min_dang);
			callbanker_response.setDangMax(table._max_dang);
			callbanker_response.setOpereteDangNum(_call_action);
			callbanker_response.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_response));

			table.send_response_to_player(seat_index, roomResponse);
		}
		if (table._game_status == GameConstants.GS_WZDD_MAI_DI) {
			table.send_mai_di_begin(seat_index);
		}
		if (table._game_status == GameConstants.GS_WZDD_DING_ZHU) {
			table.send_ding_zhu_begin(seat_index);

		}

		table.RefreshScore(seat_index);
		table.send_history(seat_index);

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
	public boolean handler_call_banker(SJTable_WZDD table, int seat_index, int call_action) {
		if (table._game_status != GameConstants.GS_WZDD_CALL_BANKER) {
			return true;
		}
		if (seat_index != table._current_player) {
			return true;
		}
		if (call_action < table._min_dang && call_action != 0) {
			return true;
		}
		if (table._cur_banker == seat_index && call_action < table._min_dang) {
			return true;
		}
		boolean call_finish = false;

		if (call_action == table._max_dang) {
			table._select_dang[seat_index] = call_action;
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._min_dang = call_action + 1;
		} else {
			if (call_action != 0) {
				table._min_dang = call_action + 10;
				table._prv_call_player = seat_index;
			}

			table._select_dang[seat_index] = call_action;
			int next_player = (seat_index + 1) % table.getTablePlayerNumber();
			int num = 0;
			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				if (table._select_dang[j] == -1) {
					num++;
				}
			}
			if (num == 0) {
				call_finish = true;
				table.GRR._banker_player = table._prv_call_player;
				table._current_player = GameConstants.INVALID_SEAT;
			} else {
				table._current_player = next_player;
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WZDD_CALL_BANKER);
		roomResponse.setRoomInfo(table.getRoomInfo());
		// 发送数据
		CallBankerResponse_Wzdd.Builder callbanker_response = CallBankerResponse_Wzdd.newBuilder();
		callbanker_response.setRoomInfo(table.getRoomInfo());
		callbanker_response.setCallPlayer(seat_index);
		callbanker_response.setBankerPlayer(table.GRR._banker_player);
		callbanker_response.setCurrentPlayer(table._current_player);
		callbanker_response.setDangMin(table._min_dang);
		callbanker_response.setDangMax(table._max_dang);
		callbanker_response.setOpereteDangNum(call_action);
		callbanker_response.setDisplayTime(10);
		roomResponse.setCommResponse(PBUtil.toByteString(callbanker_response));

		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		if (table.GRR._banker_player != GameConstants.INVALID_SEAT) {
			table.get_di_pai_card();
			table.send_ding_zhu_begin(GameConstants.INVALID_SEAT);
			table.RefreshScore(GameConstants.INVALID_SEAT);
		} else if (call_finish) {
			int delay = 1;
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT, GameConstants.Game_End_DRAW),
					delay, TimeUnit.SECONDS);
			// table.Reset();
			// table.Send_card();
		}
		return true;
	}

}
