package com.cai.game.ddz.handler.jxddz;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.game.ddz.handler.DDZHandlerAddtimes;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.AddTimesDDZResult;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;
import protobuf.clazz.ddz.DdzRsp.TableResponseDDZ;

public class DDZHandlerAddtimes_JX extends DDZHandlerAddtimes<DDZ_JX_Table> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public DDZHandlerAddtimes_JX() {
	}

	public void reset_status(int seat_index) {
		_seat_index = seat_index;
	}

	@Override
	public void exe(DDZ_JX_Table table) {
		table._game_status = GameConstants.GS_DDZ_ADD_TIMES;// 设置状态

		int next_player = (table.GRR._banker_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[next_player]._call_banker == -1 && next_player != table.GRR._banker_player) {
				table._current_player = next_player;
				table.send_add_times(table._current_player, GameConstants.INVALID_SEAT);
				return;
			} else {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			}
		}

		table._current_player = table.GRR._banker_player;
		table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,
				GameConstants.INVALID_SEAT);

	}

	@Override
	public boolean handler_player_be_in_room(DDZ_JX_Table table, int seat_index) {
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
			tableResponse_ddz.addAddTimes(table._add_times[i]);
			tableResponse_ddz.addDifenBombDes(table.get_boom_difen_des(i));
		}
		tableResponse_ddz.addQiangAction(table._qiang_action[0]);
		tableResponse_ddz.addQiangAction(table._qiang_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[0]);
		tableResponse_ddz.addCallAction(table._call_action[1]);
		tableResponse_ddz.addCallAction(table._call_action[2]);
		tableResponse_ddz.addCallAction(table._call_action[3]);

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
		if (seat_index == table.GRR._banker_player) {
			for (int i = 0; i < table._di_pai_card_count; i++) {
				tableResponse_ddz.addDiCardsData(table._di_pai_card_data[i]);
			}
		} else {
			for (int i = 0; i < table._di_pai_card_count; i++) {
				tableResponse_ddz.addDiCardsData(GameConstants.BLACK_CARD);
			}
		}
		tableResponse_ddz.setPrCardsCount(table._turn_out_card_count);
		tableResponse_ddz.setPrOutCardType(table._turn_out_card_type);
		tableResponse_ddz.setPrOutCardPlayer(table._turn_out__player);
		if (table._turn_out_card_count == 0) {
			tableResponse_ddz.setIsFirstOut(1);
		} else {
			tableResponse_ddz.setIsFirstOut(0);
		}
		// tableResponse_ddz.setCurPlayerYaPai(1);
		tableResponse_ddz.setDisplayTime(10);
		tableResponse_ddz.setMagicCard(GameConstants.INVALID_CARD);

		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
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
	public boolean handler_call_banker(DDZ_JX_Table table, int seat_index, int addtimes) {
		if (table._add_times_operate[seat_index] || table._game_status != GameConstants.GS_DDZ_ADD_TIMES) {
			return false;
		}
		if (table._playerStatus[seat_index]._call_banker != -1 && seat_index != table.GRR._banker_player) {
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
		table._add_times_operate[seat_index] = true;
		table._add_times[seat_index] = addtimes;
		if (addtimes == 1) {
			table._user_times[seat_index]++;
			// 比赛场重置数据
			PlayerServiceImpl.getInstance().updateRoomInfo(table.getRoom_id());
		}

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_ADD_TIMES_RESULE);
		AddTimesDDZResult.Builder add_time_result = AddTimesDDZResult.newBuilder();

		if (addtimes == 1) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == seat_index) {
					continue;
				}
				if (table._add_times[i] == 1) {
					addtimes = 2;
				}
			}
		}

		add_time_result.setAddtimesaction(addtimes);
		add_time_result.setOpreatePlayer(seat_index);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			add_time_result.addDifenBombDes(table.get_boom_difen_des(i));
		}
		roomResponse.setCommResponse(PBUtil.toByteString(add_time_result));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		int next_player = (table._current_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[next_player]._call_banker == -1 && next_player != table.GRR._banker_player
					&& !table._add_times_operate[next_player]) {
				table._current_player = next_player;
				table.send_add_times(table._current_player, GameConstants.INVALID_SEAT);
				return true;
			} else {
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			}
		}

		table._current_player = table.GRR._banker_player;
		table._game_status = GameConstants.GS_MJ_PLAY;// 设置状态
		table.operate_out_card(GameConstants.INVALID_SEAT, 0, null, GameConstants.DDZ_CT_ERROR,
				GameConstants.INVALID_SEAT);
		return true;
	}

}
