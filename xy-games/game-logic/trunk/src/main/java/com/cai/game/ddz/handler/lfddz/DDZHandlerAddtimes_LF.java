package com.cai.game.ddz.handler.lfddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.domain.SheduleArgs;
import com.cai.game.ddz.DDZConstants;
import com.cai.game.ddz.DDZMsgConstants;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz_lf.ddz_lfRsp.TableResponse_DDZ_LF;

public class DDZHandlerAddtimes_LF extends DDZHandlerAddtimes<DDZ_LF_Table> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public DDZHandlerAddtimes_LF() {
	}

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}

	@Override
	public void exe(DDZ_LF_Table table) {
		if (table.has_rule(DDZConstants.GAME_RULE_LF_DDZ_NO_TI)) {
			table.exe_add_times_finish();
			return;
		}
		table._game_status = GameConstants.GS_DDZ_ADD_TIMES;// 设置状态
		int next_player = _seat_index;
		if (table._call_action[_seat_index] != -1) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				next_player = (_seat_index + 1) % table.getTablePlayerNumber();
				if (table._call_action[next_player] == -1) {
					break;
				}
				if (i == table.getTablePlayerNumber() - 1) {
					table.exe_add_times_finish();
					return;
				}
			}
		}
		table._current_player = next_player;
		int cur_cation[] = new int[2];
		cur_cation[0] = 0;
		cur_cation[1] = 1;
		table.call_banker_resopnse(2, 2, GameConstants.INVALID_SEAT, 0, next_player, cur_cation,
				GameConstants.INVALID_SEAT);
	}

	@Override
	public boolean handler_player_be_in_room(DDZ_LF_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(DDZMsgConstants.RESPONSE_DDZ_RECONNECT_DATA);

		TableResponse_DDZ_LF.Builder tableResponse_ddz = TableResponse_DDZ_LF.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_ddz);
		tableResponse_ddz.setRoomInfo(table.getRoomInfo());

		tableResponse_ddz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_ddz.setCurrentPlayer(GameConstants.INVALID_SEAT);
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

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		if (seat_index == table.GRR._banker_player) {
			table.Send_di_Card(false, true, seat_index);
		} else {
			table.Send_di_Card(false, false, seat_index);
		}

		if (seat_index == table._current_player) {
			if (seat_index == table.GRR._banker_player) {
				int cur_cation[] = new int[2];
				cur_cation[0] = 0;
				cur_cation[1] = 2;
				table.call_banker_resopnse(3, 2, GameConstants.INVALID_SEAT, 0, table._current_player, cur_cation,
						seat_index);
			} else {
				int cur_cation[] = new int[2];
				cur_cation[0] = 0;
				cur_cation[1] = 1;
				table.call_banker_resopnse(2, 2, GameConstants.INVALID_SEAT, 0, table._current_player, cur_cation,
						seat_index);
			}

		} else {
			int cur_cation[] = new int[2];
			cur_cation[0] = 0;
			cur_cation[1] = 1;
			table.call_banker_resopnse(2, 2, GameConstants.INVALID_SEAT, 0, table._current_player, cur_cation,
					seat_index);
		}

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
	public boolean handler_call_banker(DDZ_LF_Table table, int seat_index, int addtimes) {
		if (table._add_times[seat_index] != -1 || table._game_status != GameConstants.GS_DDZ_ADD_TIMES) {
			return false;
		}
		if (table._call_action[seat_index] != -1 && seat_index != table.GRR._banker_player) {
			return false;
		}

		table._add_times[seat_index] = addtimes;
		if (addtimes > 0) {
			if (seat_index == table.GRR._banker_player) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i != table.GRR._banker_player && table._add_times[i] > 0) {
						table._user_times[i] *= 2;
					}

				}
			} else {
				table._user_times[seat_index] *= 2;
			}
			table._user_times[table.GRR._banker_player] = 0;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i != table.GRR._banker_player) {
					table._user_times[table.GRR._banker_player] += table._user_times[i];
				}
			}
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == table.GRR._banker_player) {
					table.Send_di_Card(false, true, i);
				} else {
					table.Send_di_Card(false, false, i);
				}

			}
			table.Send_di_Card(false, true, GameConstants.INVALID_SEAT);
		}
		if (addtimes > 0 && table._call_action[table.GRR._banker_player] != -1) {
			table._call_action[table.GRR._banker_player] = -1;
		}
		int next_player = (seat_index + 1) % table.getTablePlayerNumber();
		if (table._add_times[next_player] != -1 || table._call_action[next_player] != -1) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
				if (table._add_times[next_player] == -1 && table._call_action[next_player] == -1) {
					table._current_player = next_player;
					break;
				}
				if (i == table.getTablePlayerNumber() - 1) {
					table.schedule(table.ID_TIMER_ADD_TIME_OUT_OPREATE, SheduleArgs.newArgs(), 1500);
					table._current_player = GameConstants.INVALID_SEAT;
					break;
				}
			}
		} else {
			table._current_player = next_player;
		}

		if (table._current_player == table.GRR._banker_player) {
			int cur_action[] = new int[2];
			cur_action[0] = 0;
			cur_action[1] = 2;
			table.call_banker_resopnse(3, 2, seat_index, addtimes, table._current_player, cur_action,
					GameConstants.INVALID_SEAT);
		} else {
			if (seat_index == table.GRR._banker_player) {
				int cur_action[] = new int[2];
				cur_action[0] = 0;
				cur_action[1] = 1;
				table.call_banker_resopnse(2, 3, seat_index, addtimes, table._current_player, cur_action,
						GameConstants.INVALID_SEAT);
			} else {
				int cur_action[] = new int[2];
				cur_action[0] = 0;
				cur_action[1] = 1;
				table.call_banker_resopnse(2, 2, seat_index, addtimes, table._current_player, cur_action,
						GameConstants.INVALID_SEAT);
			}

		}

		return true;
	}

}
