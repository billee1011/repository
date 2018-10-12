package com.cai.game.hongershi;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.GameConstants_KWX;
import com.cai.common.constant.game.hongershi.HongErShiConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class HandlerDispatchCard extends HHHandlerDispatchCard<HongErShiTable> {

	public int[] _send_two_card_data = new int[2];

	public boolean _send_two_card = false;

	@SuppressWarnings("static-access")
	@Override
	public void exe(HongErShiTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table.logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i], table.GRR._weave_count[i],
						GameConstants.INVALID_SEAT);
			}

			table.cal_cha_jiao(); // 查叫

			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;

			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x4;
		}

		table._send_card_data = _send_card_data;
		table._current_player = _seat_index;
		table._provide_player = _seat_index;
		table._last_card = _send_card_data;

		if (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { HongErShiConstants.WIK_TOU }, 5,
					GameConstants.INVALID_SEAT);
		}

		if (_type == GameConstants.WIK_NULL) {
			table.is_mo_or_show = false;
			while (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {
				table.is_mo_or_show = true;
				table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

				if (!table.has_king_tou[_seat_index]) {
					int cbWeaveIndex = table.GRR._weave_count[_seat_index];
					table.GRR._weave_count[_seat_index]++;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = HongErShiConstants.MAGIC_CARD_KING;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_TOU;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = new int[] { HongErShiConstants.MAGIC_CARD_KING };

					table.has_king_tou[_seat_index] = true;
				} else {
					for (int wc = 0; wc < table.GRR._weave_count[_seat_index]; wc++) {
						if (table.GRR._weave_items[_seat_index][wc].center_card != HongErShiConstants.MAGIC_CARD_KING) {
							continue;
						}

						int[] king_card = new int[HongErShiConstants.KING_MAX_COUNT];
						int king_count = 0;
						king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
						for (int c = 0; c < table.GRR._weave_items[_seat_index][wc].weave_card.length; c++) {
							king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
						}

						table.GRR._weave_items[_seat_index][wc].weave_card = Arrays.copyOf(king_card, king_count);
					}
				}

				table.operate_player_cards(_seat_index, table.GRR._card_count[_seat_index], table.GRR._cards_data[_seat_index],
						table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

				if (table.GRR._left_card_count == 0) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
					}

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						int cards[] = new int[GameConstants.MAX_HH_COUNT];
						int hand_card_count = table.logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

						table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
								table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
					}

					table.cal_cha_jiao(); // 查叫

					table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					table._shang_zhuang_player = GameConstants.INVALID_SEAT;

					table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
					return;
				}

				table._send_card_count++;
				_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				table.GRR._left_card_count--;

				if (table.DEBUG_CARDS_MODE) {
					_send_card_data = 0x4;
				}
				table._send_card_data = _send_card_data;
				table._last_card = _send_card_data;
			}

			if (table.is_mo_or_show) {
				table.operate_player_mo_card(_seat_index, 1, new int[] { _send_card_data }, table.GRR._card_count[_seat_index],
						GameConstants.INVALID_SEAT, true);
			} else {
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
			}

		} else if (_type == GameConstants.GANG_TYPE_AN_GANG) {
			boolean have_king = false;
			if (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {
				have_king = true;
			}
			_send_two_card_data[0] = _send_card_data;
			table.GRR._cards_data[_seat_index][table.GRR._card_count[_seat_index]] = _send_card_data;

			if (table.GRR._left_card_count == 0) {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
				}

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants.MAX_HH_COUNT];
					int hand_card_count = table.logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
				}

				table.cal_cha_jiao(); // 查叫

				table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._shang_zhuang_player = GameConstants.INVALID_SEAT;

				table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
				return;
			}

			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			table.GRR._left_card_count--;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x31;
			}
			if (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {
				have_king = true;
			}
			_send_two_card_data[1] = _send_card_data;
			_send_two_card = true;

			while (have_king) {
				have_king = false;

				int cardsIndex[] = new int[53];
				table.logic.switch_to_cards_index_real(table.GRR._cards_data[_seat_index], 0, table.GRR._card_count[_seat_index], cardsIndex);
				int cards[] = new int[HongErShiConstants.MAX_COUNT];
				int hand_card_count = table.logic.switch_to_cards_data(cardsIndex, cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);

				table.operate_player_mo_card(_seat_index, 2, _send_two_card_data, hand_card_count, GameConstants.INVALID_SEAT, true);

				for (int i = 0; i < 2; i++) {
					if (_send_two_card_data[i] != HongErShiConstants.MAGIC_CARD_KING) {
						continue;
					}

					if (!table.has_king_tou[_seat_index]) {
						int cbWeaveIndex = table.GRR._weave_count[_seat_index];
						table.GRR._weave_count[_seat_index]++;
						table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
						table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = HongErShiConstants.MAGIC_CARD_KING;
						table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_TOU;
						table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
						table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = new int[] { HongErShiConstants.MAGIC_CARD_KING };

						table.has_king_tou[_seat_index] = true;
					} else {
						for (int wc = 0; wc < table.GRR._weave_count[_seat_index]; wc++) {
							if (table.GRR._weave_items[_seat_index][wc].center_card != HongErShiConstants.MAGIC_CARD_KING) {
								continue;
							}

							int[] king_card = new int[HongErShiConstants.KING_MAX_COUNT];
							int king_count = 0;
							king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
							for (int c = 0; c < table.GRR._weave_items[_seat_index][wc].weave_card.length; c++) {
								king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
							}

							table.GRR._weave_items[_seat_index][wc].weave_card = Arrays.copyOf(king_card, king_count);
						}
					}

					if (table.GRR._left_card_count == 0) {
						for (int m = 0; m < table.getTablePlayerNumber(); m++) {
							table.GRR._chi_hu_card[m][0] = GameConstants.INVALID_VALUE;
						}

						for (int m = 0; m < table.getTablePlayerNumber(); m++) {
							cards = new int[GameConstants.MAX_HH_COUNT];
							hand_card_count = table.logic.switch_to_cards_data(table.GRR._cards_index[m], cards);

							table.operate_show_card(m, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[m],
									table.GRR._weave_count[m], GameConstants.INVALID_SEAT);
						}

						table.cal_cha_jiao(); // 查叫

						table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
						table._shang_zhuang_player = GameConstants.INVALID_SEAT;

						table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
						return;
					}

					table._send_card_count++;
					_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
					table.GRR._left_card_count--;

					if (table.DEBUG_CARDS_MODE) {
						_send_card_data = 0x4F;
					}

					if (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {
						have_king = true;
					} else {
						have_king |= false;
					}

					_send_two_card_data[i] = _send_card_data;
					table._send_card_data = _send_card_data;
					table._last_card = _send_card_data;
				}
			}
			table.operate_player_mo_card(_seat_index, 2, _send_two_card_data, table.GRR._card_count[_seat_index], GameConstants.INVALID_SEAT, true);

			table.is_mo_or_show = true;
		} else {
			while (_send_card_data == HongErShiConstants.MAGIC_CARD_KING) {

				table.operate_player_mo_card(_seat_index, 1, new int[] { _send_card_data }, table.GRR._card_count[_seat_index],
						GameConstants.INVALID_SEAT, true);

				if (!table.has_king_tou[_seat_index]) {
					int cbWeaveIndex = table.GRR._weave_count[_seat_index];
					table.GRR._weave_count[_seat_index]++;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = HongErShiConstants.MAGIC_CARD_KING;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = HongErShiConstants.WIK_TOU;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card = new int[] { HongErShiConstants.MAGIC_CARD_KING };

					table.has_king_tou[_seat_index] = true;
				} else {
					for (int wc = 0; wc < table.GRR._weave_count[_seat_index]; wc++) {
						if (table.GRR._weave_items[_seat_index][wc].center_card != HongErShiConstants.MAGIC_CARD_KING) {
							continue;
						}

						int[] king_card = new int[HongErShiConstants.KING_MAX_COUNT];
						int king_count = 0;
						king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
						for (int c = 0; c < table.GRR._weave_items[_seat_index][wc].weave_card.length; c++) {
							king_card[king_count++] = HongErShiConstants.MAGIC_CARD_KING;
						}

						table.GRR._weave_items[_seat_index][wc].weave_card = Arrays.copyOf(king_card, king_count);
					}
				}

				table.operate_player_cards(_seat_index, table.GRR._card_count[_seat_index], table.GRR._cards_data[_seat_index],
						table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

				if (table.GRR._left_card_count == 0) {
					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
					}

					for (int i = 0; i < table.getTablePlayerNumber(); i++) {
						int cards[] = new int[GameConstants.MAX_HH_COUNT];
						int hand_card_count = table.logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

						table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
								table.GRR._weave_count[i], GameConstants.INVALID_SEAT);
					}

					table.cal_cha_jiao(); // 查叫

					table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					table._shang_zhuang_player = GameConstants.INVALID_SEAT;

					table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
					return;
				}

				table._send_card_count++;
				_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				if (table.DEBUG_CARDS_MODE) {
					_send_card_data = 0x4;
				}
				table.GRR._left_card_count--;

				table._send_card_data = _send_card_data;
				table._last_card = _send_card_data;
			}

			table.operate_player_cards(_seat_index, table.GRR._card_count[_seat_index], table.GRR._cards_data[_seat_index],
					table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
			table.operate_player_mo_card(_seat_index, 1, new int[] { table._send_card_data }, table.GRR._card_count[_seat_index],
					GameConstants.INVALID_SEAT, true);

			table.is_mo_or_show = true;
		}

		boolean bAroseAction = false;

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}

		int action_hu[] = new int[table.getTablePlayerNumber()];

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (table._current_player + p + table.getTablePlayerNumber()) % table.getTablePlayerNumber();
			// if (_seat_index != i) {
			// continue;
			// }
			if (_type != GameConstants.WIK_NULL || (_type == GameConstants.WIK_NULL && table.is_mo_or_show)) {
				if (i != _seat_index) {
					continue;
				}
			}
			if (table.is_mo_or_show && i != _seat_index) {
				continue;
			}

			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();

			if (table.is_mo_or_show) {
				card_type = HongErShiConstants.WIK_GANG_SHANG_HUA;
			}
			action_hu[i] = table.analyse_chi_hu_card(i, _seat_index, _send_card_data, card_type, chr[i], true);

			if (action_hu[i] != GameConstants.WIK_NULL) {
				if (_seat_index != i) {
					tempPlayerStatus.add_action(HongErShiConstants.WIK_CHI_HU);
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_tou(table._send_card_data, HongErShiConstants.WIK_CHI_HU, i);
				} else {
					tempPlayerStatus.add_action(HongErShiConstants.WIK_ZI_MO);
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_tou(table._send_card_data, HongErShiConstants.WIK_ZI_MO, i);
				}

				bAroseAction = true;
			} else {
				chr[i].set_empty();
			}
		}

		if (_type != GameConstants.WIK_NULL || (_type == GameConstants.WIK_NULL && table.is_mo_or_show)) {
			if (_type == GameConstants.GANG_TYPE_AN_GANG) {
				table.GRR._cards_data[_seat_index][table.GRR._card_count[_seat_index]++] = _send_two_card_data[0];
			}
			bAroseAction |= table.estimate_player_mo_card_response(_seat_index, _send_card_data, 0, false);
			table.GRR._cards_data[_seat_index][table.GRR._card_count[_seat_index]++] = _send_card_data;
		} else {
			bAroseAction |= table.estimate_player_card_respond(_seat_index, _send_card_data, true);
		}

		if (bAroseAction) { // 有动作
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		} else {
			if (_type != GameConstants.WIK_NULL || (_type == GameConstants.WIK_NULL && table.is_mo_or_show)) {
				if (table.player_bao_ting[_seat_index]) {
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					// table.operate_player_status();
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_KWX.GANG_LAST_CARD_DELAY,
							TimeUnit.MILLISECONDS);
					if (table.player_bao_ting[_seat_index]) {
						table._playerStatus[_seat_index].set_status(GameConstants.CARD_STATUS_BAO_TING);
						table.operate_player_action(_seat_index, false);
					}
				} else {
					table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			} else {
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

				int discard_time = 2000;
				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
				SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
				if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
					discard_time = sysParamModel1104.getVal4();
				}

				if (table._last_card != 0)
					table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, discard_time);

				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = table._send_card_data;
				table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._send_card_data;

				table._current_player = next_player;
				_seat_index = next_player;

				int dispatch_time = 3000;
				if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
					dispatch_time = sysParamModel1104.getVal5();
				}
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);

				table._last_card = table._send_card_data;
				table._last_player = table._current_player;
			}
		}

		return;
	}

	@Override
	public boolean handler_operate_card(HongErShiTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("没有这个操作:" + operate_code);
			return false;
		}
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return true;
		}
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}

		if (table._playerStatus[seat_index].has_action_by_code(HongErShiConstants.WIK_CHI_HU) && operate_code != HongErShiConstants.WIK_CHI_HU) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(_send_card_data);
		}
		if (table._playerStatus[seat_index].has_action_by_code(HongErShiConstants.WIK_ZI_MO) && operate_code != HongErShiConstants.WIK_ZI_MO) {
			table._playerStatus[seat_index].add_cards_abandoned_hu(_send_card_data);
		}
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code = luoCode;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {

			int i = (_seat_index + p) % table.getTablePlayerNumber();
			int cbUserActionRank = 0;
			int cbTargetActionRank = 0;

			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					cbUserActionRank = table.logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = table.logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table.logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = table.logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("优先级最高的人还没操作");
			return true;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { target_action }, 1);

		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 5,
				GameConstants.INVALID_SEAT);

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			table._playerStatus[_seat_index].clean_action();
			// table._playerStatus[_seat_index].clean_status();
			if (_type != GameConstants.WIK_NULL || (_type == GameConstants.WIK_NULL && table.is_mo_or_show)) {
				if (table.player_bao_ting[target_player]) {
					GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_KWX.DELAY_JIAN_PAO_HU_NEW,
							TimeUnit.MILLISECONDS);
					return true;
				}
				table.change_player_status(_seat_index, GameConstants_KWX.Player_Status_OUT_CARD);
				table.operate_player_status();
			} else {
				// table.operate_player_get_card(_seat_index, 0, null,
				// GameConstants.INVALID_SEAT, false);

				int discard_time = 2000;
				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
				SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
				if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
					discard_time = sysParamModel1104.getVal4();
				}

				if (table._last_card != 0)
					table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, 1);

				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = table._send_card_data;
				table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = table._send_card_data;

				table._current_player = next_player;
				_seat_index = next_player;

				int dispatch_time = 3000;
				if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
					dispatch_time = sysParamModel1104.getVal5();
				}
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);

				table._last_card = table._send_card_data;
				table._last_player = table._current_player;

			}
			return true;
		}
		case HongErShiConstants.WIK_CHI: {
			int[] cbRemoveCard = new int[] { target_card };
			int count = table.logic.remove_cards_by_cards(table.GRR._cards_data[target_player], table.GRR._card_count[target_player], cbRemoveCard,
					1);
			if (count == -1) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			} else {
				table.GRR._card_count[target_player] = count;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH,
					new int[] { target_card, _send_card_data });
			return true;
		}
		case HongErShiConstants.WIK_PENG:
		case HongErShiConstants.WIK_AN_PENG: {
			int[] remove_cards = new int[3];
			int count = 0;

			for (int c = 0; c < table.GRR._card_count[target_player]; c++) {
				if (table.logic.get_card_value(table.GRR._cards_data[target_player][c]) == table.logic.get_card_value(target_card)) {
					remove_cards[count++] = table.GRR._cards_data[target_player][c];
				}

				int limit = target_action == HongErShiConstants.WIK_PENG ? 2 : 3;
				if (count == limit) {
					break;
				}
			}

			int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[target_player], table.GRR._card_count[target_player],
					remove_cards, 3);

			if (target_action == HongErShiConstants.WIK_PENG) {
				remove_cards[count++] = target_card;
			}

			if (card_count == -1) {
				table.log_player_error(target_player, "碰牌删除出错");
				return false;
			} else {
				table.GRR._card_count[target_player] = card_count;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, remove_cards);
			return true;
		}
		case HongErShiConstants.WIK_GANG: {
			for (int i = 0; i < table.m_gangCardResult[target_player].cbCardCount; i++) {
				if (operate_card == table.m_gangCardResult[target_player].cbCardData[i]) {
					return table.exe_gang(target_player, _seat_index, operate_card, target_action, table.m_gangCardResult[target_player].type[i],
							true, true, false, 1000);
				}
			}

			return true;
		}
		case HongErShiConstants.WIK_ZI_MO:
		case HongErShiConstants.WIK_CHI_HU: {
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			if (target_action == HongErShiConstants.WIK_ZI_MO) {
				table._player_result.zi_mo_count[target_player]++;
			}

			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HongErShiTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table.logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		// if (table._playerStatus[_seat_index].get_status() !=
		// GameConstants.Player_Status_OUT_CARD) {
		// table.log_error("状态不对不能出牌");
		// return false;
		// }

		// 删除扑克
		int card_count = table.logic.remove_cards_by_cards(table.GRR._cards_data[seat_index], table.GRR._card_count[seat_index], new int[] { card },
				1);
		if (card_count == -1) {
			table.log_error("出牌删除出错");
			return false;
		} else {
			table.GRR._card_count[seat_index] = card_count;
		}

		if (table.is_mo_or_show) {
			table.exe_out_card(_seat_index, card, HongErShiConstants.WIK_GANG_SHANG_PAO);
		} else {
			table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);
		}
		// 出牌

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HongErShiTable table, int seat_index) {

		table.handler_player_be_in_room(table, seat_index);
		return true;
	}
}
