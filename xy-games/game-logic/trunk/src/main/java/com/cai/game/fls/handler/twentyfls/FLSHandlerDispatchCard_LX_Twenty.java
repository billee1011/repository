package com.cai.game.fls.handler.twentyfls;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.ThreadUtil;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.fls.FLSTable;
import com.cai.game.fls.handler.FLSHandlerDispatchCard;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class FLSHandlerDispatchCard_LX_Twenty extends FLSHandlerDispatchCard {

	@Override
	public void exe(FLSTable table) {

		int cbCardCount = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		if (table.send_count_when_started != 1 && (cbCardCount - 1) % 3 != 0) {
			table.log_error("出现多牌cbCardCount==" + cbCardCount + "and " + ThreadUtil.getStack());
			return;
		}

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			// table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (table.DEBUG_CARDS_MODE) {
			// table.GRR._left_card_count=2;
		}

		// // 海底
		int left = 0;
		// int left = table.getTablePlayerNumber() == GameConstants.GAME_PLAYER
		// ? 4 : 3;
		// if (table.GRR._left_card_count <= left) {
		// // 最后四张牌。看谁能自摸。不用打
		// table.operate_effect_action(GameConstants.INVALID_SEAT,
		// GameConstants.Effect_Action_Other, 1,
		// new long[] { GameConstants.EFFECT_LAST_FOUR_CARD }, 1,
		// GameConstants.INVALID_SEAT,0,true);
		// // 延迟3秒
		// table.exe_dispatch_last_cardtwenty(_seat_index,
		// GameConstants.WIK_NULL, 1000);
		// return;
		// }

		table._current_player = _seat_index;// 轮到操作的人是自己
		table._provide_player = _seat_index;

		if (table.send_count_when_started == 0) {
			// 从牌堆拿出一张牌
			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;

			table.send_count_when_started++;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x63;
				// table.GRR._left_card_count=2;
			}

			table._send_card_data = _send_card_data;

			// 加到手牌
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

			table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT);

			if (table.send_count_when_started == 1) {
				boolean has_xiao_hu = false;

				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					if (table.getTablePlayerNumber() == 4) {
						if (table.isZhuangDui(i))
							continue;
					}

					PlayerStatus playerStatus = table._playerStatus[i];

					int action = table.analyse_chi_hu_card_xiaohu(table.GRR._cards_index[i], table.GRR._start_hu_right[i], i);

					if (action != GameConstants.WIK_NULL) {
						playerStatus.add_action(GameConstants.WIK_XIAO_HU);
						has_xiao_hu = true;
					} else {
						table.GRR._start_hu_right[i].set_empty();
					}
				}

				if (has_xiao_hu) {
					table.exe_xiao_hu();
					return;
				}
			}
		} else if (table.dispatch_after_xiao_hu == false) {
			// 从牌堆拿出一张牌
			table._send_card_count++;
			_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
			--table.GRR._left_card_count;

			table.send_count_when_started++;

			if (table.DEBUG_CARDS_MODE) {
				_send_card_data = 0x63;
				// table.GRR._left_card_count=2;
			}

			table._send_card_data = _send_card_data;

			// 加到手牌
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

			table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT);
		}

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		}

		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]--;
		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], table._send_card_data, chr, card_type, _seat_index);// 自摸
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;

		if (table.dispatch_after_xiao_hu)
			table.dispatch_after_xiao_hu = false;

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(table._send_card_data, _seat_index);
		} else {
			chr.set_empty();
		}

		cbCardCount = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		if ((cbCardCount - 2) % 3 != 0) {
			table.log_error("出现多牌cbCardCount==" + cbCardCount + "and " + ThreadUtil.getStack());
		}

		if (curPlayerStatus.has_zi_mo() && table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_JIANPAOHU)) {
			table.exe_jian_pao_hu(_seat_index, GameConstants.WIK_ZI_MO, table._send_card_data);
			return;
		}

		if (table.isTrutess(_seat_index) && curPlayerStatus.has_zi_mo()) {
			table.operate_player_action(_seat_index, false);
			table.exe_jian_pao_hu(_seat_index, GameConstants.WIK_ZI_MO, table._send_card_data);
			return;
		}

		// 设置变量
		table._provide_card = table._send_card_data;// 提供的牌

		m_gangCardResult.cbCardCount = 0;

		// 长沙麻将
		if (table.GRR._left_card_count > 0 && curPlayerStatus.lock_huan_zhang() == false) {
			// 看手上所有的牌,有没有补张或杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
					table.GRR._weave_count[_seat_index], m_gangCardResult, false);

			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				// if (!curPlayerStatus.lock_huan_zhang()) {
				// curPlayerStatus.add_action(GameConstants.WIK_ZHAO);
				// }

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					if (table.GRR._left_card_count > 0) {
						curPlayerStatus.add_action(GameConstants.WIK_ZHAO);// 听牌的时候可以杠
						// 加上杠
						curPlayerStatus.add_zhao(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					}

				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			// curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
			// 操作状态
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				// curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);//
				// 出牌状态
				table.operate_player_status();
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
	public boolean handler_operate_card(FLSTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("DispatchCard 不是当前玩家操作");
			return false;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}

		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		// playerStatus.clean_status();
		table.change_player_status(seat_index, GameConstants.INVALID_VALUE);
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_NULL }, 1);

			// add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			// table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index, GameConstants.INVALID_VALUE);
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data), GameConstants.DELAY_AUTO_OUT_CARD,
						TimeUnit.MILLISECONDS);
			} else {
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				// table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			// table._playerStatus[i].clean_status();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		case GameConstants.WIK_ZHAO: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true, false);
					return true;
				}
			}

		}
			break;
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._banker_select = _seat_index;
			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
				if (table.GRR._banker_player == _seat_index) {
					table._banker_select = _seat_index;
				} else {
					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				}
			}
			table._shang_zhuang_player = _seat_index;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_fls_twenty(_seat_index, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[_seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[_seat_index]++;
			}
			table.countChiHuTimesTwenty(_seat_index, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[_seat_index].type_count > 2) {
				delay += table.GRR._chi_hu_rights[_seat_index].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(FLSTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.be_in_room_trustee(seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
