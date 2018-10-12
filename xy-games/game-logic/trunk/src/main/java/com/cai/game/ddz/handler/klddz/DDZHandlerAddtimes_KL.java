package com.cai.game.ddz.handler.klddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.common.util.PBUtil;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.AddTimesDDZResult;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;
import protobuf.clazz.ddz.DdzRsp.TableResponseDDZ;

public class DDZHandlerAddtimes_KL extends DDZHandlerAddtimes<DDZ_KL_Table> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public DDZHandlerAddtimes_KL() {
	}

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}

	@Override
	public void exe(DDZ_KL_Table table) {
		table._game_status = GameConstants.GS_DDZ_ADD_TIMES;// 设置状态

		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_QIANG_DI_ZHU_WAN_FA)) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._playerStatus[i]._call_banker = -1;
				table.operate_player_data();
			}

		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i]._call_banker < 0 && i != table.GRR._banker_player) {
				table._current_player = i;
				table.send_add_times(table._current_player, i);
				// return;
			} else {
				table.send_add_times(table.GRR._banker_player, i);
			}
			if (i == table.GRR._banker_player) {
				table.send_add_times(i, i);
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._logic.sort_card_date_list(table.GRR._cards_data[i], table.GRR._card_count[i]);
			table.RefreshCard(i);
		}
	}

	@Override
	public boolean handler_player_be_in_room(DDZ_KL_Table table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_RECONNECT_DATA);

		TableResponseDDZ.Builder tableResponse_ddz = TableResponseDDZ.newBuilder();
		table.load_player_info_data_reconnect(tableResponse_ddz);
		RoomInfoDdz.Builder room_info = table.getRoomInfoDdz();
		tableResponse_ddz.setRoomInfo(room_info);

		tableResponse_ddz.setBankerPlayer(table.GRR._banker_player);
		tableResponse_ddz.setCurrentPlayer(table._current_player);
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
			tableResponse_ddz.addQiangBanker(table._qiang_banker[i]);
			tableResponse_ddz.addCallBanker(table._call_banker[i]);
			if (table._game_status == GameConstants.GS_DDZ_ADD_TIMES) {
				if (i == seat_index) {
					tableResponse_ddz.addAddTimes(table._add_times[i]);
				} else {
					tableResponse_ddz.addAddTimes(-1);
				}
			} else {
				tableResponse_ddz.addAddTimes(table._add_times[i]);
			}

			tableResponse_ddz.addDifenBombDes(table.get_boom_difen_des(i));
		}
		tableResponse_ddz.addQiangAction(table._qiang_action[0]);
		tableResponse_ddz.addQiangAction(table._qiang_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[0]);
		tableResponse_ddz.addCallAction(table._call_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[2]);
		tableResponse_ddz.addCallAction(table._call_action[3]);

		for (int i = 0; i < table._di_pai_card_count; i++) {
			tableResponse_ddz.addDiCardsData(table._di_pai_card_data[i]);
		}
		tableResponse_ddz.setDiCardCount(table._di_pai_card_count);
		tableResponse_ddz.setDiCardsType(table._di_pai_type);
		// 手牌--将自己的手牌数据发给自己
		Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
		for (int j = 0; j < table.GRR._card_count[seat_index]; j++) {
			if (table._logic.switch_card_to_idnex(table.GRR._cards_data[seat_index][j]) == table._logic.magic_card[0]
					&& table._magic_card != GameConstants.INVALID_CARD) {
				cards_card.addItem(table.GRR._cards_data[seat_index][j] + 0x100);
			} else {
				cards_card.addItem(table.GRR._cards_data[seat_index][j]);
			}
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
		tableResponse_ddz.setCurPlayerYaPai(1);
		if (table._turn_out_card_count == 0) {
			tableResponse_ddz.setIsFirstOut(1);
		} else {
			tableResponse_ddz.setIsFirstOut(0);
		}
		tableResponse_ddz.setDisplayTime(10);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);

		if (table.ming_pai && table._game_status == GameConstants.GS_MJ_PLAY) {
			table.ming_pai_operate();
		}

		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_JIAO_FEN_WAN_FA)) {
			if (seat_index == table.GRR._banker_player) {
				if (table._game_status == GameConstants.GS_DDZ_ADD_TIMES && !table._add_times_operate[seat_index]) {
					table.send_add_times(seat_index, seat_index);
				}
			} else {
				if (table._playerStatus[seat_index]._call_banker < 0
						&& table.has_rule(Constants_KL_DDZ.GAME_RULE_JIAO_FEN_WAN_FA)
						&& table._game_status == GameConstants.GS_DDZ_ADD_TIMES
						&& !table._add_times_operate[seat_index]) {
					table.send_add_times(seat_index, seat_index);
				}
			}
		} else {
			if (table._game_status == GameConstants.GS_DDZ_ADD_TIMES && !table._add_times_operate[seat_index]) {
				table.send_add_times(seat_index, seat_index);
			}
		}

		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
			table.send_lai_zi(0, seat_index);
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
	public boolean handler_call_banker(DDZ_KL_Table table, int seat_index, int addtimes) {
		if (table._add_times_operate[seat_index] || table._game_status != GameConstants.GS_DDZ_ADD_TIMES) {
			return false;
		}
		if (table._playerStatus[seat_index]._call_banker > 0 && seat_index != table.GRR._banker_player) {
			return false;
		}
		if (table._auto_add_time_scheduled[seat_index] != null) {
			table._auto_add_time_scheduled[seat_index].cancel(false);
			table._auto_add_time_scheduled[seat_index] = null;
		}
		if (table._trustee_auto_opreate_scheduled[seat_index] != null) {
			table._trustee_auto_opreate_scheduled[seat_index].cancel(false);
			table._trustee_auto_opreate_scheduled[seat_index] = null;
		}
		if (seat_index == table.GRR._banker_player) {
			if (addtimes == 0) {
				addtimes = 2;
			}
		}
		table._add_times_operate[seat_index] = true;
		table._add_times[seat_index] = addtimes;
		if (addtimes == 1) {
			table._user_times[seat_index]++;
			// 比赛场重置数据
			PlayerServiceImpl.getInstance().updateRoomInfo(table.getRoom_id());
		}

		if (seat_index == table.GRR._banker_player && addtimes == 1) {
			table.ming_pai = true;
		}
		// if (addtimes == 1) {
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// if (i == seat_index) {
		// continue;
		// }
		// if (table._add_times[i] == 1) {
		// addtimes = 2;
		// }
		// }
		// }

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES_RESULE);
		AddTimesDDZResult.Builder add_time_result = AddTimesDDZResult.newBuilder();
		add_time_result.setAddtimesaction(addtimes);
		add_time_result.setOpreatePlayer(seat_index);

		for (int j = 0; j < table.getTablePlayerNumber(); j++) {
			add_time_result.addDifenBombDes(table.get_boom_difen_des(j));
		}
		roomResponse.setCommResponse(PBUtil.toByteString(add_time_result));
		table.send_response_to_player(seat_index, roomResponse);
		table.GRR.add_room_response(roomResponse);

		// 等所有操作完才返回结果给所有人
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (!table._add_times_operate[i]) {
				if (i != table.GRR._banker_player && table._playerStatus[i]._call_banker < 0) {
					return true;
				}
				if (i == table.GRR._banker_player) {
					return true;
				}
			}
		}
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (i == table.GRR._banker_player || table._add_times[i] == -1) {
				continue;
			}
			RoomResponse.Builder roomResponse2 = RoomResponse.newBuilder();
			roomResponse2.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES_RESULE);
			AddTimesDDZResult.Builder add_time_result2 = AddTimesDDZResult.newBuilder();
			add_time_result2.setAddtimesaction(table._add_times[i]);
			add_time_result2.setOpreatePlayer(i);

			for (int j = 0; j < table.getTablePlayerNumber(); j++) {
				add_time_result2.addDifenBombDes(table.get_boom_difen_des(j));
			}
			roomResponse2.setCommResponse(PBUtil.toByteString(add_time_result2));
			table.send_response_to_room(roomResponse2);
			table.GRR.add_room_response(roomResponse2);
		}
		if (table.ming_pai) {
			table.jia_bei_operate();
		}
		table.ming_pai_operate();
		// int next_player = (table._current_player + 1) %
		// table.getTablePlayerNumber();
		// for (int i = 0; i < table.getTablePlayerNumber(); i++) {
		// if(!table._add_times_operate[seat_index]){
		// return true;
		// }
		// if (table._playerStatus[next_player]._call_banker == -1 &&
		// next_player != table.GRR._banker_player
		// && !table._add_times_operate[next_player]) {
		// table._current_player = next_player;
		// table.send_add_times(table._current_player,
		// GameConstants.INVALID_SEAT);
		// return true;
		// } else {
		// next_player = (next_player + 1) % table.getTablePlayerNumber();
		// }
		// }

		table._current_player = table.GRR._banker_player;
		table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,
				GameConstants.INVALID_SEAT);
		return true;
	}

}
