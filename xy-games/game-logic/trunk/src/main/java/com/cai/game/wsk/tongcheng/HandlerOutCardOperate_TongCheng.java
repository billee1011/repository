package com.cai.game.wsk.tongcheng;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.util.PBUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.tcdg.TcdgRsp.TableResponse_tcdg;

public class HandlerOutCardOperate_TongCheng extends AbstractHandler_TongCheng {
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
	public void exe(Table_TongCheng table) {
		if (_out_card_player != table._current_player) {
			return;
		}

		if (_out_type == 0) {
			user_pass_card(table);
			return;
		}

		table._logic.sort_out_card_list(_out_cards_data, _out_card_count);

		int card_type = adjust_out_card_right(table);
		if (card_type == GameConstants.TCDG_CT_ERROR) {
			// 如果出的牌经过转换之后，不是单张、对子、三张、连对、飞机里、五十K、炸弹的任何一种
			table.send_error_notify(_out_card_player, 2, "请选择正确的牌型!");
			return;
		}

		// 明牌之前，用炸弹炸队友，再下一次出牌之前，处理罚分
		if (!table._is_yi_da_san && table._out_card_ming_ji == GameConstants.INVALID_CARD && (card_type == GameConstants.TCDG_CT_BOMB
				|| card_type == GameConstants.TCDG_CT_510K_DC || card_type == GameConstants.TCDG_CT_510K_SC)) {
			boolean punish = true;
			
			for (int i = 0; i < _out_card_count; i++) {
				if (_out_cards_data[i] == table._jiao_pai_card) {
					punish = false;
				}
			}
			
			if (punish == true && table._out_card_player != GameConstants.INVALID_SEAT) {
				if (_out_card_player == table._friend_seat[table._out_card_player] && table._is_first_false_bomb[table._out_card_player] == false) {
					table._is_first_false_bomb[_out_card_player] = true;
					table._cur_round_false_bomb[_out_card_player]++;
				}
			}
		}

		table_pai_socre(table);

		int magic_count = table._logic.get_wang_count_before_card_change(_out_cards_data, _out_card_count);
		if (magic_count > 0) {
			table._out_magic_count[_out_card_player] += table._logic.get_magic_count(_out_cards_data, _out_card_count);
			table._out_flower_count[_out_card_player] += table._logic.get_flower_count(_out_cards_data, _out_card_count);
		}

		// 如果打出的牌是炸弹，并且是硬7或硬8的炸弹，计算喜钱分，并累加总的七喜或八喜的次数
		if (card_type == GameConstants.TCDG_CT_BOMB) {
			int basicScore = 0;
			boolean hasSix = table.hasRuleLiuZhaYiFen;
			if (hasSix) {
				if (_out_card_count - magic_count >= 8) {
					basicScore = 4;
					table._tmp_eight_boom_times[_out_card_player]++;
				} else if (_out_card_count - magic_count >= 7) {
					basicScore = 2;
					table._tmp_seven_boom_times[_out_card_player]++;
				} else if (_out_card_count - magic_count >= 6) {
					basicScore = 1;
					table._tmp_six_boom_times[_out_card_player]++;
				}
			} else {
				if (_out_card_count - magic_count >= 8) {
					basicScore = 2;
					table._tmp_eight_boom_times[_out_card_player]++;
				} else if (_out_card_count - magic_count >= 7) {
					basicScore = 1;
					table._tmp_seven_boom_times[_out_card_player]++;
				}
			}
			if (basicScore > 0) {
				int pCount = table.getTablePlayerNumber();
				for (int i = 0; i < pCount; i++) {
					if (i == _out_card_player) {
						table._xi_qian_score[i] += basicScore * (pCount - 1);
					} else {
						table._xi_qian_score[i] -= basicScore;
					}
				}
			}
		}

		// 如果打出的牌是五十K，并且没王牌，累加计数器
		if (magic_count == 0 && (card_type == GameConstants.TCDG_CT_510K_DC || card_type == GameConstants.TCDG_CT_510K_SC)) {
			table._ying_wsk_count[_out_card_player]++;
		}

		// 减掉玩家的手牌数目
		table.GRR._card_count[_out_card_player] -= _out_card_count;

		if (!table._is_yi_da_san) {
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

		// 本轮牌桌上的累加分
		table._turn_have_score += table._logic.get_card_score(_out_cards_data, _out_card_count);
		table._turn_out_card_type = card_type;
		table._turn_out_card_count = _out_card_count;

		table._out_card_player = _out_card_player;
		table._prev_player = _out_card_player;
		table._cur_out_card_count[_out_card_player] = _out_card_count;
		Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);

		// 处理本轮的出牌数据
		int wang_count = table._logic.get_wang_count_before_card_change(_out_cards_data, _out_card_count);
		for (int i = 0; i < _out_card_count; i++) {
			if (_out_change_cards_data[i] > Constants_TongCheng.SPECIAL_CARD_TYPE) {
				table._turn_real_card_data[i] = (_out_cards_data[_out_card_count - wang_count--] & 0xFF);
				table._cur_out_card_data[_out_card_player][i] = table._turn_real_card_data[i];
				table._turn_out_card_data[i] = _out_change_cards_data[i] & 0xFF;
			} else {
				table._turn_real_card_data[i] = _out_change_cards_data[i];
				table._cur_out_card_data[_out_card_player][i] = table._turn_real_card_data[i];
				table._turn_out_card_data[i] = _out_change_cards_data[i];
			}
		}

		// 获取下一次需要进行操作的玩家
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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

		// 上下游的相对概念。第一个游走的玩家和队友算上游。第二个游走的人和队友算下游。
		int shang_you_score = 0;
		// 第二个游走的人的分单独处理，因为需要判断，二游是庄还是闲
		int er_you_score = 0;
		int yi_you_score = 0;

		boolean finish_in_advance = false;

		if (table.GRR._card_count[_out_card_player] == 0) {
			// 如果当前出牌人的牌出完了

			if (table._is_yi_da_san) {
				// 如果是独牌，直接结束游戏
				table._chuwan_shunxu[0] = _out_card_player;
				finish_in_advance = true;
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._chuwan_shunxu[i] == GameConstants.INVALID_SEAT) {
						table._chuwan_shunxu[i] = _out_card_player;
						break;
					}
				}

				int you_num = 0;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table._chuwan_shunxu[i] != GameConstants.INVALID_SEAT) {
						you_num++;
					}
				}

				if (table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT) {
					yi_you_score = table._get_score[table._chuwan_shunxu[0]];

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (i == table._chuwan_shunxu[0] || i == table._friend_seat[table._chuwan_shunxu[0]]) {
							shang_you_score += table._get_score[i];
						}
					}
				}

				if (table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT) {
					er_you_score += table._get_score[table._chuwan_shunxu[1]];
				}

				if (table._chuwan_shunxu[2] != GameConstants.INVALID_SEAT) {
					// 如果已经游走了3个玩家，结束游戏
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[i] != 0) {
							table._chuwan_shunxu[3] = i;
							break;
						}
					}

					finish_in_advance = true;
				} else {
					if (you_num == 1) {
						if (yi_you_score == 200) {
							// 打出一游 一游拿分200分
							finish_in_advance = true;
						}
					} else if (you_num == 2) {
						int score_fence = 100;
						if (table._chuwan_shunxu[1] == table.GRR._banker_player
								|| table._friend_seat[table._chuwan_shunxu[1]] == table.GRR._banker_player)
							score_fence = 105;

						if (table._friend_seat[table._chuwan_shunxu[0]] == table._chuwan_shunxu[1]) {
							// 打出一二游 一二游同队
							finish_in_advance = true;
						} else {
							// 打出一二有 一二不同队
							if ((er_you_score == 200) || (er_you_score >= score_fence && shang_you_score > 0) || shang_you_score == 200) {
								// (二游=200) 或 (N<=二游<200 & 一游队伍有拿分) 或 (一游队伍=200)
								finish_in_advance = true;
							}

							int score_fence_two = 105;
							if (table._chuwan_shunxu[1] == table.GRR._banker_player
									|| table._friend_seat[table._chuwan_shunxu[1]] == table.GRR._banker_player)
								score_fence_two = 100;

							if (er_you_score > 0 && er_you_score < score_fence && shang_you_score >= score_fence_two) {
								// 0<二游<N_1 & 一游队伍>=N_2
								finish_in_advance = true;
							}
						}
					}
				}
			}
		}

		if (finish_in_advance) {
			// 提前结束的时候 处理本轮的罚分
			boolean has_dealed[] = new boolean[table.getTablePlayerNumber()];
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int friend_seat = table._friend_seat[i];
				if (table._cur_round_false_bomb[i] > 0 && !has_dealed[i]) {
					if (table._cur_round_false_bomb[i] > table._cur_round_false_bomb[friend_seat]) {
						table._punish_score[i] -= table._cur_round_false_bomb[i] - table._cur_round_false_bomb[friend_seat];
						table._punish_score[friend_seat] += table._cur_round_false_bomb[i] - table._cur_round_false_bomb[friend_seat];

						has_dealed[i] = true;
						has_dealed[friend_seat] = true;
					} else if (table._cur_round_false_bomb[i] < table._cur_round_false_bomb[friend_seat]) {
						table._punish_score[i] += table._cur_round_false_bomb[friend_seat] - table._cur_round_false_bomb[i];
						table._punish_score[friend_seat] -= table._cur_round_false_bomb[friend_seat] - table._cur_round_false_bomb[i];

						has_dealed[i] = true;
						has_dealed[friend_seat] = true;
					}
				}
			}

			table._is_first_false_bomb = new boolean[table.getTablePlayerNumber()];

			Arrays.fill(table._cur_round_false_bomb, 0);

			// 提前结束时 把本轮桌面上的分 给到出完手牌的那个人
			table._get_score[_out_card_player] += table._turn_have_score;

			table._current_player = GameConstants.INVALID_SEAT;
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _out_card_player, GameConstants.Game_End_NORMAL), 3, TimeUnit.SECONDS);
		}

		// 显示出牌
		table.operate_out_card(table._out_card_player, table._turn_out_card_count, table._turn_real_card_data, table._turn_out_card_type,
				GameConstants.INVALID_SEAT, false);

		table.refresh_pai_score(GameConstants.INVALID_SEAT);

		table.refresh_user_get_score(GameConstants.INVALID_SEAT);
	}

	/**
	 * 每次有玩家出牌之后，处理牌桌上剩余的五十K的牌，并计算牌桌上还剩下的分
	 * 
	 * @param table
	 */
	public void table_pai_socre(Table_TongCheng table) {
		int pai_score = 0;
		int remove_card[] = new int[table.get_hand_card_count_max()];
		int remove_count = 0;

		for (int i = 0; i < _out_card_count; i++) {
			int value = table._logic.get_card_logic_value(_out_cards_data[i]);

			if (value == Constants_TongCheng.CARD_FIVE || value == Constants_TongCheng.CARD_TEN || value == Constants_TongCheng.CARD_THIRTEEN) {
				remove_card[remove_count++] = _out_cards_data[i];
				table._out_pai_score_card[table._out_pai_score_count++] = _out_cards_data[i];
			}

			if (value == Constants_TongCheng.CARD_FIVE) {
				pai_score += 5;
			} else if (value == Constants_TongCheng.CARD_TEN || value == Constants_TongCheng.CARD_THIRTEEN) {
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
	public void user_pass_card(Table_TongCheng table) {
		if (table._turn_out_card_count == 0) {
			return;
		}
		
		if (table.GRR == null)
			return;

		// 下一个玩家
		int next_player = (_out_card_player + 1) % table.getTablePlayerNumber();

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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

		if (next_player == table._out_card_player) {
			// 处理罚分
			boolean has_dealed[] = new boolean[table.getTablePlayerNumber()];
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int friend_seat = table._friend_seat[i];
				if (table._cur_round_false_bomb[i] > 0 && !has_dealed[i]) {
					if (table._cur_round_false_bomb[i] > table._cur_round_false_bomb[friend_seat]) {
						table._punish_score[i] -= table._cur_round_false_bomb[i] - table._cur_round_false_bomb[friend_seat];
						table._punish_score[friend_seat] += table._cur_round_false_bomb[i] - table._cur_round_false_bomb[friend_seat];

						has_dealed[i] = true;
						has_dealed[friend_seat] = true;
					} else if (table._cur_round_false_bomb[i] < table._cur_round_false_bomb[friend_seat]) {
						table._punish_score[i] += table._cur_round_false_bomb[friend_seat] - table._cur_round_false_bomb[i];
						table._punish_score[friend_seat] -= table._cur_round_false_bomb[friend_seat] - table._cur_round_false_bomb[i];

						has_dealed[i] = true;
						has_dealed[friend_seat] = true;
					}
				}
			}

			table._is_first_false_bomb = new boolean[table.getTablePlayerNumber()];

			Arrays.fill(table._cur_round_false_bomb, 0);

			// 如果其他人都不要牌了，处理一下牌桌上的分
			int shang_you_score = 0;
			int er_you_score = 0;
			int yi_you_score = 0;

			// 本轮上次出牌的人抓取本轮桌子上的分
			table._get_score[table._out_card_player] += table._turn_have_score;

			table._turn_have_score = 0;
			table._turn_out_card_count = 0;

			if (table._chuwan_shunxu[0] != GameConstants.INVALID_SEAT) {
				yi_you_score = table._get_score[table._chuwan_shunxu[0]];

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (i == table._chuwan_shunxu[0] || i == table._friend_seat[table._chuwan_shunxu[0]]) {
						shang_you_score += table._get_score[i];
					}
				}
			}

			int you_num = 0;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._chuwan_shunxu[i] != GameConstants.INVALID_SEAT) {
					you_num++;
				}
			}

			if (table._chuwan_shunxu[1] != GameConstants.INVALID_SEAT) {
				er_you_score += table._get_score[table._chuwan_shunxu[1]];
			}

			boolean finish_in_advance = false;

			if (you_num == 1) {
				if (yi_you_score == 200) {
					// 打出一游 一游拿分200分
					finish_in_advance = true;
				}
			} else if (you_num == 2) {
				int score_fence = 100;
				if (table._chuwan_shunxu[1] == table.GRR._banker_player || table._friend_seat[table._chuwan_shunxu[1]] == table.GRR._banker_player)
					score_fence = 105;

				if (table._friend_seat[table._chuwan_shunxu[0]] == table._chuwan_shunxu[1]) {
					// 打出一二游 一二游同队
					finish_in_advance = true;
				} else {
					// 打出一二有 一二不同队
					if ((er_you_score == 200) || (er_you_score >= score_fence && shang_you_score > 0) || shang_you_score == 200) {
						// (二游=200) 或 (N<=二游<200 & 一游队伍有拿分) 或 (一游队伍=200)
						finish_in_advance = true;
					}

					int score_fence_two = 105;
					if (table._chuwan_shunxu[1] == table.GRR._banker_player
							|| table._friend_seat[table._chuwan_shunxu[1]] == table.GRR._banker_player)
						score_fence_two = 100;

					if (er_you_score > 0 && er_you_score < score_fence && shang_you_score >= score_fence_two) {
						// 0<二游<N_1 & 一游队伍>=N_2
						finish_in_advance = true;
					}
				}
			}

			if (finish_in_advance) {
				table._current_player = GameConstants.INVALID_SEAT;
				GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), table._chuwan_shunxu[0], GameConstants.Game_End_NORMAL), 3,
						TimeUnit.SECONDS);
			}

			Arrays.fill(table._turn_out_card_data, GameConstants.INVALID_CARD);
			Arrays.fill(table._turn_real_card_data, GameConstants.INVALID_CARD);

			if (table.GRR._card_count[next_player] == 0) {
				// 如果上次出牌的人，手里已经没牌了，接风
				if (table._out_card_ming_ji != GameConstants.INVALID_CARD) {
					// 如果本局已经明鸡确定队友关系
					next_player = table._friend_seat[next_player];
				} else {
					// 如果本局还没有明鸡
					next_player = (next_player + 1) % table.getTablePlayerNumber();

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						if (table.GRR._card_count[next_player] == 0) {
							next_player = (next_player + 1) % table.getTablePlayerNumber();
						} else {
							break;
						}
					}
				}

				table._current_player = next_player;

				// 播放接风音效
				table.operate_catch_action(next_player);
			} else {
				table._current_player = next_player;
				table._prev_player = _out_card_player;
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table._cur_out_card_count[i] = 0;
				Arrays.fill(table._cur_out_card_data[i], GameConstants.INVALID_CARD);
			}
		} else {
			table._current_player = next_player;
			table._prev_player = _out_card_player;
			table._cur_out_card_count[table._current_player] = 0;
			Arrays.fill(table._cur_out_card_data[table._current_player], GameConstants.INVALID_CARD);
		}

		// 显示出牌
		table.operate_out_card(_out_card_player, 0, null, GameConstants.TCDG_CT_PASS, GameConstants.INVALID_SEAT, false);

		table.refresh_user_get_score(GameConstants.INVALID_SEAT);
	}

	/**
	 * 对当前出牌人的牌数据，进行合理的转换，并和上一次出牌人的牌型进行大小比对。如果当前出牌人的牌型比上一次出牌人的牌型小，返回错误的牌型值。
	 * 
	 * @param table
	 * @return
	 */
	public int adjust_out_card_right(Table_TongCheng table) {
		Arrays.fill(_out_change_cards_data, 0);

		int[] _tmp_out_cards_data = Arrays.copyOf(_out_cards_data, _out_cards_data.length);

		for (int i = 0; i < _out_card_count; i++) {
			if (_tmp_out_cards_data[i] > Constants_TongCheng.SPECIAL_CARD_TYPE) {
				_tmp_out_cards_data[i] = _tmp_out_cards_data[i] & 0xFF;
			}
		}

		table._logic.make_change_card(_out_change_cards_data, _out_card_count, _tmp_out_cards_data, _out_type);
		table._logic.sort_out_card_list(_out_change_cards_data, _out_card_count);

		int card_type = GameConstants.TCDG_CT_ERROR;
		card_type = table._logic.get_card_type_after_card_change(_out_change_cards_data, _out_card_count);

		if (card_type == GameConstants.TCDG_CT_ERROR) {
			return card_type;
		}
		if (table._turn_out_card_count != 0) {
			if (!table._logic.compare_card(table._turn_out_card_data, _out_change_cards_data, table._turn_out_card_count, _out_card_count)) {
				return GameConstants.TCDG_CT_ERROR;
			}
		}

		if (table.GRR == null) {
			return GameConstants.TCDG_CT_ERROR;
		}

		if (!table._logic.remove_card(_out_cards_data, _out_card_count, table.GRR._cards_data[_out_card_player],
				table.GRR._card_count[_out_card_player])) {
			return GameConstants.TCDG_CT_ERROR;
		}

		// 对手牌里的已经理过的牌 重新进行颜色标记
		int tmpSortCount = 0;
		int tmpFlag = 0;
		int flag = 0;
		for (int i = table.GRR._card_count[_out_card_player] - 1; i >= 0; i--) {
			if (table.GRR._cards_data[_out_card_player][i] > Constants_TongCheng.SPECIAL_CARD_TYPE) {
				int newTmpFlag = table.GRR._cards_data[_out_card_player][i] - (table.GRR._cards_data[_out_card_player][i] & 0xFF);
				if (newTmpFlag != tmpFlag) {
					tmpFlag = newTmpFlag;
					flag = (++tmpSortCount & 0xF) << 8;
				}
				table.GRR._cards_data[_out_card_player][i] = (table.GRR._cards_data[_out_card_player][i] & 0xFF) + flag;
			}
		}
		table.player_sort_card[_out_card_player] = tmpSortCount;

		return card_type;
	}

	@Override
	public boolean handler_player_be_in_room(Table_TongCheng table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_WSK_TCDG_RECONNECT_DATA);

		TableResponse_tcdg.Builder tableResponse = TableResponse_tcdg.newBuilder();
		table.load_player_info_data_reconnect(tableResponse);
		tableResponse.setRoomInfo(table.getRoomInfo());

		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(table._current_player);
		tableResponse.setPrevPlayer(table._prev_player);
		tableResponse.setPrOutCardPlayer(table._out_card_player);
		tableResponse.setPrCardsCount(table._turn_out_card_count);
		tableResponse.setPrOutCardType(table._turn_out_card_type);
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
			if (!table.hasRuleDisplayCount)
				cardCount = 0;
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
