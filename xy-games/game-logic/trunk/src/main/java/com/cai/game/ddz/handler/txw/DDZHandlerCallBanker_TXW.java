package com.cai.game.ddz.handler.txw;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.game.ddz.DDZConstants;
import com.cai.game.ddz.DDZMsgConstants;
import com.cai.game.ddz.handler.DDZHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.txw.TxwRsp.TableResponse_Txw;

public class DDZHandlerCallBanker_TXW extends DDZHandlerCallBanker<TXW_Table> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public DDZHandlerCallBanker_TXW() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(TXW_Table table) {
		table._game_status = GameConstants.GS_CALL_BANKER;// 设置状态

		int call_action[] = new int[2];
		call_action[0] = 1;
		call_action[1] = 2;
		table.call_banker_resopnse(1, GameConstants.INVALID_SEAT, 0, _seat_index, call_action,
				GameConstants.INVALID_SEAT);

	}

	@Override
	public boolean handler_player_be_in_room(TXW_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_TXW_RECONNECT_DATA);

		TableResponse_Txw.Builder tableResponse_ddz = TableResponse_Txw.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_ddz);
		tableResponse_ddz.setRoomInfo(table.getRoomInfo());

		tableResponse_ddz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_ddz.setCurrentPlayer(GameConstants.INVALID_SEAT);
		tableResponse_ddz.setPrevPlayer(GameConstants.INVALID_SEAT);

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
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			tableResponse_ddz.addCardsData(i, cards_card);
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
			}
		}
		tableResponse_ddz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_ddz.setPrOutCardType(table._turn_out_card_type);
		tableResponse_ddz.setPrOutCardPlayer(table._turn_out__player);
		tableResponse_ddz.setRound(table._round);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		int call_action[] = new int[2];
		call_action[0] = 1;
		call_action[1] = 2;
		table.call_banker_resopnse(1, GameConstants.INVALID_SEAT, 0, table._current_player, call_action,
				GameConstants.INVALID_SEAT);
		table.send_player_times(seat_index);
		table.send_callbaner_result();
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
	public boolean handler_call_banker(TXW_Table table, int seat_index, int call_banker, int qiang_bangker) {
		if (table._game_status != GameConstants.GS_CALL_BANKER || call_banker == -1) {
			return true;
		}
		if (table._call_action[seat_index] != -1) {
			return true;
		}

		table._call_action[seat_index] = call_banker;
		table.cancelShedule(table.ID_TIMER_AUTO_OPREATE);
		table.cancelShedule(table.ID_TIMER_AUTO_TRUESS);
		if (call_banker == 1) {

			int next_player = (seat_index + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._call_action[i] == -1) {
					break;
				}
				if (i == table.getTablePlayerNumber() - 1) {
					table._current_player = GameConstants.INVALID_SEAT;
				}
			}

		} else {
			table.GRR._banker_player = seat_index;
			table._current_player = (seat_index + 1) % table.getTablePlayerNumber();
			table._user_times[table.GRR._banker_player] = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					table._user_times[i] *= 2;
					table._user_times[table.GRR._banker_player] += table._user_times[i];
				}

			}
			table.send_player_times(GameConstants.INVALID_SEAT);
			if (table._call_action[table._current_player] == 1 && !table.has_rule(DDZConstants.GAME_RULE_TXW_TI_TWO)) {
				table._current_player = GameConstants.INVALID_SEAT;
			}
		}

		if (table.GRR._banker_player != GameConstants.INVALID_SEAT) {
			if (table._current_player == GameConstants.INVALID_SEAT) {
				table.schedule(table.ID_TIMER_ADD_TIME_OUT_OPREATE, SheduleArgs.newArgs(), 1500);
			}
			int cur_action[] = new int[2];
			cur_action[0] = 3;
			cur_action[1] = 4;
			table.call_banker_resopnse(1, seat_index, call_banker, table._current_player, cur_action,
					GameConstants.INVALID_SEAT);
			table._game_status = GameConstants.GS_DDZ_ADD_TIMES;// 设置状态
			table._handler = table._handler_add_times;
		} else {
			int cur_action[] = new int[2];
			cur_action[0] = 1;
			cur_action[1] = 2;
			table.call_banker_resopnse(1, seat_index, call_banker, table._current_player, cur_action,
					GameConstants.INVALID_SEAT);
			if (table._current_player == GameConstants.INVALID_SEAT) {
				table.schedule(table.ID_TIMER_ADD_TIME_OUT_OPREATE, SheduleArgs.newArgs(), 1500);
			}
		}
		table.send_callbaner_result();

		if (table._current_player != GameConstants.INVALID_SEAT) {
			if (table.istrustee[table._current_player]) {
				table.schedule(table.ID_TIMER_AUTO_OPREATE, SheduleArgs.newArgs(), 1000);
			} else {
				table.schedule(table.ID_TIMER_AUTO_TRUESS, SheduleArgs.newArgs(), table._truess_time);
			}
		}
		return true;
	}

}
