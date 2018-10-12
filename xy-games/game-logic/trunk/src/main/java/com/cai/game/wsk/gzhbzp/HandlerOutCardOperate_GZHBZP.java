package com.cai.game.wsk.gzhbzp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.wsk.data.tagAnalyseIndexResult_WSK;
import com.cai.game.wsk.handler.pcdz.WSKTable_PCDZ;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.gzhbzp.gzhbzpRsp.TableResponse_gzhbzp;

public class HandlerOutCardOperate_GZHBZP extends AbstractHandler_GZHBZP {
	public int _out_card_player = GameConstants.INVALID_SEAT; // 出牌用户
	public int[] _out_cards_data = new int[GameConstants.WSK_MAX_COUNT]; // 出牌扑克
	public int[] _out_change_cards_data = new int[GameConstants.WSK_MAX_COUNT]; // 变换扑克
	public int _out_card_count = 0;
	public int _out_type;

	public void reset_status(int seat_index, int cards[], int card_count, int is_out) {
		_out_card_player = seat_index;
		_out_cards_data = new int[card_count];

		for (int i = 0; i < card_count; i++) {
			_out_cards_data[i] = cards[i];
		}

		_out_card_count = card_count;
		_out_type = is_out;
	}

	@Override
	public void exe(Table_GZHBZP table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		if (_out_type == 0) {
			user_pass_card(table);
			return;
		}

		table._logic.sort_out_card_list(_out_cards_data, _out_card_count);

		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.BZP_GZH_ERROR) {
			// 如果出的牌经过转换之后，不是单张、对子、三张、连对、飞机里、五十K、炸弹的任何一种
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		// 本轮牌桌上的累加分
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;

		table._out_card_player = _out_card_player;
		table._prev_player = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);

		if (!(table._is_yi_da_san||table._is_yi_da_yi)) {
			// 如果不是独牌玩法，判断当前出牌人是不是庄家的队友
			if (_out_card_player == table._friend_seat[table.GRR._banker_player]) {
				for (int i = 0; i < _out_card_count; i++) {
					if (_out_cards_data[i] == table._jiao_pai_card) {
						table._out_card_ming_ji = table._jiao_pai_card;

						table.refresh_ming_pai(GameConstants.INVALID_SEAT);
					}
				}
			}
		}
		for (int i = 0; i < _out_card_count; i++) {
		 
			table._turn_real_card_data[i] = _out_cards_data[i];
			table._cur_out_card_data[_out_card_player][i] = table._turn_real_card_data[i];
			table._turn_out_card_data[i] = _out_cards_data[i];
			
		}

		// 获取下一次需要进行操作的玩家
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber()&&table._is_yi_da_yi == true; i++) {
			if (!(table._call_banker_opreate[next_player]==1||table._call_banker_opreate[next_player]==2)&& next_player != table._out_card_player) {
			
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;

				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		for (int i = 0; i < table.getTablePlayerNumber()&&table._is_yi_da_yi == false; i++) {
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}

				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;

				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}

		table._current_player = next_player;
		table._cur_out_card_count[next_player] = 0;
		Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

		boolean is_end = false;
		if (table.GRR._card_count[_out_card_player] == 0) {
			int delay = 2000;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
					table._chuwan_shunxu[i] = _out_card_player;
					break;
				}
			}
			if (table._is_yi_da_san||table._is_yi_da_yi) {
				if(table._is_yi_da_san){
					for (int j = 0; j < table.getTablePlayerNumber(); j++) {
						if (table.GRR._card_count[j] != 0) {
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
									table._chuwan_shunxu[i] = j;
									break;
								}
							}
						}

					}

				}else {
					for (int j = 0; j < table.getTablePlayerNumber(); j++) {
						if (table.GRR._card_count[j] != 0) {
							int index = j;
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								if (table._chuwan_shunxu[i] == index) {
									index = -1;
									break;
								}
							}
							for (int i = 0; i < table.getTablePlayerNumber(); i++) {
								if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT&&index != -1) {
									table._chuwan_shunxu[i] = index;
									break;
								}
							}
						}

					}

				}
				table.caculate_score();
				table._current_player = -1;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), delay,
						TimeUnit.MILLISECONDS);
				is_end = true;


			} else {
				// 一二游
					
					if((table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT && table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT)&&table._friend_seat[table._chuwan_shunxu[0]] == table._chuwan_shunxu[1]){
						table._zhua_pai[table._chuwan_shunxu[1]] += table._zhua_pai_count;
						is_end = true;
					}
					else if(table._chuwan_shunxu[2] != -1){
						table._zhua_pai[table._chuwan_shunxu[2]] += table._zhua_pai_count;
						is_end = true;
					}
					if(is_end == true){
						for (int j = 0; j < table.getTablePlayerNumber(); j++) {
							if (table.GRR._card_count[j] != 0) {
								for (int i = 0; i < table.getTablePlayerNumber(); i++) {
									if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
										table._chuwan_shunxu[i] = j;
										break;
									}
								}
							}

						}
						table.caculate_score();
						table._current_player = -1;
						GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), delay,
								TimeUnit.MILLISECONDS);
						
					}
					
					
				
			}
		}



		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_real_card_data, table._turn_out_card_type,
				GameConstants.INVALID_SEAT, false);
//		for(int i = 0; i< table.getTablePlayerNumber()&&is_end == true;i++){
//			if(table.GRR._card_count[i]>0){
//				table._current_player = -1;
//				int next_type = table._logic.get_card_type(table.GRR._cards_data[i],table.GRR._card_count[i]);
//				if(next_type == -1)
//					next_type = 0;
//				table.operate_out_card(i, table.GRR._card_count[i], table.GRR._cards_data[i], next_type,
//						GameConstants.INVALID_SEAT, false);
//			}
//		}
		table.refresh_user_get_score(GameConstants.INVALID_SEAT);
	}

	/**
	 * 每次有玩家出牌之后，处理牌桌上剩余的五十K的牌，并计算牌桌上还剩下的分
	 * 
	 * @param table
	 */
	public void table_pai_socre(Table_GZHBZP table) {
		int pai_score = 0;
		int remove_card[] = new int[table.get_hand_card_count_max()];
		int remove_count = 0;

		for (int i = 0; i < _out_card_count; i++) {
			int value = table._logic.get_card_logic_value(_out_cards_data[i]);

			if (value == Constants_GZHBZP.CARD_FIVE || value == Constants_GZHBZP.CARD_TEN || value == Constants_GZHBZP.CARD_THIRTEEN) {
				remove_card[remove_count++] = _out_cards_data[i];
				table._out_pai_score_card[table._out_pai_score_count++] = _out_cards_data[i];
			}

			if (value == Constants_GZHBZP.CARD_FIVE) {
				pai_score += 5;
			} else if (value == Constants_GZHBZP.CARD_TEN || value == Constants_GZHBZP.CARD_THIRTEEN) {
				pai_score += 10;
			}
		}

		if (!table._logic.remove_card(remove_card, remove_count, table._pai_score_card, table._pai_score_count)) {
		}

		table._pai_score_count -= remove_count;
		table._pai_score -= pai_score;

		table._out_pai_score += pai_score;
	}

	/**
	 * 牌桌上，当前操作人点了过
	 * 
	 * @param table
	 */
	public void user_pass_card(Table_GZHBZP table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		// 清空接下去出牌玩家出牌数据
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber()&&table._is_yi_da_yi == true; i++) {
			if (!(table._call_banker_opreate[next_player]==1||table._call_banker_opreate[next_player]==2)&& next_player != table._out_card_player) {
			
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}
		for (int i = 0; i < table.getTablePlayerNumber()&&table._is_yi_da_yi == false; i++) {
			if (table.GRR._card_count[next_player] == 0 && next_player != table._out_card_player) {
				if (table._cur_out_card_count[next_player] > 0) {
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				// 如果下一个玩家的牌已经出完了，并且下一个玩家不是当前操作人
				next_player = (next_player + 1) % table.getTablePlayerNumber();
			} else {
				break;
			}
		}

		// 一轮不出
		if (next_player == table._out_card_player) {
			// 清空桌面牌分
			table._zhua_pai[table._out_card_player] += table._zhua_pai_count;
			table._zhua_pai_count = 0;
			int award_dou = table.get_award(table._turn_out_card_type, table._turn_out_card_data[0]);
			int award_plane =  table.get_gun_tong(table._turn_out_card_type);
			table._award_dou[table._out_card_player] += award_dou*(table.getTablePlayerNumber()-1);
			table._award_plane[table._out_card_player] += award_plane*(table.getTablePlayerNumber()-1);
			for(int i = 0; i< table.getTablePlayerNumber();i++){
				if(i == table._out_card_player)
					continue;
				table._award_dou[i] -= award_dou;
				table._award_plane[i] -= award_plane;
			}
			table._turn_out_card_count = 0;

			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			if (table.GRR._card_count[next_player] == 0) {
				if (table._cur_out_card_count[next_player] > 0) {
					table._current_player = GameConstants.INVALID_SEAT;
					table.operate_out_card(next_player, 0, null, -2, GameConstants.INVALID_SEAT, false);
				}
				table._current_player = next_player;
				table._cur_out_card_count[next_player] = 0;
				Arrays.fill(table._cur_out_card_data[next_player], GameConstants.INVALID_CARD);

				next_player = (next_player + 1) % table.getTablePlayerNumber();
				boolean jie_feng = false;
//				if(table._out_card_ming_ji == GameConstants.INVALID_CARD)
//					jie_feng = false;
				int friend_seat = table._friend_seat[table._out_card_player];
				if (jie_feng) {
					next_player = friend_seat;
				} else {

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[next_player] == 0) {
							next_player = (next_player + 1) % table.getTablePlayerNumber();
						} else {
							break;
						}
					}
				}
				table._current_player = next_player;

			} else {
				table._current_player = next_player;
				table._prev_player = _out_card_player;
				table._cur_out_card_count[table._current_player] = 0;
				Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._prev_player = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.WSK_GF_CT_PASS, GameConstants.INVALID_SEAT,
				false);
		table.refresh_user_get_score(GameConstants.INVALID_SEAT);

	}

	/**
	 * 对当前出牌人的牌数据，进行合理的转换，并和上一次出牌人的牌型进行大小比对。如果当前出牌人的牌型比上一次出牌人的牌型小，返回错误的牌型值。
	 * 
	 * @param table
	 * @return
	 */
	public int adjust_out_card_right(Table_GZHBZP table) {
		Arrays.fill(_out_change_cards_data, 0);

		int[] _tmp_out_cards_data = Arrays.copyOf(_out_cards_data, _out_cards_data.length);

		for (int i = 0; i < _out_card_count; i++) {
			if (_tmp_out_cards_data[i] > Constants_GZHBZP.SPECIAL_CARD_TYPE) {
				_tmp_out_cards_data[i] = _tmp_out_cards_data[i] & 0xFF;
			}
		}

		int card_type = GameConstants.BZP_GZH_ERROR;
		card_type = table._logic.get_card_type(_out_cards_data, _out_card_count);

		if (card_type == GameConstants.BZP_GZH_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.compare_card(table._turn_out_card_data, _out_cards_data, table._turn_out_card_count, _out_card_count)) {
				return GameConstants.BZP_GZH_ERROR;
			}
		}

		if (table._logic == null) {
			table.log_error("table._logic == null");
		}
		if (table.GRR == null) {
			table.log_error("table.GRR == null");
		}
		if (table.GRR._cards_data == null) {
			table.log_error("table.GRR._cards_data == null");
		}
		if (table.GRR._card_count == null) {
			table.log_error("table.GRR._card_count == null");
		}
		if (!table._logic.remove_card(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.BZP_GZH_ERROR;
		}
		// 减掉玩家的手牌数目
		table.GRR._card_count[_out_card_player] -= _out_card_count;
		table._zhua_pai_count += _out_card_count;

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(Table_GZHBZP table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_BZP_GZH_RECONNECT_DATA);

		TableResponse_gzhbzp.Builder tableResponse = TableResponse_gzhbzp.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._logic.switch_s_to_c(table._turn_out_card_type));
		tableResponse.setIsYiDaSan(table._is_yi_da_san);

		if (table._turn_out_card_count == 0 && seat_index == table._current_player) {
			tableResponse.setIsFirstOut(1);
		} else {
			tableResponse.setIsFirstOut(0);
		}

		for (int i = 0; i < table._turn_out_card_count; i++) {
			tableResponse.addPrCardsData(table._turn_real_card_data[i]);
			tableResponse.addPrCardsChangeData(table._turn_out_card_data[i]);
		}

		if (table._out_card_ming_ji != GameConstants.INVALID_CARD && table.GRR._card_count[seat_index] == 0) {
			tableResponse.setFriendSeatIndex(table._friend_seat[seat_index]);
		} else {
			tableResponse.setFriendSeatIndex(GameConstants.INVALID_SEAT);
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int cardCount = table.GRR._card_count[i];
	
			tableResponse.addCardCount(cardCount);

			Int32ArrayResponse.Builder cards = Int32ArrayResponse.newBuilder();
			Int32ArrayResponse.Builder cur_out_cards = Int32ArrayResponse.newBuilder();
			@SuppressWarnings("unused")
			Int32ArrayResponse.Builder wang_cards = Int32ArrayResponse.newBuilder();

			if (table._out_card_ming_ji != GameConstants.INVALID_CARD && table.GRR._card_count[i] == 0) {
				if (seat_index == i) {
					int tmpI = table._friend_seat[i];
					for (int j = 0; j < table.GRR._card_count[tmpI]; j++) {
						cards.addItem(table.GRR._cards_data[tmpI][j]);
					}
				}
			} else {
				if (seat_index == i) {
					for (int j = 0; j < table.GRR._card_count[i]; j++) {
						cards.addItem(table.GRR._cards_data[i][j]);
					}
				}
			}

			for (int j = 0; j < table._cur_out_card_count[i]; j++) {
				cur_out_cards.addItem(table._cur_out_card_data[i][j]);
			}

			tableResponse.addOutCardsData(cur_out_cards);
			tableResponse.addCardsData(cards);
			if (table._is_yi_da_yi == true || table._is_yi_da_yi == true)
				tableResponse.addWinOrder(-1);
			else	
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

		if (table._current_player == seat_index && table._current_player != GameConstants.INVALID_SEAT) {
			int can_out_card_data[] = new int[table.get_hand_card_count_max()];
			int can_out_card_count = table._logic.search_can_out_cards(table.GRR._cards_data[table._current_player],
					table.GRR._card_count[table._current_player], table._turn_out_card_data, table._turn_out_card_count, can_out_card_data);

			for (int i = 0; i < can_out_card_count; i++) {
				tableResponse.addUserCanOutData(can_out_card_data[i]);
			}
			tableResponse.setUserCanOutCount(can_out_card_count);
		}

		tableResponse.setJiaoCardData(table._jiao_pai_card);
		roomResponse.setCommResponse(PBUtil.toByteString(tableResponse));

		table.send_response_to_player(seat_index, roomResponse);

		table.refresh_pai_score(seat_index);

		table.refresh_user_get_score(seat_index);

		table.refresh_ming_pai(seat_index);

		return true;
	}

}
