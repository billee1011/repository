package com.cai.fls.handler.lxfls;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.fls.FLSTable;
import com.cai.fls.handler.FLSHandlerDispatchCard;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.mj.MJTable;

/**
 * 摸牌
 * 
 * @author Administrator
 *
 */
public class FLSHandlerDispatchCard_LX extends FLSHandlerDispatchCard {

	@Override
	public void exe(FLSTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.GRR._left_card_count == 0) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}

			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._shang_zhuang_player =  GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (table.DEBUG_CARDS_MODE) {
			// table.GRR._left_card_count=1;
		}

		// //海底
		// if(table.is_mj_type(GameConstants.GAME_TYPE_CS) &&
		// table.GRR._left_card_count==1){
		// table.exe_hai_di(_seat_index,_seat_index);
		// return;
		// }

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
//			 _send_card_data=0x61;
		}
		table._provide_player = _seat_index;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		if (_type == GameConstants.HU_CARD_TYPE_GANG_KAI) {
			card_type = GameConstants.HU_CARD_TYPE_GANG_KAI;
		}

		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index], _send_card_data, chr, card_type);// 自摸

		boolean is_ting = false;

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);

			if(table.GRR._left_card_count<=3) {//最后4张牌的自摸胡牌都算海底捞
				chr.opr_or(GameConstants.CHR_FLS_HAIDI);
			}
			is_ting = true;

		} else {
			chr.set_empty();
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		m_gangCardResult.cbCardCount = 0;
		// 长沙麻将
		if (table.GRR._left_card_count > 1) {
			// 看手上所有的牌,有没有补张或杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult, false);

			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				// 添加动作 长沙麻将是补张
				curPlayerStatus.add_action(GameConstants.WIK_ZHAO);

				// int bu_index =
				// table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
				// int save_count =
				// table.GRR._cards_index[_seat_index][bu_index];
				// table.GRR._cards_index[_seat_index][bu_index]=0;
				// table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]--;
				// is_ting =
				// table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
				// table.GRR._weave_items[_seat_index],
				// table.GRR._weave_count[_seat_index]);

				is_ting = false;
				// 把牌加回来
				// table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				// table.GRR._cards_index[_seat_index][bu_index] = save_count;

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上补张
					curPlayerStatus.add_zhao(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					boolean can_gang = false;
					if (table.GRR._left_card_count > 2) {
						// 把可以杠的这张牌去掉。看是不是听牌
						// int bu_index =
						// table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						// int save_count =
						// table.GRR._cards_index[_seat_index][bu_index];
						// table.GRR._cards_index[_seat_index][bu_index]=0;

						// is_ting =
						// table.is_cs_ting_card(table.GRR._cards_index[_seat_index],
						// table.GRR._weave_items[_seat_index],
						// table.GRR._weave_count[_seat_index]);
						// 把牌加回来
						// table.GRR._cards_index[_seat_index][bu_index] =
						// save_count;
						if (is_ting == true) {
							can_gang = true;

						} else {
							// 把可以杠的这张牌去掉。看是不是听牌
							int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
							int save_count = table.GRR._cards_index[_seat_index][bu_index];
							table.GRR._cards_index[_seat_index][bu_index] = 0;

							int cbWeaveIndex = table.GRR._weave_count[_seat_index];

							if (m_gangCardResult.type[i] == GameConstants.GANG_TYPE_AN_GANG) {
								table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 0;
								table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = m_gangCardResult.cbCardData[i];
								table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_GANG;
								table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
								table.GRR._weave_count[_seat_index]++;
							}

							can_gang = table.is_fls_ting_card(table.GRR._cards_index[_seat_index],
									table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);

							table.GRR._weave_count[_seat_index] = cbWeaveIndex;//将落地数目还原

							// 把牌加回来
							table.GRR._cards_index[_seat_index][bu_index] = save_count;//将手牌加回来

						}

						if (can_gang) {
							curPlayerStatus.add_action(GameConstants.WIK_GANG);// 听牌的时候可以杠
							// 加上杠
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index,
									m_gangCardResult.isPublic[i]);
						}
					}

				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
		} else {
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
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
			table.log_error("没有这个操作");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return true;
		}
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();

		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			return true;
		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		case GameConstants.WIK_ZHAO: // 杠牌操作
		{
			for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
				if (operate_card == m_gangCardResult.cbCardData[i]) {
					// 是否有抢杠胡
					table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,
							false);
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
				table._banker_select =  (table.GRR._banker_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			}
			table._shang_zhuang_player = _seat_index;
			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score_fls(_seat_index, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[_seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[_seat_index]++;
			}
			table.countChiHuTimes(_seat_index,true);

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(FLSTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}

}
