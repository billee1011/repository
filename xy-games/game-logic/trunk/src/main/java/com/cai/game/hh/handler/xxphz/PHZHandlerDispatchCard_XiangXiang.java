package com.cai.game.hh.handler.xxphz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.mj.GameConstants_XiangXiang;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class PHZHandlerDispatchCard_XiangXiang extends HHHandlerDispatchCard<XiangXiangHHTable> {

	@Override
	public void exe(XiangXiangHHTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants_XiangXiang.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants_XiangXiang.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants_XiangXiang.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
						table.GRR._weave_count[i], GameConstants_XiangXiang.INVALID_SEAT);

			}
			table.GRR._game_score[table._cur_banker] -= 10;
			table._hu_xi[table._cur_banker] -= 10;
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants_XiangXiang.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants_XiangXiang.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		int an_long_Index[] = new int[5];
		int an_long_count = 0;
		//// 玩家出牌 响应判断,是否有提 暗龙

		for (int i = 0; i < GameConstants_XiangXiang.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[_seat_index][i] == 4) {
				an_long_Index[an_long_count++] = i;

			}
		}
		if (an_long_count > 0) {
			int _action = GameConstants_XiangXiang.WIK_AN_LONG;
			// 效果
			table.operate_effect_action(_seat_index, GameConstants_XiangXiang.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
					GameConstants_XiangXiang.INVALID_SEAT);

			for (int i = 0; i < an_long_count; i++) {
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants_XiangXiang.WIK_TI_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic
						.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;

				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);

			}
			// 刷新手牌包括组合
			int cards[] = new int[GameConstants_XiangXiang.MAX_HH_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);

		}

		if (an_long_count >= 2) {
			table._ti_two_long[_seat_index] = true;
		}
		table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(table._playerStatus[_seat_index]._hu_cards,
				table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _seat_index,
				_seat_index);

		int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
		int ting_count = table._playerStatus[_seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
		} else {
			ting_cards[0] = 0;
			table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
		}

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		if (table.DEBUG_CARDS_MODE) {
			_send_card_data = 0x15;
		}
		// _send_card_data = 0x09;

		--table.GRR._left_card_count;
		table._last_card = _send_card_data;
		// table.log_info(_seat_index + "牌" + _send_card_data);
		// 用户是否可以提扫
		int card_type = GameConstants_XiangXiang.HU_CARD_TYPE_ZIMO;
		int ti_sao = table.estimate_player_ti_wei_respond_phz(_seat_index, _send_card_data);
		if (ti_sao != GameConstants_XiangXiang.WIK_NULL) {
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants_XiangXiang.INVALID_SEAT, false);
			return;
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断

		ChiHuRight chr[] = new ChiHuRight[3];
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			chr[i] = table.GRR._chi_hu_rights[i];
			chr[i].set_empty();
		}
		int bHupai = 0;

		int action_hu[] = new int[3];
		int action_pao[] = new int[3];
		int pao_type[][] = new int[3][1];

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			int hu_xi_chi[] = new int[1];
			hu_xi_chi[0] = 0;
			PlayerStatus tempPlayerStatus = table._playerStatus[i];
			tempPlayerStatus.reset();
			action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i], table.GRR._weave_count[i], i, _seat_index,
					_send_card_data, chr[i], card_type, hu_xi_chi, true);// 自摸
			action_pao[i] = table.estimate_player_respond_phz(i, _seat_index, _send_card_data, pao_type[i], true);
			if (table._is_xiang_gong[i] == true)
				action_hu[i] = GameConstants_XiangXiang.WIK_NULL;
			if (action_hu[i] != GameConstants_XiangXiang.WIK_NULL) {
				tempPlayerStatus.add_action(GameConstants_XiangXiang.WIK_ZI_MO);
				tempPlayerStatus.add_zi_mo(_send_card_data, i);
				if (action_pao[i] != GameConstants_XiangXiang.WIK_PAO) {
					tempPlayerStatus.add_action(GameConstants_XiangXiang.WIK_NULL);
					tempPlayerStatus.add_pass(_send_card_data, _seat_index);
				} else {
					tempPlayerStatus.add_action(GameConstants_XiangXiang.WIK_PAO);
					tempPlayerStatus.add_pao(_send_card_data, _seat_index);
				}
				if (bHupai == 0)
					table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants_XiangXiang.INVALID_SEAT, false);
				ti_sao = GameConstants_XiangXiang.WIK_ZI_MO;
				bHupai = 1;

			} else {
				chr[i].set_empty();
			}
		}

		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((action_pao[i] != GameConstants_XiangXiang.WIK_NULL) && (bHupai == 0)) {
				ti_sao = GameConstants_XiangXiang.WIK_PAO;
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants_XiangXiang.INVALID_SEAT, false);
				table.exe_gang(i, _seat_index, _send_card_data, action_pao[i], pao_type[i][0], true, true, false, 1000);
				return;
			} else if (action_pao[i] != GameConstants_XiangXiang.WIK_NULL) {
				ti_sao = GameConstants_XiangXiang.WIK_PAO;
			}
		}

		// 玩家出牌 响应判断,是否有吃,碰,胡 swe
		table.log_info(_seat_index + "发牌 " + _send_card_data + "ti_sao = " + ti_sao);
		boolean bAroseAction = false;

		if (ti_sao != GameConstants_XiangXiang.WIK_PAO) {
			bAroseAction = table.estimate_player_out_card_respond_PHZ_syzp(_seat_index, _send_card_data, true);
			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants_XiangXiang.INVALID_SEAT, false);
		}

		if ((bAroseAction == false) && (ti_sao == GameConstants_XiangXiang.WIK_NULL)) {

			table.operate_player_action(_seat_index, true);

		} else {
			// 等待别人操作这张牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				curPlayerStatus = table._playerStatus[i];
				if (table._playerStatus[i].has_action()) {
					table._playerStatus[i].set_status(GameConstants_XiangXiang.Player_Status_OPR_CARD);//
					// 操作状态
					table.operate_player_action(i, false);
				}

			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants_XiangXiang.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants_XiangXiang.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
			table.log_info(_seat_index + "操作状态" + bAroseAction);
		} else {
			if (table.isTrutess(_seat_index)) {

				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants_XiangXiang.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_XiangXiang.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else if (ti_sao == GameConstants_XiangXiang.WIK_NULL) {

				if (bAroseAction == false) {

					table.operate_player_get_card(_seat_index, 0, null, GameConstants_XiangXiang.INVALID_SEAT, false);
					// table.operate_remove_discard(table._current_player,
					// table.GRR._discard_count[table._current_player]);
					// 没有人要就加入到牌堆
					int discard_time = 2000;
					int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
					SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId).get(1104);
					if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
						discard_time = sysParamModel1104.getVal4();
					}

					if (table._last_card != 0)
						table.exe_add_discard(_seat_index, 1, new int[] { table._last_card }, true, discard_time);

					// 显示出牌
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 过张的牌都不可以
					table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
					table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = _send_card_data;

					table._current_player = next_player;
					_seat_index = next_player;
					table.log_info(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户");
					// 延时5秒发牌
					int dispatch_time = 3000;
					if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
						dispatch_time = sysParamModel1104.getVal5();
					}
					table.exe_dispatch_card(next_player, GameConstants_XiangXiang.WIK_NULL, dispatch_time);
					table._last_card = _send_card_data;
					table._last_player = table._current_player;
					table.log_info(next_player + "发牌" + bAroseAction);

				}
			}
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
	public boolean handler_operate_card(XiangXiangHHTable table, int seat_index, int operate_code, int operate_card, int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.log_info(_seat_index + "  " + table._current_player + "  " + "下次 出牌用户" + seat_index + "操作用户");
		// 效验操作
		if ((operate_code != GameConstants_XiangXiang.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if (operate_code == GameConstants_XiangXiang.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants_XiangXiang.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { GameConstants_XiangXiang.WIK_NULL }, 1);
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		// if (seat_index != _seat_index) {
		// table.log_info("DispatchCard 不是当前玩家操作");
		// return false;
		// }
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		if (operate_card != this._send_card_data) {
			table.log_player_error(seat_index, "DispatchCard 操作牌，与当前牌不一样");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);

		playerStatus.clean_status();

		// 吃操作后，是否有落
		switch (operate_code) {
		case GameConstants_XiangXiang.WIK_LEFT:
		case GameConstants_XiangXiang.WIK_CENTER:
		case GameConstants_XiangXiang.WIK_RIGHT:
		case GameConstants_XiangXiang.WIK_XXD:
		case GameConstants_XiangXiang.WIK_DDX:
		case GameConstants_XiangXiang.WIK_EQS:
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
		int cbActionRank[] = new int[3];
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
			table.log_info("最用户操作");
			return true;
		}

		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
		// 判断可不可以吃的上家用户
		int last_player = (target_player + 3 + 1) % 3;
		boolean flag = false;
		int eat_type = GameConstants_XiangXiang.WIK_LEFT | GameConstants_XiangXiang.WIK_CENTER | GameConstants_XiangXiang.WIK_RIGHT
				| GameConstants_XiangXiang.WIK_DDX | GameConstants_XiangXiang.WIK_XXD | GameConstants_XiangXiang.WIK_EQS;

		if (target_action == GameConstants.WIK_NULL) {
			// 显示出牌
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			// 过张的牌都不可以吃
			table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
			table._cannot_chi[next_player][table._cannot_chi_count[next_player]++] = _send_card_data;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][table._cannot_peng_count[i]++] = _send_card_data;
					}
				}
			}
		} else if ((target_action & eat_type) != GameConstants_XiangXiang.WIK_NULL) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				for (int j = 0; j < table._playerStatus[i]._action_count; j++) {
					if (table._playerStatus[i]._action[j] == GameConstants.WIK_PENG) {
						table._cannot_peng[i][table._cannot_peng_count[i]++] = _send_card_data;
					}
				}
			}
			if (_seat_index != target_player)
				table._cannot_chi[_seat_index][table._cannot_chi_count[_seat_index]++] = _send_card_data;
		}
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {

			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		// 执行动作
		switch (target_action) {
		case GameConstants_XiangXiang.WIK_NULL: {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				// 显示胡牌
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int cards[] = new int[GameConstants_XiangXiang.MAX_HH_COUNT];
					int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

					table.operate_show_card(i, GameConstants_XiangXiang.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
							table.GRR._weave_count[i], GameConstants_XiangXiang.INVALID_SEAT);

				}
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants_XiangXiang.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					int pao_type[] = new int[1];
					int action = table.estimate_player_respond_phz(i, _seat_index, _send_card_data, pao_type, true);
					if (action != GameConstants_XiangXiang.WIK_NULL) {
						table.exe_gang(i, _seat_index, _send_card_data, action, pao_type[0], true, true, false, 1000);
						return true;
					}
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants_XiangXiang.INVALID_SEAT, false);
				// 要出牌，但是没有牌出设置成相公 下家用户发牌
				int pai_count = 0;
				for (int i = 0; i < GameConstants_XiangXiang.MAX_HH_INDEX; i++) {
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

					table.exe_dispatch_card(next_player, GameConstants_XiangXiang.WIK_NULL, 1000);
					table.log_info(next_player
							+ "可以胡，而不胡的情况                                                                                                                                                                                                                                                       "
							+ _seat_index);
					return true;
				}
				int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
				int ting_count = table._playerStatus[_seat_index]._hu_card_count;

				if (ting_count > 0) {
					table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
				} else {
					ting_cards[0] = 0;
					table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
				}
				table.exe_add_discard(_seat_index, 1, new int[] { _send_card_data }, true, 0);
				int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();

				table._current_player = next_player;
				_seat_index = next_player;
				table._last_player = next_player;
				// 没有人要就加入到牌堆
				table.exe_dispatch_card(next_player, GameConstants_XiangXiang.WIK_NULL, 0);
				table._last_card = _send_card_data;
				table.log_info(next_player + "发牌" + _seat_index + "  " + next_player);
			}
			return true;

		}
		case GameConstants_XiangXiang.WIK_LEFT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card + 1, target_card + 2 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(target_player, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][0]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_RIGHT: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card - 1, target_card - 2 };

			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}

			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][2]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_CENTER: // 上牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card - 1, target_card + 1 };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][1]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_XXD:// 吃小
		{
			// 删除扑克
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
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][4]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_DDX:// 吃大
		{
			// 删除扑克
			// 删除扑克
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
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][5]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_EQS:// 吃二七十
		{
			// 删除扑克
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
				table.log_player_error(seat_index, "吃牌删除出错");
				return false;
			}
			table.add_lou_weave(target_lou_code, target_player, target_card, _seat_index, table._lou_weave_item[target_player][3]);

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_PENG: // 碰牌操作
		{
			// 删除扑克
			int cbRemoveCard[] = new int[] { target_card, target_card };
			if (!table._logic.remove_cards_by_index(table.GRR._cards_index[target_player], cbRemoveCard, 2)) {
				table.log_player_error(seat_index, "碰牌删除出错");
				return false;
			}

			table.exe_chi_peng(target_player, _seat_index, target_action, target_card, GameConstants_XiangXiang.CHI_PENG_TYPE_DISPATCH,
					target_lou_code);
			return true;
		}
		case GameConstants_XiangXiang.WIK_PAO: // 杠牌操作
		{
			int pao_type[] = new int[1];
			int action = table.estimate_player_respond_phz_chd(target_player, _seat_index, _send_card_data, pao_type, true);
			if (action != GameConstants_XiangXiang.WIK_NULL) {
				table.exe_gang(target_player, _seat_index, _send_card_data, action, pao_type[0], true, true, false, 1000);
			}
			return true;

		}

		case GameConstants_XiangXiang.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;
			// if
			// (table.has_rule(GameConstants_XiangXiang.GAME_RULE_LIXIANG_FLS_ZHUANG))
			// {// 轮装
			// if (table.GRR._banker_player == target_player) {
			// table._banker_select = target_player;
			// } else {
			// table._banker_select = (table.GRR._banker_player +
			// table.getTablePlayerNumber() + 1)
			// % table.getTablePlayerNumber();
			// }
			// }

			if (table.is_first_mo[target_player]) {
				table.GRR._chi_hu_rights[target_player].opr_or(GameConstants_XiangXiang.CHR_DI_HU_2);
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

			int delay = GameConstants_XiangXiang.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants_XiangXiang.Game_End_NORMAL), delay,
					TimeUnit.SECONDS);

			return true;
		}

		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(XiangXiangHHTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
