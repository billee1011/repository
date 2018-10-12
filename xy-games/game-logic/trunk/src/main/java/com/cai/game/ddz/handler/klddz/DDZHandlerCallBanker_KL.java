package com.cai.game.ddz.handler.klddz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.mj.Constants_KL_DDZ;
import com.cai.common.util.PBUtil;
import com.cai.common.util.RandomUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.ddz.handler.DDZHandlerCallBanker;
import com.cai.service.PlayerServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.ddz.DdzRsp.CallBankerDDZ;
import protobuf.clazz.ddz.DdzRsp.CallBankerResult;
import protobuf.clazz.ddz.DdzRsp.RoomInfoDdz;
import protobuf.clazz.ddz.DdzRsp.TableResponseDDZ;

public class DDZHandlerCallBanker_KL extends DDZHandlerCallBanker<DDZ_KL_Table> {
	protected int _seat_index;
	protected int _game_status;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public DDZHandlerCallBanker_KL() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(DDZ_KL_Table table) {

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i]._call_banker = -1;
			table.operate_player_data();
		}
		int qiang_action[] = new int[2];
		int call_action[] = new int[4];
		Arrays.fill(qiang_action, -1);
		Arrays.fill(call_action, -1);
		table._game_status = GameConstants.GS_CALL_BANKER;// 设置状态
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		int call_banker_type = 0;
		// if (table.has_rule(GameConstants.GAME_RULE_JIAO_DI_ZHU)) {
		// call_banker_type = 1;
		// call_action[0] = 0;
		// call_action[1] = 3;
		// } else
		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_JIAO_FEN_WAN_FA)) {
			call_banker_type = 2;
			call_action[0] = 0;
			call_action[1] = 1;
			call_action[2] = 2;
			call_action[3] = 3;
		} else {
			call_banker_type = 3;
			call_action[0] = 0;
			call_action[1] = 1;
		}
		table._call_banker_type = call_banker_type;
		table._call_banker_status = 1;
		int temp = 0;
		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_RAND_CALL_BANKER)) {
			temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
		} else {
			if (table._banker_select == GameConstants.INVALID_SEAT) {
				temp = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % table.getTablePlayerNumber());
			} else {
				temp = table._banker_select;
				table._banker_select = GameConstants.INVALID_SEAT;
			}
		}
		table._banker_select = GameConstants.INVALID_SEAT;
		table._current_player = temp;
		this._seat_index = table._current_player;
		CallBankerDDZ.Builder call_banker_ddz = CallBankerDDZ.newBuilder();
		call_banker_ddz.setCallCurrentPlayer(table._current_player);
		call_banker_ddz.setQiangCurrentPlayer(GameConstants.INVALID_SEAT);
		call_banker_ddz.setCallType(call_banker_type);
		call_banker_ddz.setDisplayTime(10);

		int fei_card_data = table.GRR._cards_data[table._current_player][RandomUtil.generateRandomNumber(0,
				table.GRR._card_count[table._current_player] - 1)];
		call_banker_ddz.setFeiPaiCard(fei_card_data);
		for (int i = 0; i < 4; i++) {
			table._call_action[i] = call_action[i];
			call_banker_ddz.addCallCurrentAction(call_action[i]);
		}
		for (int i = 0; i < 2; i++) {
			table._qiang_action[i] = qiang_action[i];
			call_banker_ddz.addQiangCurrentAction(qiang_action[i]);
		}

		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		// int dealy=0;
		// if(table.istrustee[table._current_player]){
		// dealy=1;
		// }else{
		// dealy=10;
		// }
		// table._auto_call_banker_scheduled=GameSchedule.put(new
		// DDZAutoCallbankerRunnable(table.getRoom_id(),
		// table,table._current_player,1),
		// dealy, TimeUnit.SECONDS);
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
			tableResponse_ddz.addAddTimes(table._add_times[i]);
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
		tableResponse_ddz.setCurPlayerYaPai(1);
		if (table._turn_out_card_count == 0) {
			tableResponse_ddz.setIsFirstOut(1);
		} else {
			tableResponse_ddz.setIsFirstOut(0);
		}
		tableResponse_ddz.setCallType(table._call_banker_type);
		tableResponse_ddz.setDisplayTime(10);
		tableResponse_ddz.setCurPlayerYaPai(1);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			tableResponse_ddz.addPiao(table._piao_fen[i]);
			tableResponse_ddz.addIsPiao(table._player_result.pao[i]);
		}
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse_ddz));
		table.send_response_to_player(seat_index, roomResponse);
		if (table.ming_pai) {
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
	public boolean handler_call_banker(DDZ_KL_Table table, int seat_index, int call_banker, int qiang_bangker) {
		if (table._call_banker_status == 0 || table._current_player != seat_index) {
			return true;
		} else if (table._call_banker_status == 1 && qiang_bangker != -1) {
			return true;
		} else if (table._call_banker_status == 2 && call_banker != -1) {
			return true;
		}
		if (table._current_player != seat_index) {
			return true;
		}
		if (table._call_banker_status == 2 && table._qiang_banker[seat_index] != -1) {
			return true;
		}
		if (table._call_banker_status == 1 && table._call_banker[seat_index] != -1) {
			return true;
		}
		if (table._game_status != GameConstants.GS_CALL_BANKER) {
			return true;
		}

		if (table._auto_call_banker_scheduled != null) {
			table._auto_call_banker_scheduled.cancel(false);
			table._auto_call_banker_scheduled = null;
		}

		if (call_banker != -1) {
			table._playerStatus[seat_index]._call_banker = call_banker;
		}
		if (qiang_bangker != -1) {
			table._playerStatus[seat_index]._qiang_banker = qiang_bangker;
		}
		table._qiang_banker[seat_index] = qiang_bangker;
		table._call_banker[seat_index] = call_banker;
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER_RESULT);
		// 刷新用户叫分
		table.operate_player_data();
		int qiang_player = GameConstants.INVALID_SEAT;
		int call_player = GameConstants.INVALID_SEAT;
		int cur_qiang_player = GameConstants.INVALID_SEAT;
		int cur_call_player = GameConstants.INVALID_SEAT;
		int qiang_action = -1;
		int call_action = -1;
		int call_banker_type = 2;
		int qiang_action_current[] = new int[2];
		int call_action_current[] = new int[4];
		Arrays.fill(qiang_action_current, -1);
		Arrays.fill(call_action_current, -1);

		if (table.has_rule(Constants_KL_DDZ.GAME_RULE_JIAO_FEN_WAN_FA)) {
			call_action = call_banker;
			call_player = seat_index;
			// 用户叫分
			if (call_banker == 3) {
				// 玩家叫3分该玩家即为地主
				table.GRR._banker_player = table._banker_select = seat_index;
				table.game_cell = call_banker;

				// 可叫分动作
				for (int i = 0; i < 4; i++) {
					table._call_action[i] = call_action_current[i];
				}
				for (int i = 0; i < 2; i++) {
					table._qiang_action[i] = qiang_action_current[i];
				}

				for (int i = 0; i < table._di_pai_card_count; i++) {
					table.GRR._cards_data[table.GRR._banker_player][i
							+ table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
				}
				table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
				table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player],
						table.GRR._card_count[table.GRR._banker_player]);

				for (int index = 0; index < table.getTablePlayerNumber(); index++) {
					CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
					call_banker_result.setQiangPlayer(qiang_player);
					call_banker_result.setCallPlayer(call_player);
					call_banker_result.setCallAction(call_action);
					call_banker_result.setQiangAction(qiang_action);
					call_banker_result.setBankerPlayer(table.GRR._banker_player);
					// if (index == table.GRR._banker_player) {
					for (int i = 0; i < table._di_pai_card_count; i++) {
						call_banker_result.addCardsData(table._di_pai_card_data[i]);
					}
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
					}
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
						if (i == index) {
							for (int j = 0; j < table.GRR._card_count[i]; j++) {
								cards_card.addItem(table.GRR._cards_data[i][j]);
							}

						}
						call_banker_result.addUserCardCount(table.GRR._card_count[i]);
						call_banker_result.addUserCardsData(cards_card);
					}
					roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
					table.send_response_to_player(index, roomResponse);
				}

				// 回放
				CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
				call_banker_result.setQiangPlayer(qiang_player);
				call_banker_result.setCallPlayer(call_player);
				call_banker_result.setCallAction(call_action);
				call_banker_result.setQiangAction(qiang_action);
				call_banker_result.setBankerPlayer(table.GRR._banker_player);
				for (int i = 0; i < table._di_pai_card_count; i++) {
					call_banker_result.addCardsData(table._di_pai_card_data[i]);
				}
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
				}
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards_card.addItem(table.GRR._cards_data[i][j]);
					}
					call_banker_result.addUserCardCount(table.GRR._card_count[i]);
					call_banker_result.addUserCardsData(cards_card);
				}
				call_banker_result.setBankerPlayer(table.GRR._banker_player);
				roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
				table.GRR.add_room_response(roomResponse);
				// 可叫分动作
				for (int i = 0; i < 4; i++) {
					table._call_action[i] = call_action_current[i];
				}
				for (int i = 0; i < 2; i++) {
					table._qiang_action[i] = qiang_action_current[i];
				}
				table._game_status = GameConstants.GS_DDZ_ADD_TIMES;
				table.set_timer(DDZ_KL_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 4000);
				if (table._magic_card != GameConstants.INVALID_CARD) {
					table._logic.add_magic_card(table._logic.switch_card_to_idnex(table._magic_card));
				}
				if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
					table.send_lai_zi(1, GameConstants.INVALID_SEAT);
				}

				// // 出牌操作延迟
				return true;
			} else {
				// 玩家叫分
				if (call_banker != 0) {
					if (table._call_banker_score > call_banker) {
						return true;
					}
					table._call_banker_score = call_banker;
					table.game_cell = call_banker;
					if (table._banker_select != GameConstants.INVALID_SEAT) {
						if (table._playerStatus[table._banker_select]._call_banker < call_banker) {
							table._banker_select = seat_index;
						}
					} else {
						table._banker_select = seat_index;
					}
				}
				// 判断是否还有人没有轮到叫地主操作
				boolean bhave_no_call = false;
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._playerStatus[i]._call_banker == -1) {
						bhave_no_call = true;
					}
				}
				if (!bhave_no_call) {
					// 叫地主结束
					if (table._banker_select == GameConstants.INVALID_SEAT) {
						CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
							for (int j = 0; j < table.GRR._card_count[i]; j++) {
								cards_card.addItem(table.GRR._cards_data[i][j]);
							}
							call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							call_banker_result.addUserCardsData(cards_card);
						}
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.send_response_to_room(roomResponse);
						table.GRR.add_room_response(roomResponse);
						// 没有玩家叫地主
						int delay = 1;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), GameConstants.INVALID_SEAT,
								GameConstants.Game_End_DRAW), delay, TimeUnit.SECONDS);
						// table.exe_dispath();
						return true;
					} else {
						table.GRR._banker_player = table._banker_select;
						table._call_banker_status = 2;
						for (int i = 0; i < table._di_pai_card_count; i++) {
							table.GRR._cards_data[table.GRR._banker_player][i
									+ table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
						}
						table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
						table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player],
								table.GRR._card_count[table.GRR._banker_player]);

						for (int index = 0; index < table.getTablePlayerNumber(); index++) {
							CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
							call_banker_result.setQiangPlayer(qiang_player);
							call_banker_result.setCallPlayer(call_player);
							call_banker_result.setCallAction(call_action);
							call_banker_result.setQiangAction(qiang_action);
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							for (int i = 0; i < table._di_pai_card_count; i++) {
								call_banker_result.addCardsData(table._di_pai_card_data[i]);
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
								if (i == index) {
									for (int j = 0; j < table.GRR._card_count[i]; j++) {
										cards_card.addItem(table.GRR._cards_data[i][j]);
									}

								}
								call_banker_result.addUserCardCount(table.GRR._card_count[i]);
								call_banker_result.addUserCardsData(cards_card);
							}
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
							table.send_response_to_player(index, roomResponse);
						}

						// 回放
						CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						for (int i = 0; i < table._di_pai_card_count; i++) {
							call_banker_result.addCardsData(table._di_pai_card_data[i]);
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
							for (int j = 0; j < table.GRR._card_count[i]; j++) {
								cards_card.addItem(table.GRR._cards_data[i][j]);
							}
							call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							call_banker_result.addUserCardsData(cards_card);
						}
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.GRR.add_room_response(roomResponse);

						table._current_player = table.GRR._banker_player;
						table._game_status = GameConstants.GS_DDZ_ADD_TIMES;
						table.set_timer(DDZ_KL_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 4000);
						if (table._magic_card != GameConstants.INVALID_CARD) {
							table._logic.add_magic_card(table._logic.switch_card_to_idnex(table._magic_card));
						}
						if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
							table.send_lai_zi(1, GameConstants.INVALID_SEAT);
						}
						return true;
					}
				} else {
					// 可叫地主动作
					int can_call = table._call_banker_score;
					int index = 0;
					call_action_current[index++] = 0;
					while (can_call < 3 || index < 4) {
						call_action_current[index++] = ++can_call;
					}
					for (int i = 0; i < 4; i++) {
						table._call_action[i] = call_action_current[i];
					}
					for (int i = 0; i < 2; i++) {
						table._qiang_action[i] = qiang_action_current[i];
					}
					table._current_player = (table._current_player + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
					cur_call_player = table._current_player;
				}
			}
		} else {
			// 叫地主
			call_action = call_banker;
			qiang_action = qiang_bangker;
			call_banker_type = 3;
			if (qiang_bangker == -1) {
				call_player = seat_index;
				// 叫地主
				if (call_banker > 0) {
					table._banker_select = seat_index;
					boolean have_qiang_palyer = false;
					// 玩家如果之前没叫地主没有机会进行抢地主
					int nextplayer = table._current_player;
					for (int i = 0; i < table.getTablePlayerNumber() - 1; i++) {

						nextplayer = (nextplayer + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
						if (table._playerStatus[nextplayer]._call_banker == -1) {
							table._current_player = nextplayer;
							cur_qiang_player = table._current_player;
							table._call_banker_status = 2;
							have_qiang_palyer = true;
							break;
						}
					}

					if (!have_qiang_palyer) {
						// 叫地主结束
						table.GRR._banker_player = table._banker_select;
						for (int i = 0; i < table._di_pai_card_count; i++) {
							table.GRR._cards_data[table.GRR._banker_player][i
									+ table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
						}
						table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
						table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player],
								table.GRR._card_count[table.GRR._banker_player]);
						for (int index = 0; index < table.getTablePlayerNumber(); index++) {
							CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
							call_banker_result.setQiangPlayer(qiang_player);
							call_banker_result.setCallPlayer(call_player);
							call_banker_result.setCallAction(call_action);
							call_banker_result.setQiangAction(qiang_action);
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							// if (index == table.GRR._banker_player) {
							for (int i = 0; i < table._di_pai_card_count; i++) {
								call_banker_result.addCardsData(table._di_pai_card_data[i]);
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
								if (i == index) {
									for (int j = 0; j < table.GRR._card_count[i]; j++) {
										cards_card.addItem(table.GRR._cards_data[i][j]);
									}

								}
								call_banker_result.addUserCardCount(table.GRR._card_count[i]);
								call_banker_result.addUserCardsData(cards_card);
							}
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
							table.send_response_to_player(index, roomResponse);
						}

						// 回放
						CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						for (int i = 0; i < table._di_pai_card_count; i++) {
							call_banker_result.addCardsData(table._di_pai_card_data[i]);
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
							for (int j = 0; j < table.GRR._card_count[i]; j++) {
								cards_card.addItem(table.GRR._cards_data[i][j]);
							}
							call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							call_banker_result.addUserCardsData(cards_card);
						}
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.GRR.add_room_response(roomResponse);
						table._game_status = GameConstants.GS_DDZ_ADD_TIMES;
						table.set_timer(DDZ_KL_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 4000);
						if (table._magic_card != GameConstants.INVALID_CARD) {
							table._logic.add_magic_card(table._logic.switch_card_to_idnex(table._magic_card));
						}
						if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
							table.send_lai_zi(1, GameConstants.INVALID_SEAT);
						}
						return true;
					}
					// 可抢地主动作
					qiang_action_current[0] = 0;
					qiang_action_current[1] = 1;
					for (int i = 0; i < 4; i++) {
						table._call_action[i] = call_action_current[i];
					}
					for (int i = 0; i < 2; i++) {
						table._qiang_action[i] = qiang_action_current[i];
					}
				} else {
					// 判断是否还有人没有轮到叫地主操作
					boolean bhave_no_call = false;
					int nextplayer = table._current_player;

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						nextplayer = (nextplayer + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
						if (table._playerStatus[nextplayer]._call_banker == -1) {
							bhave_no_call = true;
							table._current_player = nextplayer;
							cur_call_player = table._current_player;
							break;
						}
					}
					if (!bhave_no_call) {
						if (table._banker_select == GameConstants.INVALID_SEAT) {
							CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
							call_banker_result.setQiangPlayer(qiang_player);
							call_banker_result.setCallPlayer(call_player);
							call_banker_result.setCallAction(call_action);
							call_banker_result.setQiangAction(qiang_action);
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
								call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							}
							call_banker_result.setBankerPlayer(GameConstants.INVALID_SEAT);
							roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
							table.send_response_to_room(roomResponse);
							table.GRR.add_room_response(roomResponse);
							// 没有玩家叫地主
							table.exe_dispath();
							return true;
						}
						for (int i = 0; i < table._di_pai_card_count; i++) {
							table.GRR._cards_data[table.GRR._banker_player][i
									+ table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
						}
						table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
						table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player],
								table.GRR._card_count[table.GRR._banker_player]);
						// 出牌操作延迟
						for (int index = 0; index < table.getTablePlayerNumber(); index++) {
							CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
							call_banker_result.setQiangPlayer(qiang_player);
							call_banker_result.setCallPlayer(call_player);
							call_banker_result.setCallAction(call_action);
							call_banker_result.setQiangAction(qiang_action);
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							// if (index == table.GRR._banker_player) {
							for (int i = 0; i < table._di_pai_card_count; i++) {
								call_banker_result.addCardsData(table._di_pai_card_data[i]);
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
								if (i == index) {
									for (int j = 0; j < table.GRR._card_count[i]; j++) {
										cards_card.addItem(table.GRR._cards_data[i][j]);
									}

								}
								call_banker_result.addUserCardCount(table.GRR._card_count[i]);
								call_banker_result.addUserCardsData(cards_card);
							}
							call_banker_result.setBankerPlayer(table.GRR._banker_player);
							roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
							table.send_response_to_player(index, roomResponse);
						}

						// 回放
						CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						for (int i = 0; i < table._di_pai_card_count; i++) {
							call_banker_result.addCardsData(table._di_pai_card_data[i]);
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
							for (int j = 0; j < table.GRR._card_count[i]; j++) {
								cards_card.addItem(table.GRR._cards_data[i][j]);
							}
							call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							call_banker_result.addUserCardsData(cards_card);
						}
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.GRR.add_room_response(roomResponse);
						table._game_status = GameConstants.GS_DDZ_ADD_TIMES;
						table.set_timer(DDZ_KL_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 4000);
						if (table._magic_card != GameConstants.INVALID_CARD) {
							table._logic.add_magic_card(table._logic.switch_card_to_idnex(table._magic_card));
						}
						if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
							table.send_lai_zi(1, GameConstants.INVALID_SEAT);
						}
						return true;
					}

					// 可叫地主动作
					call_action_current[0] = 0;
					call_action_current[1] = 1;
					for (int i = 0; i < 4; i++) {
						table._call_action[i] = call_action_current[i];
					}
					for (int i = 0; i < 2; i++) {
						table._qiang_action[i] = qiang_action_current[i];
					}
				}

			} else {
				// 抢地主
				qiang_player = seat_index;
				if (qiang_bangker > 0) {
					table._times *= 2;
					table._banker_select = seat_index;
					table.jia_bei_operate();

					// 比赛场更新数据
					PlayerServiceImpl.getInstance().updateRoomInfo(table.getRoom_id());
				}
				boolean have_qiang_palyer = false;
				int nextplayer = table._current_player;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					nextplayer = (nextplayer + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					if (table._playerStatus[nextplayer]._call_banker != 0
							&& table._playerStatus[nextplayer]._qiang_banker == -1) {

						if (nextplayer != table._banker_select) {
							have_qiang_palyer = true;
							table._current_player = nextplayer;
							cur_qiang_player = table._current_player;
						}
						break;
					}
				}
				if (!have_qiang_palyer) {
					// 叫地主结束
					table.GRR._banker_player = table._banker_select;
					table._user_times[table._banker_select] = 0;
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table._banker_select) {
							continue;
						}
						table._user_times[table._banker_select] += table._user_times[i];
					}
					for (int i = 0; i < table._di_pai_card_count; i++) {
						table.GRR._cards_data[table.GRR._banker_player][i
								+ table.GRR._card_count[table.GRR._banker_player]] = table._di_pai_card_data[i];
					}
					table.GRR._card_count[table.GRR._banker_player] += table._di_pai_card_count;
					table._logic.sort_card_date_list(table.GRR._cards_data[table.GRR._banker_player],
							table.GRR._card_count[table.GRR._banker_player]);
					for (int index = 0; index < table.getTablePlayerNumber(); index++) {
						CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
						call_banker_result.setQiangPlayer(qiang_player);
						call_banker_result.setCallPlayer(call_player);
						call_banker_result.setCallAction(call_action);
						call_banker_result.setQiangAction(qiang_action);
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						// if (index == table.GRR._banker_player) {
						for (int i = 0; i < table._di_pai_card_count; i++) {
							call_banker_result.addCardsData(table._di_pai_card_data[i]);
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
						}
						for (int i = 0; i < table.getTablePlayerNumber(); i++) {
							Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
							if (i == index) {
								for (int j = 0; j < table.GRR._card_count[i]; j++) {
									cards_card.addItem(table.GRR._cards_data[i][j]);
								}

							}
							call_banker_result.addUserCardCount(table.GRR._card_count[i]);
							call_banker_result.addUserCardsData(cards_card);
						}
						call_banker_result.setBankerPlayer(table.GRR._banker_player);
						roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
						table.send_response_to_player(index, roomResponse);
					}

					// 回放
					CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
					call_banker_result.setQiangPlayer(qiang_player);
					call_banker_result.setCallPlayer(call_player);
					call_banker_result.setCallAction(call_action);
					call_banker_result.setQiangAction(qiang_action);
					call_banker_result.setBankerPlayer(table.GRR._banker_player);
					for (int i = 0; i < table._di_pai_card_count; i++) {
						call_banker_result.addCardsData(table._di_pai_card_data[i]);
					}
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
					}
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
						for (int j = 0; j < table.GRR._card_count[i]; j++) {
							cards_card.addItem(table.GRR._cards_data[i][j]);
						}
						call_banker_result.addUserCardCount(table.GRR._card_count[i]);
						call_banker_result.addUserCardsData(cards_card);
					}
					call_banker_result.setBankerPlayer(table.GRR._banker_player);
					roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
					table.GRR.add_room_response(roomResponse);
					table._game_status = GameConstants.GS_DDZ_ADD_TIMES;
					table.set_timer(DDZ_KL_Table.ID_TIMER_CALL_BANKER_TO_ADD_TIME, 4000);
					if (table._magic_card != GameConstants.INVALID_CARD) {
						table._logic.add_magic_card(table._logic.switch_card_to_idnex(table._magic_card));
					}
					if (table.has_rule(Constants_KL_DDZ.GAME_RULE_SUI_JI_LAI_ZI)) {
						table.send_lai_zi(1, GameConstants.INVALID_SEAT);
					}
					return true;
				}
				// 可抢地主动作
				qiang_action_current[0] = 0;
				qiang_action_current[1] = 1;
				for (int i = 0; i < 4; i++) {
					table._call_action[i] = call_action_current[i];
				}
				for (int i = 0; i < 2; i++) {
					table._qiang_action[i] = qiang_action_current[i];
				}
			}
		}
		table._call_banker_type = call_banker_type;
		if (cur_call_player != GameConstants.INVALID_SEAT) {
			table._call_banker[cur_call_player] = -1;
		}
		if (cur_qiang_player != GameConstants.INVALID_SEAT) {
			table._qiang_banker[cur_qiang_player] = -1;
		}

		CallBankerResult.Builder call_banker_result = CallBankerResult.newBuilder();
		call_banker_result.setQiangPlayer(qiang_player);
		call_banker_result.setCallPlayer(call_player);
		call_banker_result.setCallAction(call_action);
		call_banker_result.setQiangAction(qiang_action);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			Int32ArrayResponse.Builder cards_card = Int32ArrayResponse.newBuilder();
			call_banker_result.addDifenBombDes(table.get_boom_difen_des(i));
			for (int j = 0; j < table.GRR._card_count[i]; j++) {
				cards_card.addItem(table.GRR._cards_data[i][j]);
			}
			call_banker_result.addUserCardCount(table.GRR._card_count[i]);
			call_banker_result.addUserCardsData(cards_card);
		}
		call_banker_result.setBankerPlayer(GameConstants.INVALID_SEAT);

		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_result));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);

		roomResponse.setType(MsgConstants.RESPONSE_DDZ_CALL_BANKER);
		CallBankerDDZ.Builder call_banker_ddz = CallBankerDDZ.newBuilder();
		call_banker_ddz.setCallCurrentPlayer(cur_call_player);
		call_banker_ddz.setQiangCurrentPlayer(cur_qiang_player);
		call_banker_ddz.setCallType(call_banker_type);
		call_banker_ddz.setDisplayTime(10);
		call_banker_ddz.setFeiPaiCard(-1);
		for (int i = 0; i < 4; i++) {
			call_banker_ddz.addCallCurrentAction(call_action_current[i]);
		}
		for (int i = 0; i < 2; i++) {
			call_banker_ddz.addQiangCurrentAction(qiang_action_current[i]);
		}

		roomResponse.clearCommResponse();
		roomResponse.setCommResponse(PBUtil.toByteString(call_banker_ddz));
		table.send_response_to_room(roomResponse);
		table.GRR.add_room_response(roomResponse);
		return true;
	}

}
