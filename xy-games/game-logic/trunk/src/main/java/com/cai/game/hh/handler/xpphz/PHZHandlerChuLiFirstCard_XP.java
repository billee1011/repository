/**
 * 
 */
package com.cai.game.hh.handler.xpphz;

/**
 * @author hexinqi
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.MsgConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

import protobuf.clazz.Protocol.Int32ArrayResponse;
import protobuf.clazz.Protocol.RoomResponse;
import protobuf.clazz.Protocol.TableResponse;
import protobuf.clazz.Protocol.WeaveItemResponse;
import protobuf.clazz.Protocol.WeaveItemResponseArrayResponse;

public class PHZHandlerChuLiFirstCard_XP extends HHHandlerDispatchCard<HHTable_XP> {

	@Override
	public void exe(HHTable_XP table) {
		XPPHZUtils.cleanPlayerStatus(table);
		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		if (XPPHZUtils.endHuangZhuang(table, false)) {
			return;
		}

		ChiHuRight chrs[] = new ChiHuRight[table.getTablePlayerNumber()];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chrs[i] = table.GRR._chi_hu_rights[i];
			chrs[i].set_empty();
		}
		boolean haveTianHu = false;
		int hu_xi_chi[] = new int[1];
		hu_xi_chi[0] = 0;
		int i = table._cur_banker;
		PlayerStatus playerStatus = table._playerStatus[i];
		int cardType = GameConstants.HU_CARD_TYPE_TIAN_HU;
		int hu_xi = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _seat_index,
				table._send_card_data, chrs[i], cardType, hu_xi_chi, true);// 自
		if (hu_xi != GameConstants.WIK_NULL) {
			playerStatus.add_action(GameConstants.WIK_ZI_MO);
			playerStatus.add_zi_mo(table._send_card_data, i);
			playerStatus.add_action(GameConstants.WIK_NULL);
			playerStatus.add_pass(_send_card_data, _seat_index);
			haveTianHu = true;
		}

		if (haveTianHu) {
			// 等待别人操作这张牌
			boolean needAction = true;
			for (int z = 0; z < table.getTablePlayerNumber(); z++) {
				if (table._playerStatus[i].has_action()) {
					if( table.has_rule(GameConstants.GAME_RULE_XP_ZI_MO_QIANG_ZHI_HU) && table._playerStatus[z].has_zi_mo() && z == _seat_index){
						needAction  = false;
					}
				}

			}
			if(needAction){
				for (int z = 0; z < table.getTablePlayerNumber(); z++) {
					if (table._playerStatus[z].has_action()) {
						if( table.has_rule(GameConstants.GAME_RULE_XP_ZI_MO_QIANG_ZHI_HU) && table._playerStatus[z].has_zi_mo() && z == _seat_index){
							GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data ), GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS,TimeUnit.MILLISECONDS);
						}
						table._playerStatus[z].set_status(GameConstants.Player_Status_OPR_CARD); //
						// 操作状态
						table.operate_player_action(z, false);
					}
				}
			}
			return;
		}

		bankerOperaterCard(table);
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HHTable_XP table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logicXP.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if (table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD) {
			table.log_error("状态不对不能出牌");
			return false;
		}
		if ((table.GRR._cards_index[_seat_index][table._logicXP.switch_to_card_index(table._logicXP.toLowCard(card))]
				+ table.GRR._cards_index[_seat_index][table._logicXP.switch_to_card_index(table._logicXP.toUpCard(card))]) >= 3) {
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			// 显示出牌
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			table.log_error(_seat_index + "出牌出错 HHHandlerDispatchCard " + _seat_index);
			return true;
		}
		// 删除扑克
		if (table._logicXP.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌
		table.exe_out_card(_seat_index, card, GameConstants.WIK_NULL);

		return true;
	}

	private boolean judgeSanTiWuKan(HHTable_XP table) {
		int send_index = table._logicXP.switch_to_card_index(table._send_card_data);
		boolean is_fa_pai = false;
		int loop = 0;

		// 三提五坎天胡 直接天胡
		while (loop < table.getTablePlayerNumber()) {
			int i = (table._current_player + loop) % table.getTablePlayerNumber();
			loop++;
			int ti_count = 0;
			int sao_count = 0;
			for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
				if (i == table._current_player) {
					if (j == send_index) {
						table.GRR._cards_index[i][j]++;
					} else if (j + 20 == send_index) {
						table.GRR._cards_index[i][j + 20]++;
					}
				}
				if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 4) {
					ti_count++;
					if ((i == table._current_player) && (j == send_index % 20)) {
						is_fa_pai = true;
					}
				} else if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 3) {
					sao_count++;
					if ((i == table._current_player) && (j == send_index % 20)) {
						is_fa_pai = true;
					}
				}
				if (i == table._current_player) {
					if (j == send_index) {
						table.GRR._cards_index[i][j]--;
					} else if (j + 20 == send_index) {
						table.GRR._cards_index[i][j + 20]--;
					}
				}
			}
			if ((ti_count >= 3) || (sao_count >= 5) || ((ti_count + sao_count) >= 5)) {
				ChiHuRight chr = table.GRR._chi_hu_rights[i];
				int card_type = Constants_XPPHZ.CHR_ZI_MO;
				chr.set_empty();
				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					if (i == table._current_player) {
						if (j == send_index) {
							table.GRR._cards_index[i][j]++;
						} else if (j + 20 == send_index) {
							table.GRR._cards_index[i][j + 20]++;
						}
					}
					if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 4) {
						ti_count++;
					} else if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 3) {
						sao_count++;
					}
					if (i == table._current_player) {
						if (j == send_index) {
							table.GRR._cards_index[i][j]--;
						} else if (j + 20 == send_index) {
							table.GRR._cards_index[i][j + 20]--;
						}
					}
				}
				int weave_count = 0;
				for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
					if (i == table._current_player) {
						if (j == send_index) {
							table.GRR._cards_index[i][j]++;
						} else if (j + 20 == send_index) {
							table.GRR._cards_index[i][j + 20]++;
						}
					}
					if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 4) {
						table._hu_weave_items[i][weave_count].center_card = table._logicXP.switch_to_card_data(j);
						table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
						table._hu_weave_items[i][weave_count].hu_xi = table._logicXP.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
						for (int k = 0; k < 4; k++) {
							table._hu_weave_items[i][weave_count].weave_card[k] = table._logicXP.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_card[++k] = table._logicXP.switch_to_card_data(j + 20);
						}

						weave_count++;
					} else if ((table.GRR._cards_index[i][j] + table.GRR._cards_index[i][j + 20]) == 3) {
						table._hu_weave_items[i][weave_count].center_card = table._logicXP.switch_to_card_data(j);
						table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_KAN;
						table._hu_weave_items[i][weave_count].hu_xi = table._logicXP.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);

						int k = 0;
						if (table.GRR._cards_index[i][j] == 1) {
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j + 20);
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j + 20);
						} else {
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_card[k++] = table._logicXP.switch_to_card_data(j + 20);
						}
						weave_count++;
					}
					if (i == table._current_player) {
						if (j == send_index) {
							table.GRR._cards_index[i][j]--;
						} else if (j + 20 == send_index) {
							table.GRR._cards_index[i][j + 20]--;
						}
					}
				}
				int hu_card = table._hu_weave_items[i][weave_count - 1].center_card;
				table._hu_weave_count[i] = weave_count;
				if (card_type == Constants_XPPHZ.CHR_ZI_MO) {
					chr.opr_or(Constants_XPPHZ.CHR_TIAN_HU);
				}
				PlayerStatus curPlayerStatus = table._playerStatus[i];
				curPlayerStatus.reset();
				if ((i == table._current_player) && (is_fa_pai == true)) { // 发送数据
																			// 只有自己才有数值
					//WalkerGeek 本代码有个bug，但是不知道是做什么操作的，产品要求不改
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT, false);
				}
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(hu_card, i);
				//table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_HU, 1, table.GRR._chi_hu_rights[i].type_list, 1, GameConstants.INVALID_SEAT);
				table.sanTi = true;
				XPPHZUtils.hu_pai(table, i, i);
				return true;
			}
		}
		return false;
	}

	private void bankerOperaterCard(HHTable_XP table) {
		// 刷新手牌包括组合
		table.GRR._cards_index[_seat_index][table._logicXP.switch_to_card_index(table._send_card_data)]++;
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		if (judgeSanTiWuKan(table)) {
			return;
		}

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if ((table.GRR._cards_index[_seat_index][i] + table.GRR._cards_index[_seat_index][i + 20]) == 4) {
				an_long_Index[an_long_count++] = i;
			}
		}
		if (an_long_count > 0) {
			int _action = GameConstants.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logicXP.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				for (int k = 0; k < 4; k++) {
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[k] = table._logicXP.switch_to_card_data(an_long_Index[i]);
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[++k] = table._logicXP.switch_to_card_data(an_long_Index[i] + 20);
				}
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logicXP.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
				table.GRR._cards_index[_seat_index][an_long_Index[i] + 20] = 0;

				table.GRR._card_count[_seat_index] = table._logicXP.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

			}

			// 刷新手牌包括组合
			hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		}

		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
		}

		// 加到手牌
		hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);

		if (table.gu[_seat_index]) {
			table.exe_dispatch_card((_seat_index + 1) % table.getTablePlayerNumber(), GameConstants.WIK_NULL, 1000);
		} else {
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}
		return;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(HHTable_XP table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			return false;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
		if (operate_code == GameConstants.WIK_NULL) {
			if (playerStatus.has_zi_mo() == true) {
				int index = -1;
				for (int i = 0; i < table._guo_hu_pai_count[seat_index]; i++) {
					if (table._guo_hu_pai_cards[seat_index][i] == table._logicXP.toLowCard(operate_card)) {
						index = i;
					}
				}
				if (index == -1) {
					index = table._guo_hu_pai_count[seat_index]++;
				}
				table._guo_hu_pai_cards[seat_index][index] = table._logicXP.toLowCard(operate_card);

				int all_hu_xi = 0;
				for (int i = 0; i < table._hu_weave_count[seat_index]; i++) {
					all_hu_xi += table._hu_weave_items[seat_index][i].hu_xi;
				}
				table._guo_hu_xi[seat_index][index] = all_hu_xi;
			}
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		if (operate_code == GameConstants.WIK_NULL) {
			boolean flag = false;
			for (int i = 0; i < playerStatus._action_count; i++) {

				switch (playerStatus._action[i]) {
				case GameConstants.WIK_LEFT:
				case GameConstants.WIK_CENTER:
				case GameConstants.WIK_RIGHT:
				case GameConstants.WIK_XXD:
				case GameConstants.WIK_DDX:
				case GameConstants.WIK_EQS:
					if (flag == false) {
						table._cannot_chi[seat_index][table._cannot_chi_count[seat_index]++] = table._logicXP.toLowCard(operate_card);
						playerStatus.set_exe_pass(true);
						flag = true;
					}
					break;
				case GameConstants.WIK_PENG: {
					table._cannot_peng[seat_index][table._cannot_peng_count[seat_index]++] = table._logicXP.toLowCard(operate_card);
					playerStatus.set_exe_pass(true);
				}
					break;
				}
			}

		}
		// 吃操作后，是否有落
		switch (operate_code) {
		case GameConstants.WIK_LEFT:
		case GameConstants.WIK_CENTER:
		case GameConstants.WIK_RIGHT:
		case GameConstants.WIK_XXD:
		case GameConstants.WIK_DDX:
		case GameConstants.WIK_EQS:
			if (luoCode != -1) {
				playerStatus.set_lou_pai_kind(luoCode);
			}
		}

		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_p = 0;
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			if (i == target_player) {
				target_p = table.getTablePlayerNumber() - p;
			}
		}
		int cbActionRank[] = new int[3];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logicXP.get_action_rank(table._playerStatus[i].get_perform()) + table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logicXP.get_action_list_rank(table._playerStatus[i]._action_count, table._playerStatus[i]._action)
							+ table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logicXP.get_action_rank(table._playerStatus[target_player].get_perform()) + target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logicXP.get_action_list_rank(table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action) + target_p;
				}

				// 优先级别
				// 动作判断 优先级最高的人和动作
				if (cbUserActionRank > cbTargetActionRank) {
					target_player = i;// 最高级别人
					target_action = table._playerStatus[i].get_perform();
					target_p = table.getTablePlayerNumber() - p;
				}
			}
		}

		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false) {
			return true;
		}

		// 判断可不可以吃的上家用户
		int last_player = (target_player + 3 + 1) % 3;
		boolean flag = false;
		for (int j = 0; j < table._playerStatus[last_player]._action_count; j++) {

			switch (table._playerStatus[last_player]._action[j]) {
			case GameConstants.WIK_LEFT:
			case GameConstants.WIK_CENTER:
			case GameConstants.WIK_RIGHT:
			case GameConstants.WIK_XXD:
			case GameConstants.WIK_DDX:
			case GameConstants.WIK_EQS:
				if (target_action == GameConstants.WIK_NULL) {
					continue;
				}
				if (flag == false) {
					if (table._playerStatus[last_player].get_exe_pass() == true) {
						table._cannot_chi[last_player][table._cannot_chi_count[last_player]--] = 0;
						flag = true;
						table._playerStatus[last_player].set_exe_pass(false);
					}
				}
				break;
			}
		}
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT | GameConstants.WIK_DDX | GameConstants.WIK_XXD
				| GameConstants.WIK_EQS;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			boolean flag_temp = false;

			if (table._playerStatus[i].has_action()) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {

					switch (table._playerStatus[i]._action[j]) {
					case GameConstants.WIK_LEFT:
					case GameConstants.WIK_CENTER:
					case GameConstants.WIK_RIGHT:
					case GameConstants.WIK_XXD:
					case GameConstants.WIK_DDX:
					case GameConstants.WIK_EQS:
						if (!((target_action == GameConstants.WIK_PENG) || (target_action == GameConstants.WIK_ZI_MO))) {
							continue;
						}
						if (flag_temp == false) {
							if (table._playerStatus[i].get_exe_pass() == true) {
								table._cannot_chi[i][table._cannot_chi_count[i]--] = 0;
								flag_temp = true;
							}
						}
						break;
					case GameConstants.WIK_PENG:
						if (!((target_action == GameConstants.WIK_NULL) || (target_action & eat_type) != GameConstants.WIK_NULL)) {
							continue;
						}
						if (table._playerStatus[i].get_exe_pass() == false) {
							table._cannot_peng[i][table._cannot_peng_count[i]++] = table._logicXP.toLowCard(operate_card);
						}
						break;
					}
				}
			}

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL: {
			if (seat_index == _seat_index) {
				bankerOperaterCard(table);
				return true;
			}
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			if (table._long_count[_seat_index] > 0) {
				int _action = GameConstants.WIK_AN_LONG;
				// 效果
				table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1, GameConstants.INVALID_SEAT);

				// 刷新手牌包括组合
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

			}
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				// 要出牌，但是没有牌出设置成相公 下家用户发牌
				int pai_count = 0;
				for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
					if (table.GRR._cards_index[_seat_index][i] < 3)
						pai_count += table.GRR._cards_index[_seat_index][i];
				}
				if (pai_count == 0) {
					table._is_xiang_gong[_seat_index] = true;
					table.operate_player_xiang_gong_flag(_seat_index, table._is_xiang_gong[_seat_index]);
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;

					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					return true;
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT, false);
				int cards[] = new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logicXP.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.operate_player_status();
			}
			return true;
		}

		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);
			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			if (table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI) == false) {
				table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT, false);
			}
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}
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

	@Override
	public boolean handler_player_be_in_room(HHTable_XP table, int seat_index) {
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
		// 历史记录
		tableResponse.setOutCardData(0);
		tableResponse.setOutCardPlayer(0);

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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
				for (int k = 0; k < 4; k++) {
					if (table.GRR._weave_items[i][j].weave_card[k] > 0) {
						weaveItem_item.addWeaveCard(table.GRR._weave_items[i][j].weave_card[k]);
					}
				}
				if (seat_index != i) {
					if ((table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_TI_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG
							|| table.GRR._weave_items[i][j].weave_kind == GameConstants.WIK_AN_LONG_LIANG) && table.GRR._weave_items[i][j].public_card == 0) {
						weaveItem_item.setCenterCard(0);
					} else {
						if (table.is_mj_type(GameConstants.GAME_TYPE_PHZ_XT) && table.has_rule(GameConstants.GAME_RULE_DI_AN_WEI)
								&& table._xt_display_an_long[i] == true) {
							weaveItem_item.setCenterCard(0);
						} else {
							weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
						}
					}
				} else {
					weaveItem_item.setCenterCard(table.GRR._weave_items[i][j].center_card);
				}
				weaveItem_array.addWeaveItem(weaveItem_item);
			}
			tableResponse.addWeaveItemArray(weaveItem_array);
			tableResponse.addWinnerOrder(0);
			tableResponse.addHuXi(table._hu_xi[i]);
			// 牌
			if (i == _seat_index) {
				tableResponse.addCardCount(table._logicXP.get_card_count_by_index(table.GRR._cards_index[i]) - 1);
			} else {
				tableResponse.addCardCount(table._logicXP.get_card_count_by_index(table.GRR._cards_index[i]));
			}
		}
		// 数据
		tableResponse.setSendCardData(0);
		int hand_cards[] = new int[GameConstants.MAX_HH_COUNT];
		table._logicXP.switch_to_cards_data(table.GRR._cards_index[seat_index], hand_cards);
		boolean flag = false;
		if (table.getTablePlayerNumber() == GameConstants.GAME_PLAYER_HH) {
			if (table.GRR._left_card_count == 19) {
				if (seat_index == _seat_index) {
					table._logicXP.remove_card_by_data(hand_cards, _send_card_data);
				}
				if (table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)) {
					if (_send_card_data != 0) {
						table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);
					}
				} else {
					if (seat_index == _seat_index) {
						if (_send_card_data != 0) {
							table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);
						}
					} else {
						if (_send_card_data != 0) {
							table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, true);
						}
					}
				}
				flag = true;
			}
		}

		for (int i = 0; i < GameConstants.MAX_HH_COUNT; i++) {
			tableResponse.addCardsData(hand_cards[i]);
		}
		roomResponse.setTable(tableResponse);
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			roomResponse.addActions(table.chong[i]);
			roomResponse.addDouliuzi(table.gu[i] ? 1 : 0);
		}
		table.send_response_to_player(seat_index, roomResponse);
		// 摸牌
		if ((_send_card_data != 0) && (flag == false)) {
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, seat_index, false);
		}

		if (table._playerStatus[seat_index].has_action() && (table._playerStatus[seat_index].is_respone() == false)) {
			table.operate_player_action(seat_index, false);
		}
		if (table._is_xiang_gong[seat_index] == true) {
			table.operate_player_xiang_gong_flag(seat_index, table._is_xiang_gong[seat_index]);
		}
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}

		return true;
	}
}
