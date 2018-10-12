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
import com.cai.game.hh.handler.HHHandlerGang;
import com.cai.service.MongoDBServiceImpl;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class HandlerGang_YongZhou extends HHHandlerGang<Table_YongZhou> {
	public int[] win_type = new int[4]; // 1：自摸；2：别人翻牌胡牌
	public boolean is_wang_zha_others = false;

	@Override
	public void exe(Table_YongZhou table) {
		is_wang_zha_others = false;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();

		if (_depatch == false)
			table.operate_out_card(this._provide_player, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
		else
			table.operate_player_get_card(this._provide_player, 0, null, GameConstants.INVALID_SEAT, false);

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 5, GameConstants.INVALID_SEAT);

		exe_gang(table);
	}

	@SuppressWarnings("unused")
	@Override
	protected boolean exe_gang(Table_YongZhou table) {
		int cbCardIndex = table._logic.switch_to_card_index_yongzhou(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.PAO_TYPE_AN_LONG == _type || GameConstants.PAO_TYPE_TI_MINE_LONG == _type || GameConstants.PAO_TYPE_OHTER_PAO == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;
			table._long_count[_seat_index]++;
		} else if (GameConstants.PAO_TYPE_MINE_SAO_LONG == _type || GameConstants.PAO_TYPE_OTHER_SAO_PAO == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;

				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_WEI)) {
					cbWeaveIndex = i;
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		} else if (GameConstants.PAO_TYPE_MINE_PENG_PAO == _type) {
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;

				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;
					table._long_count[_seat_index]++;
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}
		} else if (GameConstants.SAO_TYPE_MINE_SAO == _type) {
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// if (table.has_rule(Constants_ChenZhou.GAME_RULE_SHE_PAO)) {
			// table.cards_has_wei[table._logic.switch_to_card_index_yongzhou(_center_card)]++;
			// //
			// 记录玩家偎牌的牌索引数据
			// }
			table.cards_has_wei[table._logic.switch_to_card_index_yongzhou(_center_card)]++; // 记录玩家偎牌的牌索引数据
		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);

		table._current_player = _seat_index;

		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;

		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

		int cards[] = new int[GameConstants.MAX_YONG_ZHOU_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data_yongzhou(table.GRR._cards_index[_seat_index], cards);

		int hu_xi_count = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		int pai_count = 0;
		for (int i = 0; i < Constants_YongZhou.MAX_CARD_INDEX - 1; i++) {
			if (table.GRR._cards_index[_seat_index][i] < 3)
				pai_count += table.GRR._cards_index[_seat_index][i];
		}

		int action_hu = GameConstants.WIK_NULL;

		if (table._ti_mul_long[_seat_index] > 0 && GameConstants.SAO_TYPE_MINE_SAO == _type) {
			table._ti_mul_long[_seat_index]--;

			int next_player = table.get_banker_next_seat(_seat_index);
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;

			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);

			return true;
		}

		if (_depatch == true && (table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)
				&& (table._is_xiang_gong[_seat_index] == false)) { // 一个提跑并且不是相公，判断是否能胡牌
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();

			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			win_type[_seat_index] = 2;
			if (_seat_index != _provide_player) {
				card_type = GameConstants.HU_CARD_TYPE_FAN_PAI;
				win_type[_seat_index] = 1;
			}

			int hu_xi[] = new int[1];

			boolean can_win = false;
			table.cardWhenWin = _center_card;

			int tmpMagicCount = table.GRR._cards_index[_seat_index][Constants_YongZhou.MAX_CARD_INDEX - 1];
			if (tmpMagicCount > 0) {
				if (_seat_index == _provide_player) {
					can_win = true;
					action_hu = table.analyse_normal(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
							table.GRR._weave_count[_seat_index], _seat_index, _provide_player, 0, chr, card_type, hu_xi, true);
				}
			} else {
				can_win = true;
				action_hu = table.analyse_normal(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index], _seat_index, _provide_player, 0, chr, card_type, hu_xi, true);
			}

			if (action_hu != GameConstants.WIK_NULL) {
				PlayerStatus tempPlayerStatus = table._playerStatus[_seat_index];

				if (_seat_index != _provide_player) {
					tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
					tempPlayerStatus.add_chi_hu(_center_card, _provide_player);
				} else {
					tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					tempPlayerStatus.add_zi_mo(_center_card, _provide_player);
				}

				tempPlayerStatus.add_action(GameConstants.WIK_NULL);
				tempPlayerStatus.add_pass(0, _seat_index);

				if (tempPlayerStatus.has_action()) {
					tempPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(_seat_index, false);
					return true;
				}
			} else {
				chr.set_empty();
			}
		}

		// 思路不一样，先判断胡，再来判断的是否相公
		if ((table._is_xiang_gong[_seat_index] == false) && (table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) { // 如果不是相公，并且提跑只有一个或者是偎牌操作，不是有点重复了吗？？？
			if (pai_count == 0) {
				table._is_xiang_gong[_seat_index] = true;
				table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);

				int pCount = table.getTablePlayerNumber();
				int next_player = table.get_banker_next_seat(_seat_index);

				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
			} else {
				if (table._long_count[_seat_index] <= 1 || GameConstants.SAO_TYPE_MINE_SAO == _type) { // 第一个提偎跑之后有牌可以出，else分支不会走吧？
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				} else { // 有牌可以出，但是是第2个以上的提跑，还是有点重复了吧？？？？
					int pCount = table.getTablePlayerNumber();
					int next_player = table.get_banker_next_seat(_seat_index);
					table._current_player = next_player;
					table._last_player = next_player;
					table._last_card = 0;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
				}
			}
		} else { // 如果是相公，或者提跑大于等于2个，给下家发牌
			int pCount = table.getTablePlayerNumber();

			int next_player = table.get_banker_next_seat(_seat_index);
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;

			if (table._long_count[_seat_index] >= 2) {
				// 如果是重跑 判断下家能否王钓 王闯 王炸 小一
				table.cardWhenWin = 0x01;
				table.is_wang_zha_others_card = true;
				boolean has_hu = table.wang_pai_pai_xing_check(next_player, _seat_index, 0x01, GameConstants.HU_CARD_TYPE_FAN_PAI);
				table.is_wang_zha_others_card = false;

				if (table._is_xiang_gong[next_player] == true)
					has_hu = false;

				if (has_hu) {
					is_wang_zha_others = true;

					table._playerStatus[next_player].add_action(GameConstants.WIK_NULL);
					table._playerStatus[next_player].add_pass(0x01, _seat_index);

					table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(next_player, false);
				} else {
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
				}
			} else {
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.time_for_dispatch_card);
			}
		}

		return true;
	}

	@SuppressWarnings({ "unused", "static-access" })
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

		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);

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
		int cbMaxActionRand = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				}
			}
		}

		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false) {
			table.log_info("优先级最高的人还没操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
			table.operate_player_action(i, true);
		}

		switch (target_action) {
		case GameConstants.WIK_NULL: { // 如果偎提跑之后，有胡不胡
			if (is_wang_zha_others) {
				int next_player = table.get_banker_next_seat(_seat_index);

				table._current_player = next_player;
				table._last_player = next_player;
				table._last_card = 0;

				table._playerStatus[next_player].clean_action();
				table._playerStatus[next_player].clean_status();

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.delay_when_passed);

				return true;
			}

			if ((table._is_xiang_gong[_seat_index] == false) && (table._long_count[_seat_index] == 1 || GameConstants.SAO_TYPE_MINE_SAO == _type)) {
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
				} else {
					table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
					table.operate_player_status();
				}
			} else { // 如果不胡牌，并且是重提重跑或者是相公，给下家发牌
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();

				int next_player = table.get_banker_next_seat(_seat_index);

				table._current_player = next_player;
				table._last_player = next_player;

				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, table.delay_when_passed);
			}

			return true;
		}
		case GameConstants.WIK_CHI_HU:
		case GameConstants.WIK_ZI_MO:
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

			if (target_action == GameConstants.WIK_WANG_ZHA || target_action == GameConstants.WIK_WANG_CHUANG
					|| target_action == GameConstants.WIK_WANG_DIAO) {
				if (table.GRR._left_card_count == 0) {
					table.liu_ju();
					return true;
				}

				changed = true;
				table.changed = true;

				table.operate_player_get_card(target_player, 0, null, GameConstants.INVALID_SEAT, false);

				table._send_card_count++;
				int _send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
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
				operate_card = table._send_card_data;

			table.change_hu_count_and_weave_items(target_action, target_player);

			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;

			table.set_niao_card(target_player, GameConstants.INVALID_VALUE, true);

			table.process_chi_hu_player_operate(target_player, operate_card, true);

			table.process_chi_hu_player_score_phz(target_player, _provide_player, operate_card, true);

			if (!table.GRR._chi_hu_rights[target_player].opr_and(Constants_YongZhou.CHR_ZI_MO).is_empty()) {
				table._player_result.zi_mo_count[target_player]++;
			}

			table._player_result.hu_pai_count[target_player]++;
			table._player_result.ying_xi_count[target_player] += table._hu_xi[target_player];

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
					if (table._logic.is_magic_card(table.GRR._discard_cards[i][j])) {
						// 癞子
						int_array.addItem(table.GRR._discard_cards[i][j] + GameConstants.CARD_ESPECIAL_TYPE_HUN);
					} else {
						int_array.addItem(table.GRR._discard_cards[i][j]);
					}
				}
				tableResponse.addDiscardCards(int_array);
				// 组合扑克
				tableResponse.addWeaveCount(table.GRR._weave_count[i]);
				WeaveItemResponseArrayResponse.Builder weaveItem_array = WeaveItemResponseArrayResponse.newBuilder();
				for (int j = 0; j < GameConstants.MAX_WEAVE_HH; j++) {
					WeaveItemResponse.Builder weaveItem_item = WeaveItemResponse.newBuilder();
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
					weaveItem_item.setProvidePlayer(table.GRR._weave_items[i][j].provide_player);
					weaveItem_item.setPublicCard(table.GRR._weave_items[i][j].public_card);
					weaveItem_item.setWeaveKind(table.GRR._weave_items[i][j].weave_kind);
					weaveItem_item.setHuXi(table.GRR._weave_items[i][j].hu_xi);
					weaveItem_array.addWeaveItem(weaveItem_item);
				}
				tableResponse.addWeaveItemArray(weaveItem_array);

				//
				tableResponse.addWinnerOrder(0);
				tableResponse.addCardCount(table._logic.get_card_count_by_index(table.GRR._cards_index[i]));

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

		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, seat_index);

		if (table._playerStatus[seat_index].has_action() && table._playerStatus[seat_index].is_respone() == false) {
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
