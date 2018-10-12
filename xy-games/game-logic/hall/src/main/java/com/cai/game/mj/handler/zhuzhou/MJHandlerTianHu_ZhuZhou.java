package com.cai.game.mj.handler.zhuzhou;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.mj.MJTable;
import com.cai.game.mj.handler.MJHandlerTianHu;

public class MJHandlerTianHu_ZhuZhou extends MJHandlerTianHu {

	private static Logger logger = Logger.getLogger(MJHandlerTianHu_ZhuZhou.class);

	private GangCardResult m_gangCardResult;

	public MJHandlerTianHu_ZhuZhou() {
		m_gangCardResult = new GangCardResult();
	}

	// 先发牌
	@Override
	public void exe(MJTable table) {

		// 用户状态
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			//table._playerStatus[i].clean_status();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		if (table.DEBUG_CARDS_MODE) {
			// table.GRR._left_card_count=1;
		}

		table._current_player = _seat_index;// 轮到操作的人是自己

		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		
		--table.GRR._left_card_count;

		if (table.DEBUG_CARDS_MODE) {
			_send_card_data=0x11;
		}

		table._provide_player = _seat_index;

		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();

		int action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], _send_card_data, chr,
				GameConstants.HU_CARD_TYPE_ZIMO,_seat_index);// 自摸

		boolean is_ting = false;

		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_send_card_data, _seat_index);
			is_ting = true;

		} else {
			chr.set_empty();
			// 如果没有天胡 切换到地胡状态
//			table.exe_di_hu(_seat_index);
		}

		// 加到手牌
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

		// 发送数据
		// 只有自己才有数值
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT);
		
		if(curPlayerStatus.has_zi_mo() && table.has_rule(GameConstants.GAME_RULE_HUNAN_JIANPAOHU)){
			//见炮胡
			table.exe_jian_pao_hu(_seat_index,GameConstants.WIK_ZI_MO,_send_card_data);
			return ;
		}

		// 设置变量
		table._provide_card = _send_card_data;// 提供的牌

		m_gangCardResult.cbCardCount = 0;
		// 长沙麻将
		if (table.GRR._left_card_count > 1) {
			// 看手上所有的牌,有没有补张或杠
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],
					m_gangCardResult, true);

			if (cbActionMask != GameConstants.WIK_NULL) {// 有杠
				// 添加动作 长沙麻将是补张
//				curPlayerStatus.add_action(MJGameConstants.WIK_BU_ZHNAG);

				for (int i = 0; i < m_gangCardResult.cbCardCount; i++) {
					// 加上补张
					curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
//					if (table.GRR._left_card_count > 2) {
						// 把可以杠的这张牌去掉。看是不是听牌
						int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						int save_count = table.GRR._cards_index[_seat_index][bu_index];
						table.GRR._cards_index[_seat_index][bu_index] = 0;

						is_ting = table.is_zhuzhou_ting_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]);
						// 把牌加回来
						table.GRR._cards_index[_seat_index][bu_index] = save_count;

						if (is_ting == true) {
							curPlayerStatus.add_action(GameConstants.WIK_GANG);// 听牌的时候可以杠
							// 加上杠
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
						}
//					}

				}
			}
		}

		if (curPlayerStatus.has_action()) {// 有动作
			//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			table.operate_player_action(_seat_index, false);
		} else {
			//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			// 不能换章,自动出牌
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
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
	public boolean handler_operate_card(MJTable table, int seat_index, int operate_code, int operate_card) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			logger.error("[要天胡],操作失败," + seat_index + "玩家操作已失效");
			return false;
		}

		if (seat_index != _seat_index) {
			table.log_error("不是当前玩家操作");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			logger.error("[要天胡],操作失败," + seat_index + "玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && playerStatus.has_action_by_code(operate_code) == false)// 没有这个操作动作
		{
			logger.error("[要天胡],操作失败," + seat_index + "没有动作");
			return false;
		}

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
			
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();

			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data), GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
				table.operate_player_status();
			}

			// 如果不胡 切换到地胡状态
			table.exe_di_hu(_seat_index);
			return true;

		}

		// 执行动作
		switch (operate_code) {
		case GameConstants.WIK_GANG: // 杠牌操作
		case GameConstants.WIK_BU_ZHNAG: // 杠牌操作
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
			// 先清掉其它类型 只保存天胡
			table.GRR._chi_hu_rights[_seat_index].set_empty();
			table.GRR._chi_hu_rights[_seat_index].opr_or(GameConstants.CHR_HUNAN_TIAN_HU);

			table.GRR._chi_hu_rights[_seat_index].set_valid(true);

			// 下局胡牌的是庄家
			table.set_niao_card(_seat_index, GameConstants.INVALID_VALUE, true, 0);// 结束后设置鸟牌

			table.GRR._chi_hu_card[_seat_index][0] = operate_card;

			table._banker_select = _seat_index;

			table.process_chi_hu_player_operate(_seat_index, operate_card, true);
			table.process_chi_hu_player_score(_seat_index, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[_seat_index].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[_seat_index]++;
			} else {
				table._player_result.xiao_hu_zi_mo[_seat_index]++;
			}

			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), _seat_index, GameConstants.Game_End_NORMAL), GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;

	}

}
