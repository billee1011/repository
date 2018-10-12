package com.cai.game.shengji.handler.yzbp;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.shengji.SJMsgConstants;
import com.cai.game.shengji.handler.SJHandlerCallBanker;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.yzbp.yzbpRsp.TableResponse_Yzbp;

public class SJHandlerCallBanker_YZBP extends SJHandlerCallBanker<SJTable_YZBP> {
	protected int _seat_index;
	protected int _game_status;
	protected int _call_ation;
	// private int _current_player =MJGameConstants.INVALID_SEAT;

	public SJHandlerCallBanker_YZBP() {
	}

	public void reset_status(int seat_index, int game_status) {
		_seat_index = seat_index;
		_game_status = game_status;
	}

	@Override
	public void exe(SJTable_YZBP table) {
		_seat_index = GameConstants.INVALID_SEAT;
		_call_ation = 0;
		table.send_call_banker_respnse(_seat_index, _call_ation, GameConstants.INVALID_SEAT);
	}

	@Override
	public boolean handler_player_be_in_room(SJTable_YZBP table, int seat_index) {

		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(SJMsgConstants.RESPONSE_YZBP_RECONNECT_DATA);
		// 发送数据
		TableResponse_Yzbp.Builder tableResponse = TableResponse_Yzbp.newBuilder();
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
			table.send_call_banker_respnse(_seat_index, _call_ation, GameConstants.INVALID_SEAT);

		}
		if (table._game_status == GameConstants.GS_XFGD_MAI_DI) {
			table.send_mai_di_begin(seat_index);
			if (table._is_banker_tou_xiang != -1) {
				table.send_tou_xiang_result(seat_index);
			}
		}

		if (seat_index == table._current_player && table._game_status == GameConstants.GS_XFGD_DING_ZHU) {
			table.send_jiao_zhu_begin(seat_index);
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
	public boolean handler_call_banker(SJTable_YZBP table, int seat_index, int call_action) {
		if (table._game_status != GameConstants.GS_XFGD_CALL_BANKER) {
			return true;
		}
		if (seat_index != table._current_player) {
			return true;
		}
		if (call_action % 5 != 0) {
			return true;
		}
		if (call_action < table._min_dang && call_action != 0) {
			return true;
		}
		boolean is_reset = false;
		_seat_index = seat_index;
		_call_ation = call_action;
		if (call_action == table._max_dang) {
			table._select_dang[seat_index] = call_action;
			table.GRR._banker_player = seat_index;
			table._current_player = seat_index;
			table._min_dang = call_action + 5;
		} else {
			if (call_action != 0) {
				table._min_dang = call_action + 5;
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

		table.send_call_banker_respnse(_seat_index, _call_ation, GameConstants.INVALID_SEAT);

		if (table.GRR._banker_player != GameConstants.INVALID_SEAT) {
			table._game_status = GameConstants.GS_XFGD_DING_ZHU;
			table.send_jiao_zhu_begin(GameConstants.INVALID_SEAT);
			table._cur_banker = table.GRR._banker_player;
			table.send_history(GameConstants.INVALID_SEAT);
			table.RefreshScore(GameConstants.INVALID_SEAT);
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
