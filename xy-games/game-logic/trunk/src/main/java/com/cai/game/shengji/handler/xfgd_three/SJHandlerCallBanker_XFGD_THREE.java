package com.cai.game.shengji.handler.xfgd_three;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.SJTable;
import com.cai.game.shengji.handler.SJHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.xfgd.xfgdRsp.CallBankerResponse_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.JiaoZhuBegin_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.MaiDiBegin_Xfgd;
import protobuf.clazz.xfgd.xfgdRsp.TableResponse_Xfgd;

public class SJHandlerCallBanker_XFGD_THREE extends SJHandlerCallBanker {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public SJHandlerCallBanker_XFGD_THREE() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(SJTable table) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_CALL_BANKER);
		roomResponse.setRoomInfo(table.getRoomInfo());
		// 发送数据
		CallBankerResponse_Xfgd.Builder callbanker_response = CallBankerResponse_Xfgd.newBuilder();
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
	public boolean handler_player_be_in_room(SJTable table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_RECONNECT_DATA);
		// 发送数据
		TableResponse_Xfgd.Builder tableResponse = TableResponse_Xfgd.newBuilder();
		tableResponse.setRoomInfo(table.getRoomInfo());
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);

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
			tableResponse.addSelectDang(table._select_dang[i]);
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

		if (seat_index == table._current_player && table._game_status == GameConstants.GS_XFGD_CALL_BANKER) {
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_CALL_BANKER);
			roomResponse.setRoomInfo(table.getRoomInfo());
			// 发送数据
			CallBankerResponse_Xfgd.Builder callbanker_response = CallBankerResponse_Xfgd.newBuilder();
			callbanker_response.setRoomInfo(table.getRoomInfo());
			callbanker_response.setCallPlayer(GameConstants.INVALID_SEAT);
			callbanker_response.setBankerPlayer(table.GRR._banker_player);
			callbanker_response.setCurrentPlayer(table._current_player);
			callbanker_response.setDangMin(table._min_dang);
			callbanker_response.setDangMax(table._max_dang);
			callbanker_response.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(callbanker_response));

			table.send_response_to_player(seat_index, roomResponse);
		}
		if (seat_index == table._current_player && table._game_status == GameConstants.GS_XFGD_MAI_DI) {
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_MAI_DI_BEGIN);
			roomResponse.setRoomInfo(table.getRoomInfo());
			MaiDiBegin_Xfgd.Builder mai_pai_begin = MaiDiBegin_Xfgd.newBuilder();

			int card_data[] = new int[table.GRR._card_count[table.GRR._banker_player] + table._di_pai_count];
			for (int i = 0; i < table.GRR._card_count[table.GRR._banker_player]; i++) {
				card_data[i] = table.GRR._cards_data[table.GRR._banker_player][i];
			}
			for (int i = 0; i < table._di_pai_count; i++) {
				card_data[table.GRR._card_count[table.GRR._banker_player] + i] = table._di_pai[i];
			}
			table._logic.SortCardList(card_data, table.GRR._card_count[table.GRR._banker_player] + table._di_pai_count);
			for (int i = 0; i < table.GRR._card_count[table.GRR._banker_player] + table._di_pai_count; i++) {
				mai_pai_begin.addCardsData(card_data[i]);
			}
			mai_pai_begin.setSeatIndex(table.GRR._banker_player);
			mai_pai_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(mai_pai_begin));
			table.send_response_to_player(seat_index, roomResponse);
		}
		if (seat_index == table._current_player && table._game_status == GameConstants.GS_XFGD_DING_ZHU) {
			// 发送数据
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_JIAO_ZHU_BEGIN);
			roomResponse.setRoomInfo(table.getRoomInfo());
			JiaoZhuBegin_Xfgd.Builder jiaozhu_begin = JiaoZhuBegin_Xfgd.newBuilder();
			for (int i = 3; i >= 0; i--) {
				int count = table._logic.GetCardColor_Count(table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index], i);
				jiaozhu_begin.addCount(count);
			}
			jiaozhu_begin.setSeatIndex(table.GRR._banker_player);
			jiaozhu_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(jiaozhu_begin));

			table.send_response_to_player(seat_index, roomResponse);
		}
		table.send_history(seat_index);
		table.RefreshScore(seat_index);

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
	public boolean handler_call_banker(SJTable table, int seat_index, int call_action) {
		if (table._game_status != GameConstants.GS_XFGD_CALL_BANKER) {
			return true;
		}
		if (seat_index != table._current_player) {
			return true;
		}
		if (call_action < table._min_dang && call_action != 0) {
			return true;
		}
		boolean is_reset = false;
		table._prv_call_player = seat_index;
		if (call_action == table._max_dang) {
			table._select_dang[seat_index] = call_action;
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._min_dang = call_action + 1;
		} else {
			if (call_action != 0) {
				table._min_dang = call_action + 1;
			}

			table._select_dang[seat_index] = call_action;
			int next_player = (seat_index + 1) % table.getTablePlayerNumber();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._select_dang[next_player] == 0) {
					next_player = (next_player + 1) % table.getTablePlayerNumber();
					int num = 0;
					for (int j = 0; j < table.getTablePlayerNumber(); j++) {
						if (table._select_dang[j] != 0) {
							num++;
						}
					}
					if (num == 0) {
						is_reset = true;
					}
				} else {
					if (next_player == seat_index) {
						table.GRR._banker_player = seat_index;
						table._current_player = seat_index;
					} else {
						int num = 0;
						for (int j = 0; j < table.getTablePlayerNumber(); j++) {
							if (table._select_dang[j] != 0) {
								num++;
							}
						}
						if (num == 1) {
							if (table._select_dang[next_player] > 0) {
								table.GRR._banker_player = next_player;
							}

						}
						table._current_player = next_player;
					}

				}
			}
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_XFGD_CALL_BANKER);
		roomResponse.setRoomInfo(table.getRoomInfo());
		// 发送数据
		CallBankerResponse_Xfgd.Builder callbanker_response = CallBankerResponse_Xfgd.newBuilder();
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
			table._game_status = GameConstants.GS_XFGD_DING_ZHU;

			// 发送数据
			roomResponse.setType(MsgConstants.RESPONSE_XFGD_JIAO_ZHU_BEGIN);
			roomResponse.setRoomInfo(table.getRoomInfo());
			JiaoZhuBegin_Xfgd.Builder jiaozhu_begin = JiaoZhuBegin_Xfgd.newBuilder();
			for (int i = 3; i >= 0; i--) {
				int count = table._logic.GetCardColor_Count(table.GRR._cards_data[table.GRR._banker_player],
						table.GRR._card_count[table.GRR._banker_player], i);
				jiaozhu_begin.addCount(count);
			}
			jiaozhu_begin.setSeatIndex(table.GRR._banker_player);
			jiaozhu_begin.setDisplayTime(10);
			roomResponse.setCommResponse(PBUtil.toByteString(jiaozhu_begin));

			table.send_response_to_room(roomResponse);
			table.GRR.add_room_response(roomResponse);

			table._cur_banker = table.GRR._banker_player;
			table.send_history(GameConstants.INVALID_SEAT);
		} else if (is_reset) {
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
