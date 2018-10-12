package com.cai.game.hh.handler.yzchz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.constant.game.phz.Constants_YongZhou;
import com.cai.common.define.ELogType;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerDispatchCard_YongZhou extends HHHandlerDispatchCard<Table_YongZhou> {
	private int[] win_type = new int[4]; // 1：自摸；2：别人翻牌胡牌

	private int action_pao[] = new int[4];
	private int pao_type[][] = new int[4][1];

	private boolean reconnectDisplayCard = false;

	@Override
	public void exe(Table_YongZhou table) {
		reconnectDisplayCard = false;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			win_type[i] = 0;
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (table.GRR._left_card_count == 0) {
			table.liu_ju();
			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		table.GRR._left_card_count--;

		// if (table.DEBUG_CARDS_MODE) {
		// _send_card_data = 0x02;
		// }

		table._send_card_data = _send_card_data;
		table._current_player = _seat_index;
		table._provide_player = _seat_index;
		table._last_card = _send_card_data;

		action_pao = new int[table.getTablePlayerNumber()];
		pao_type = new int[table.getTablePlayerNumber()][1];

		if (_send_card_data == Constants_YongZhou.MAGIC_CARD) {
			table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

			GameSchedule.put(() -> {
				try {
					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index_yongzhou(_send_card_data)]++;

					int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);

					table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
							table.GRR._weave_items[_seat_index]);

					ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
					chr.set_empty();

					int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

					table.cardWhenWin = _send_card_data;
					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index_yongzhou(_send_card_data)]--;

					boolean hasAction = table.wang_pai_pai_xing_check(_seat_index, _seat_index, _send_card_data, card_type)
							|| table.normal_pai_xing_check(_seat_index, _seat_index, _send_card_data, card_type);

					table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index_yongzhou(_send_card_data)]++;

					if (hasAction) {
						reconnectDisplayCard = true;

						win_type[_seat_index] = 1;

						curPlayerStatus.add_action(GameConstants.WIK_NULL);
						curPlayerStatus.add_pass(0, _seat_index);

						if (curPlayerStatus.has_action()) {
							curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
							table.operate_player_action(_seat_index, false);
						}
					} else {
						chr.set_empty();

						int card_count = 0;
						for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
							if (table.GRR._cards_index[_seat_index][j] < 3)
								card_count += table.GRR._cards_index[_seat_index][j];
						}
						if (card_count == 0) {
							table._is_xiang_gong[_seat_index] = true;
							table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
						}

						if (table._is_xiang_gong[_seat_index]) {
							int next_player = table.get_banker_next_seat(_seat_index);
							table._playerStatus[_seat_index].clean_action();
							table._playerStatus[_seat_index].clean_status();
							table._current_player = next_player;
							table._last_player = next_player;

							table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

							return;
						} else {
							if (table._ti_mul_long[_seat_index] == 0) {
								table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
								table.operate_player_status();
							} else {
								table._ti_mul_long[_seat_index]--;

								int next_player = table.get_banker_next_seat(_seat_index);
								table._current_player = next_player;
								table._last_player = next_player;
								table._last_card = 0;

								table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}, table.time_for_display_wang_pai, TimeUnit.MILLISECONDS);

			return;
		}

		int ti_sao = table.estimate_player_ti_wei_respond(_seat_index, table._send_card_data, true);
		if (ti_sao != GameConstants.WIK_NULL) {
			table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, true);
			return;
		}

		table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

		ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}

		int bHupai = 0;
		boolean has_hu[] = new boolean[table.getTablePlayerNumber()];

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;

		int pCount = table.getTablePlayerNumber();

		for (int p = 0; p < table.getTablePlayerNumber(); p++)
			action_pao[p] = table.estimate_player_respond_phz_chd(p, _seat_index, table._send_card_data, pao_type[p], true);

		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (table._current_player + p) % table.getTablePlayerNumber();
			if (pCount == 4 && i == table.zuo_xing_seat)
				continue;

			if (_seat_index != i)
				card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;
			else
				card_type = GameConstants.HU_CARD_TYPE_ZIMO;

			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;

			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();

			table.cardWhenWin = _send_card_data;
			int tmpMagicCount = table.GRR._cards_index[i][Constants_YongZhou.MAX_CARD_INDEX - 1];

			if (i != _seat_index)
				table.is_wang_zha_others_card = true;

			boolean only_check_normal = false;
			for (int x = 0; x < table.getTablePlayerNumber(); x++) {
				if (action_pao[x] != 0)
					only_check_normal = true;
			}

			if (tmpMagicCount > 0) {
				if (i == _seat_index) {
					// 任何时候，翻牌人都可以判断王牌牌型和正常胡牌
					has_hu[i] = table.wang_pai_pai_xing_check(i, _seat_index, _send_card_data, card_type)
							|| table.normal_pai_xing_check(i, _seat_index, _send_card_data, card_type);
				} else if (i == table.get_banker_next_seat(_seat_index) && only_check_normal == false) {
					// 手里有王牌时，并且没人有跑时，判断王牌牌型
					has_hu[i] = table.wang_pai_pai_xing_check(i, _seat_index, _send_card_data, card_type);
				}
			} else {
				if (i == _seat_index) {
					// 任何时候，翻牌人都可以判断王牌牌型和正常胡牌
					has_hu[i] = table.wang_pai_pai_xing_check(i, _seat_index, _send_card_data, card_type)
							|| table.normal_pai_xing_check(i, _seat_index, _send_card_data, card_type);
				} else {
					if (i == table.get_banker_next_seat(_seat_index)) {
						if (only_check_normal) {
							// 手里无王牌，但是有人有跑时，判断正常胡牌
							has_hu[i] = table.normal_pai_xing_check(i, _seat_index, _send_card_data, card_type);
						} else {
							// 手里无王牌，没人有跑时，判断王牌牌型和正常胡牌
							has_hu[i] = table.wang_pai_pai_xing_check(i, _seat_index, _send_card_data, card_type)
									|| table.normal_pai_xing_check(i, _seat_index, _send_card_data, card_type);
						}
					} else {
						// 上家只判断正常牌型
						has_hu[i] = table.normal_pai_xing_check(i, _seat_index, _send_card_data, card_type);
					}
				}
			}

			table.is_wang_zha_others_card = false;

			if (table._is_xiang_gong[i] == true)
				has_hu[i] = false;

			if (has_hu[i]) {
				if (_seat_index != i) {
					win_type[i] = 2;
				} else {
					win_type[i] = 1;
				}

				if (action_pao[i] != GameConstants.WIK_PAO) {
					tempPlayerStatus.add_action(GameConstants.WIK_NULL);
					tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
				} else {
					tempPlayerStatus.add_action(GameConstants.WIK_PAO);
					tempPlayerStatus.add_pao(table._send_card_data, _seat_index);
				}

				ti_sao = GameConstants.WIK_ZI_MO;

				if (tempPlayerStatus.has_action_by_code(GameConstants.WIK_CHI_HU) || tempPlayerStatus.has_action_by_code(GameConstants.WIK_ZI_MO)) {
					bHupai = 1;
					if (tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_ZHA)
							|| tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_CHUANG)
							|| tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_DIAO)) {
						tempPlayerStatus.delete_action(GameConstants.WIK_CHI_HU);
						tempPlayerStatus.delete_action(GameConstants.WIK_ZI_MO);
					}
				}

				if (i == _seat_index) {
					if (tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_ZHA)
							|| tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_CHUANG)
							|| tempPlayerStatus.has_action_by_code(GameConstants.WIK_WANG_DIAO)) {
						bHupai = 1;
					}
				}
			} else {
				chr[i].set_empty();
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((action_pao[i] != GameConstants.WIK_NULL) && (bHupai == 0)) {
				ti_sao = GameConstants.WIK_PAO;
				table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false,
						table.time_for_operate_dragon);
				return;
			} else if (action_pao[i] != GameConstants.WIK_NULL) {
				ti_sao = GameConstants.WIK_PAO;
			}
		}

		boolean bAroseAction = false;

		int real_pao_type = 0;
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (action_pao[i] != 0)
				real_pao_type = pao_type[i][0];
		}

		if (ti_sao != GameConstants.WIK_PAO || bHupai == 1) {
			// 有人有跑或有胡的时候 需要弹吃碰 因为永州扯胡子是暗偎玩法 不然让别人知道我偎的牌是哪张
			if (table.GRR._left_card_count > 0 && _send_card_data != Constants_YongZhou.MAGIC_CARD) { // 不是最后一张牌才判断吃碰
				bAroseAction = table.estimate_player_out_card_respond_yzchz(_seat_index, table._send_card_data, true, table.zuo_xing_seat);
			}
		}

		if ((bAroseAction == false) && (ti_sao == GameConstants.WIK_NULL)) {
			table.operate_player_action(_seat_index, true);
		} else {
			reconnectDisplayCard = true;

			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (table._playerStatus[i].has_action() && real_pao_type != 0) {
					boolean del = false;
					if (real_pao_type == GameConstants.PAO_TYPE_MINE_PENG_PAO) {
						// 任何人的碰行成的跑，都不能弹吃
						del = true;
					} else if (action_pao[i] != 0) {
						// 如果吃牌人自己有跑
						del = true;
					} // 其他情况下都要弹跑

					if (del) {
						table._playerStatus[i].delete_action(GameConstants.WIK_LEFT);
						table._playerStatus[i].delete_action(GameConstants.WIK_RIGHT);
						table._playerStatus[i].delete_action(GameConstants.WIK_CENTER);
						table._playerStatus[i].delete_action(GameConstants.WIK_DDX);
						table._playerStatus[i].delete_action(GameConstants.WIK_XXD);
						table._playerStatus[i].delete_action(GameConstants.WIK_EQS);
					}
				}

				if (table._playerStatus[i]._action_count == 1 && table._playerStatus[i].has_action_by_code(GameConstants.WIK_NULL))
					table._playerStatus[i].delete_action(GameConstants.WIK_NULL);

				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

		if (curPlayerStatus.has_action()) { // 有动作
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			if (ti_sao == GameConstants.WIK_NULL) {
				if (bAroseAction == false) {
					int next_player = table.get_banker_next_seat(_seat_index);

					int cIndex = table._logic.switch_to_card_index(table._send_card_data);
					table._cannot_chi[_seat_index][cIndex]++;
					table._cannot_chi[next_player][cIndex]++;

					table._current_player = next_player;

					table._last_card = table._send_card_data;
					table._last_player = table._current_player;

					GameSchedule.put(new YzDispatchAddDiscardRunnable(table.getRoom_id(), _seat_index, next_player, table._send_card_data),
							table.time_for_add_discard + 200, TimeUnit.MILLISECONDS);

					_seat_index = next_player;
				}
			}
		}

		return;
	}

	@SuppressWarnings("static-access")
	@Override
	public boolean handler_operate_card(Table_YongZhou table, int seat_index, int operate_code, int operate_card, int luoCode) {
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
		if (operate_card != table._send_card_data) {
			table.log_player_error(seat_index, "操作牌，与当前牌不一样");
			return true;
		}

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
			if (luoCode != -1)
				playerStatus.set_lou_pai_kind(luoCode);
		}

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
					cbUserActionRank = table._logic.get_action_rank_yongzhou(table._playerStatus[i].get_perform(), win_type[i])
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbUserActionRank = table._logic.get_action_list_rank_yongzhou(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action, win_type[i]) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					cbTargetActionRank = table._logic.get_action_rank_yongzhou(table._playerStatus[target_player].get_perform(),
							win_type[target_player]) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					cbTargetActionRank = table._logic.get_action_list_rank_yongzhou(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action, win_type[target_player]) + target_p;
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

		int target_card = table._playerStatus[target_player]._operate_card;

		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS;
		int cIndex = table._logic.switch_to_card_index(table._send_card_data);
		if (target_action == GameConstants.WIK_NULL) {
			int next_player = table.get_banker_next_seat(_seat_index);

			table._cannot_chi[_seat_index][cIndex]++;
			table._cannot_chi[next_player][cIndex]++;

			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][cIndex]++;
					}
				}
			}
		} else if ((target_action & eat_type) != GameConstants.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][cIndex]++;
					}
				}
			}

			if (_seat_index != target_player) {
				table._cannot_chi[_seat_index][cIndex]++;
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		int cards_cur[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
		int hand_card_count_cur = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards_cur);
		table.operate_player_cards(_seat_index, hand_card_count_cur, cards_cur, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);

		reconnectDisplayCard = false;

		switch (target_action) {
		case GameConstants.WIK_NULL: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (_send_card_data == Constants_YongZhou.MAGIC_CARD) {
				int pai_count = 0;
				for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}

				if (pai_count == 0) {
					table._is_xiang_gong[_seat_index] = true;
					table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
					int next_player = table.get_banker_next_seat(_seat_index);

					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.delay_when_passed);
					return true;
				}

				if (table._ti_mul_long[_seat_index] == 0) {
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				} else {
					table._ti_mul_long[_seat_index]--;

					int next_player = table.get_banker_next_seat(_seat_index);
					table._current_player = next_player;
					table._last_player = next_player;
					table._last_card = 0;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
				}
			} else {
				int next_player = table.get_banker_next_seat(_seat_index);

				table._current_player = next_player;
				table._last_player = next_player;

				table._last_card = table._send_card_data;

				GameSchedule.put(new YzDispatchAddDiscardRunnable(table.getRoom_id(), _seat_index, next_player, table._send_card_data),
						table.delay_when_passed, TimeUnit.MILLISECONDS);

				_seat_index = next_player;
			}

			return true;
		}
		case GameConstants.WIK_LEFT: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][0]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_RIGHT: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][2]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_CENTER: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][1]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_XXD: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int target_card_color = table._logic.get_card_color(target_card);

			int cbRemoveCard[] = new int[2];
			if (target_card_color == 0) {
				cbRemoveCard[0] = target_card;
				cbRemoveCard[1] = target_card + 16;
			} else {
				cbRemoveCard[0] = target_card - 16;
				cbRemoveCard[1] = target_card - 16;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][4]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_DDX: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int target_card_color = table._logic.get_card_color(target_card);

			int cbRemoveCard[] = new int[2];
			if (target_card_color == 0) {
				cbRemoveCard[0] = target_card + 16;
				cbRemoveCard[1] = target_card + 16;
			} else {
				cbRemoveCard[0] = target_card - 16;
				cbRemoveCard[1] = target_card;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][5]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_EQS: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int cbRemoveCard[] = new int[] { target_card, target_card };
			int target_card_value = table._logic.get_card_value(target_card);
			switch (target_card_value) {
			case 2:
				cbRemoveCard[0] = target_card + 5;
				cbRemoveCard[1] = target_card + 8;
				break;
			case 7:
				cbRemoveCard[0] = target_card - 5;
				cbRemoveCard[1] = target_card + 3;
				break;
			case 10:
				cbRemoveCard[0] = target_card - 8;
				cbRemoveCard[1] = target_card - 3;
				break;

			default:
				break;
			}
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave_yzchz(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][3]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_PENG: {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (action_pao[i] != GameConstants.WIK_NULL) {
					table.exe_gang(i, _seat_index, table._send_card_data, action_pao[i], pao_type[i][0], true, true, false, table.delay_when_passed);
					return true;
				}
			}

			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "碰牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants.CHI_PENG_TYPE_DISPATCH, target_lou_code);
			return true;
		}
		case GameConstants.WIK_PAO: {
			if (action_pao[target_player] != GameConstants.WIK_NULL) {
				table.exe_gang(target_player, _seat_index, table._send_card_data, action_pao[target_player], pao_type[target_player][0], true, true,
						false, table.delay_when_passed);
			}

			return true;
		}
		case GameConstants.WIK_ZI_MO:
		case GameConstants.WIK_CHI_HU:
		case GameConstants.WIK_WANG_ZHA:
		case GameConstants.WIK_WANG_CHUANG:
		case GameConstants.WIK_WANG_DIAO: {
			boolean changed = false;

			if (target_action == GameConstants.WIK_WANG_ZHA || target_action == GameConstants.WIK_WANG_CHUANG
					|| target_action == GameConstants.WIK_WANG_DIAO) {
				int[] cards_index = Arrays.copyOf(table.GRR._cards_index[target_player], table.GRR._cards_index[target_player].length);

				if (target_action == GameConstants.WIK_WANG_ZHA)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 3;
				if (target_action == GameConstants.WIK_WANG_CHUANG)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 2;
				if (target_action == GameConstants.WIK_WANG_DIAO)
					cards_index[Constants_YongZhou.MAGIC_CARD_INDEX] -= 1;

				int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data_yongzhou(cards_index, cards);

				table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
						table.GRR._weave_items[target_player]);
			}

			if (target_player != _seat_index && (target_action == GameConstants.WIK_WANG_ZHA || target_action == GameConstants.WIK_WANG_CHUANG
					|| target_action == GameConstants.WIK_WANG_DIAO)) {
				if (target_action == GameConstants.WIK_WANG_ZHA) {
					table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1, new long[] { Constants_YongZhou.CHR_WANG_ZHA },
							1, GameConstants.INVALID_SEAT);
				} else if (target_action == GameConstants.WIK_WANG_CHUANG) {
					table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1,
							new long[] { Constants_YongZhou.CHR_WANG_CHUANG }, 1, GameConstants.INVALID_SEAT);
				} else if (target_action == GameConstants.WIK_WANG_DIAO) {
					table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_HU, 1,
							new long[] { Constants_YongZhou.CHR_WANG_DIAO }, 1, GameConstants.INVALID_SEAT);
				}

				if (table.GRR._left_card_count == 0) {
					table.liu_ju();
					return true;
				}

				changed = true;
				table.changed = true;

				table.operate_player_get_card(target_player, 0, null, GameConstants.INVALID_SEAT, false);

				table._send_card_count++;
				_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
				table.GRR._left_card_count--;

				if (table.DEBUG_CARDS_MODE) {
					_send_card_data = 0xFF;
				}

				table._send_card_data = _send_card_data;
				table._current_player = target_player;
				table._provide_player = target_player;
				table._last_card = _send_card_data;

				table.cardWhenWin = _send_card_data;

				table.operate_player_get_card(target_player, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);

				int hu_xi[] = new int[1];
				int action = 0;
				if (target_action == GameConstants.WIK_WANG_ZHA) {
					table.wangZhaChr[target_player].set_empty();
					action = table.analyse_wang_zha(table.GRR._cards_index[target_player], table.GRR._weave_items[target_player],
							table.GRR._weave_count[target_player], target_player, target_player, _send_card_data, table.wangZhaChr[target_player],
							GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true);
				}
				if (target_action == GameConstants.WIK_WANG_CHUANG) {
					table.wangChuangChr[target_player].set_empty();
					action = table.analyse_wang_chuang(table.GRR._cards_index[target_player], table.GRR._weave_items[target_player],
							table.GRR._weave_count[target_player], target_player, target_player, _send_card_data, table.wangChuangChr[target_player],
							GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true);
				}
				if (target_action == GameConstants.WIK_WANG_DIAO) {
					table.wangDiaoChr[target_player].set_empty();
					action = table.analyse_wang_diao(table.GRR._cards_index[target_player], table.GRR._weave_items[target_player],
							table.GRR._weave_count[target_player], target_player, target_player, _send_card_data, table.wangDiaoChr[target_player],
							GameConstants.HU_CARD_TYPE_ZIMO, hu_xi, true);
				}

				final int act = target_action;
				final int tp = target_player;
				GameSchedule.put(() -> {
					ChiHuRight chr = null;
					int type_count = 0;
					long[] type_list = new long[30];
					if (act == GameConstants.WIK_WANG_ZHA) {
						chr = table.wangZhaChr[tp];
						for (int i = 0; i < chr.type_count; i++) {
							if (chr.type_list[i] != Constants_YongZhou.CHR_WANG_ZHA_WANG && chr.type_list[i] != Constants_YongZhou.CHR_WANG_ZHA)
								type_list[type_count++] = chr.type_list[i];
						}
					} else if (act == GameConstants.WIK_WANG_CHUANG) {
						chr = table.wangChuangChr[tp];
						for (int i = 0; i < chr.type_count; i++) {
							if (chr.type_list[i] != Constants_YongZhou.CHR_WANG_CHUANG_WANG && chr.type_list[i] != Constants_YongZhou.CHR_WANG_CHUANG)
								type_list[type_count++] = chr.type_list[i];
						}
					} else if (act == GameConstants.WIK_WANG_DIAO) {
						chr = table.wangDiaoChr[tp];
						for (int i = 0; i < chr.type_count; i++) {
							if (chr.type_list[i] != Constants_YongZhou.CHR_WANG_DIAO_WANG && chr.type_list[i] != Constants_YongZhou.CHR_WANG_DIAO)
								type_list[type_count++] = chr.type_list[i];
						}
					}

					table.operate_effect_action(tp, GameConstants.EFFECT_ACTION_TYPE_HU, type_count, type_list, 1, GameConstants.INVALID_SEAT);
				}, 500, TimeUnit.MILLISECONDS);

				if (action == 0) {
					table.log_error("能胡王牌牌型的牌判断出错！！");
					MongoDBServiceImpl.getInstance().server_error_log(table.getRoom_id(), ELogType.roomLogicError, "能胡王牌牌型的牌判断出错！！", 0L,
							SysGameTypeDict.getInstance().getGameDescByTypeIndex(table.getGameTypeIndex()), table.getGame_id());
					return true;
				}
			}

			if (changed)
				operate_card = _send_card_data;

			table.change_hu_count_and_weave_items(target_action, target_player);

			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			table.set_niao_card(target_player, operate_card, true);

			if (!table.GRR._chi_hu_rights[target_player].opr_and(Constants_YongZhou.CHR_ZI_MO).is_empty()) {
				table._player_result.zi_mo_count[target_player]++;
			}

			table._player_result.hu_pai_count[target_player]++;
			table._player_result.ying_xi_count[target_player] += table._hu_xi[target_player];

			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			int delay = table.time_for_display_win_border;

			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(Table_YongZhou table, int seat_index) {
		RoomResponse.Builder roomResponse = RoomResponse.newBuilder();
		roomResponse.setType(MsgConstants.RESPONSE_RECONNECT_DATA);

		TableResponse.Builder tableResponse = TableResponse.newBuilder();

		table.load_room_info_data(roomResponse);
		table.load_player_info_data(roomResponse);
		table.load_common_status(roomResponse);

		// 游戏变量
		tableResponse.setBankerPlayer(table.GRR._banker_player);
		tableResponse.setCurrentPlayer(_seat_index);
		tableResponse.setCellScore(0);

		// 状态变量
		tableResponse.setActionCard(0);
		// tableResponse.setActionMask((_response[seat_index] == false) ?
		// _player_action[seat_index] : MJGameConstants.WIK_NULL);

		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		int pCount = table.getTablePlayerNumber();
		for (int i = 0; i < pCount; i++) {
			if (pCount == 4 && i == table.zuo_xing_seat) {
				int tmpI = table.GRR._banker_player;

				tableResponse.addTrustee(false);

				tableResponse.addDiscardCount(0);
				Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
				tableResponse.addDiscardCards(int_array);

				tableResponse.addWeaveCount(0);
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				tableResponse.addWeaveItemArray(weaveItem_array);

				tableResponse.addWinnerOrder(0);
				tableResponse.addHuXi(0);

				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[tmpI]));
			} else {
				tableResponse.addTrustee(false);// 是否托管
				// 剩余牌数
				tableResponse.addDiscardCount(table.GRR._discard_count[i]);
				Int32ArrayResponse.Builder int_array = Int32ArrayResponse.newBuilder();
				for (int j = 0; j < 55; j++) {
					int_array.addItem(table.GRR._discard_cards[i][j]);
				}
				tableResponse.addDiscardCards(int_array);

				// 组合扑克
				tableResponse.addWeaveCount(table.GRR._weave_count[i]);
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
					weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
					weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
					if (seat_index != i) {
						if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
								|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
								|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
								&& table.GRR._weave_items[i][j].public_card == 0) {
							weaveItem_item.setCenterCard(0);
						} else {
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
						}
					} else {
						weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
					}
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				tableResponse.addWeaveItemArray(weaveItem_array);

				//
				tableResponse.addWinnerOrder(0);
				tableResponse.addHuXi(table._hu_xi[i]);

				// 牌
				if (i == _seat_index) {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
				} else {
					tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));
				}
			}
		}

		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];

		int hand_card_count = 0;
		if (pCount == 4 && seat_index == table.zuo_xing_seat) {
			hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[table.GRR._banker_player], hand_cards);
		} else {
			hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[seat_index], hand_cards);
		}

		for (int i = 0; i < hand_card_count; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}

		roomResponse.setTable(tableResponse);

		table.send_response_to_player(seat_index, roomResponse);

		// 摸牌
		if (_send_card_data != 0 && _send_card_data != Constants_YongZhou.MAGIC_CARD && reconnectDisplayCard)
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}

		if (table._is_xiang_gong[seat_index] == true)
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);

		table.istrustee[seat_index] = false;

		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
