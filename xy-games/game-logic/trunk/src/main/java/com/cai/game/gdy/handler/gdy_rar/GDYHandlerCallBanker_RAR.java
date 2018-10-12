package com.cai.game.gdy.handler.gdy_rar;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.domain.SheduleArgs;
import com.cai.game.gdy.handler.GDYHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gdy.gdyRsp.CallBankerResponse_GDY;
import protobuf.clazz.gdy.gdyRsp.TableResponse_Gdy;

public class GDYHandlerCallBanker_RAR extends GDYHandlerCallBanker<GDYTable_RAR> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public GDYHandlerCallBanker_RAR() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(GDYTable_RAR table) {
		table._game_status = GameConstants.GS_CALL_BANKER;
		send_call_response(table, GameConstants.INVALID_SEAT, 0, GameConstants.INVALID_SEAT);

	}

	public void send_call_response(GDYTable_RAR table, int seat_index, int call_action, int to_player) {
		if (to_player == GameConstants.INVALID_SEAT) {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			CallBankerResponse_GDY.Builder call_banker_response = CallBankerResponse_GDY.newBuilder();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				call_banker_response.addAllCallAction(table._call_action[i]);
			}
			call_banker_response.setCallAction(call_action);
			call_banker_response.setCallPlayer(seat_index);
			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_CALL_BANKER_RESPONSE);// 201

			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_response));
			table.send_response_to_room(roomResponse);
		} else {
			RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
			CallBankerResponse_GDY.Builder call_banker_response = CallBankerResponse_GDY.newBuilder();
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				call_banker_response.addAllCallAction(table._call_action[i]);
			}
			call_banker_response.setCallAction(call_action);
			call_banker_response.setCallPlayer(seat_index);
			roomResponse.setType(MsgConstants.RESPONSE_GDEYE_CALL_BANKER_RESPONSE);// 201

			roomResponse.setCommResponse(PBUtil.toByteString(call_banker_response));
			table.send_response_to_player(to_player, roomResponse);
		}

	}

	@Override
	public boolean handler_player_be_in_room(GDYTable_RAR table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_GDEYE_RECONNECT_DATA);

		TableResponse_Gdy.Builder tableResponse_gdy = TableResponse_Gdy.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_gdy);
		tableResponse_gdy.setRoomInfo(table.getRoomInfo());

		tableResponse_gdy.setBankerPlayer(table.GRR._banker_player);
		tableResponse_gdy.setCurrentPlayer(GameConstants.INVALID_SEAT);
		tableResponse_gdy.setPrevPlayer(table._prev_palyer);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_gdy.addOutCardsCount(table.GRR._cur_round_count[i]);
			tableResponse_gdy.addPlayerPass(table.GRR._cur_round_pass[i]);
			Int32ArrayResponse.Builder out_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder out_change_cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder hand_cards = Int32ArrayResponse.newBuilder();
			for (int j = 0; j < table.GRR._cur_round_count[i]; j++) {
				if (table.GRR._cur_round_count[i] > 0) {
					out_cards.addItem(table.GRR._cur_round_data[i][j]);
					out_change_cards.addItem(table.GRR._cur_change_round_data[i][j]);
				}
			}
			if (i == seat_index) {
				for (int j = 0; j < table.GRR._card_count[i]; j++) {
					hand_cards.addItem(table.GRR._cards_data[i][j]);
				}
			}
			tableResponse_gdy.addHandCardsData(hand_cards);
			if (table.has_rule(GameConstants.GAME_RULE_GDY_CARD_NUMBER) || i == seat_index) {
				tableResponse_gdy.addHandCardCount(table.GRR._card_count[i]);
			} else {
				tableResponse_gdy.addHandCardCount(-1);
			}

			tableResponse_gdy.addOutCardsData(i, out_cards);
			tableResponse_gdy.addOutChangeCardsData(out_change_cards);
		}
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_gdy.setCurPlayerYaPai(1);
			} else {
				if (table._logic.search_card_data_hb(table._turn_out_real_card_data, table._turn_out_card_data,
						table._turn_out_card_count, table.GRR._cards_data[seat_index],
						table.GRR._card_count[seat_index])) {
					tableResponse_gdy.setCurPlayerYaPai(1);
				} else {
					tableResponse_gdy.setCurPlayerYaPai(0);
				}
			}
		} else {
			tableResponse_gdy.setCurPlayerYaPai(0);
		}
		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			cards_card.addItem(table.GRR._cards_data[seat_index][j]);
		}
		for (int i = 0; i < table._turn_out_card_count; i++) {
			if (table._turn_out_card_count > 0) {
				tableResponse_gdy.addPrCardsData(table._turn_out_card_data[i]);
				tableResponse_gdy.addPrChangeCardsData(table._turn_out_card_data[i]);
			}
		}
		tableResponse_gdy.setPrCardsCount(table._turn_out_card_count);
		tableResponse_gdy.setPrOutCardType(table._turn_out_card_type);
		tableResponse_gdy.setPrOutCardPlayer(table._out_card_player);
		tableResponse_gdy.setIsFirstOut(table._is_shou_chu);
		tableResponse_gdy.setLeftCardCount(table.GRR._left_card_count);
		tableResponse_gdy.setTimesNum(table._boom_times);
		tableResponse_gdy.setGameCell((int) table.game_cell);
		if (table._current_player == seat_index) {
			if (table._turn_out_card_count == 0) {
				tableResponse_gdy.setIsCurrentFirstOut(1);
				tableResponse_gdy.setDisplayTime(15);
			} else {
				tableResponse_gdy.setIsCurrentFirstOut(0);
				tableResponse_gdy.setDisplayTime(3);
			}
		} else {
			tableResponse_gdy.setIsCurrentFirstOut(0);
			tableResponse_gdy.setDisplayTime(15);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_gdy));
		table.send_response_to_player(seat_index, roomResponse);

		send_call_response(table, GameConstants.INVALID_SEAT, 0, seat_index);
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
	public boolean handler_call_banker(GDYTable_RAR table, int seat_index, int call_banker, int qiang_bangker) {

		if (table._game_status != GameConstants.GS_CALL_BANKER || table._call_action[seat_index] != -1) {
			return true;
		}
		table._call_action[seat_index] = call_banker;

		send_call_response(table, seat_index, call_banker, GameConstants.INVALID_SEAT);

		boolean is_have_call = false;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._call_action[i] == -1) {
				return true;
			} else if (table._call_action[i] == 1) {
				is_have_call = true;
			}
		}

		if (!is_have_call) {
			table.schedule(table.ID_TIMER_REDISPATH, SheduleArgs.newArgs(), 1500);
			return true;
		}
		int rand = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
		int rand_num = 0;
		for (int i = 0; i < 10; i++) {
			if (table._call_action[i % table.getTablePlayerNumber()] == 1) {
				if (rand_num == rand) {
					table._cur_banker = i % table.getTablePlayerNumber();
					table.GRR._banker_player = table._cur_banker;
					table._current_player = table.GRR._banker_player;
					break;
				}
				rand_num++;
			}

		}
		table.set_handler(table._handler_out_card_operate);
		table._game_status = GameConstants.GS_MJ_PLAY;
		table.schedule(table.ID_TIMER_CALL_BANKER_FINISH, SheduleArgs.newArgs(), 1500);
		return true;
	}

}
