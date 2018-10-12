package com.cai.game.mj.shanxi.sxkdd;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.mj.handler.MJHandlerGang;

/**
 * 
 * 
 *
 * @author WalkerGeek 
 * date: 2018年6月13日 下午4:17:25 <br/>
 */
public class MJHandlerGang_SXKDD extends MJHandlerGang<MJTable_SXKDD> {
	

	// 执行杆操作
	@Override
	public void exe(MJTable_SXKDD table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if (table._playerStatus[i].has_action()) {
				table.operate_player_action(i, true);
			}

			table._playerStatus[i].clean_action();
			table.change_player_status(i, GameConstants.INVALID_VALUE);
		}
		

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);

		if ((GameConstants.GANG_TYPE_AN_GANG == _type) || (GameConstants.GANG_TYPE_JIE_GANG == _type)) {
			this.exe_gang(table);// 暗杆 接杆不能抢
			return;
		}

		// 检查对这个杠有没有 胡
		boolean bAroseAction = table.estimate_gang_respond(_seat_index, _center_card);

		if (bAroseAction == false) {
			this.exe_gang(table);
		} else {
			PlayerStatus playerStatus = null;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				playerStatus = table._playerStatus[i];
				if (playerStatus.has_chi_hu()) {
					table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
					table.operate_player_action(i, false);
				}
			}
		}

	}

	/***
	 * //用户操作 --抢杠胡
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @return
	 */
	@Override
	public boolean handler_operate_card(MJTable_SXKDD table, int seat_index, int operate_code, int operate_card) {

		// 效验状态
		PlayerStatus playerStatus = table._playerStatus[seat_index];

		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "出牌,玩家操作已失效");
			return false;
		}

		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "出牌,玩家已操作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_code != GameConstants.WIK_CHI_HU))// 没有这个操作动作
		{
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		if ((operate_code != GameConstants.WIK_NULL) && (operate_card != _center_card)) {
			table.log_player_error(seat_index, "出牌操作,操作牌对象出错");
			return false;
		}

		// 玩家的操作
		playerStatus.operate(operate_code, operate_card);

		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1,
					new long[] { operate_code }, 1);

			table.GRR._chi_hu_rights[seat_index].set_valid(false);// 胡牌失效
			table._playerStatus[seat_index].chi_hu_round_invalid();// 这一轮就不能吃胡了没过牌之前都不能胡
		} else if (operate_code == GameConstants.WIK_CHI_HU) {
			
			
		} else {
			table.log_player_error(seat_index, "出牌操作,没有动作");
			return false;
		}

		// 吃胡等待 因为胡牌的等级是一样的，可以一炮多响，看看是不是还有能胡的
		/*for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table._playerStatus[i].is_respone() == false) && (table._playerStatus[i].has_chi_hu()))
				return false;
		}*/

		 // 变量定义 优先级最高操作的玩家和操作
        int target_player = seat_index;
        int target_action = operate_code;
        int target_p = 0;
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_seat_index + p) % GameConstants.GAME_PLAYER;
            if (i == target_player) {
                target_p = GameConstants.GAME_PLAYER - p;
            }
        }
        // 执行判断
        for (int p = 0; p < GameConstants.GAME_PLAYER; p++) {
            int i = (_seat_index + p) % GameConstants.GAME_PLAYER;
            // 获取动作
            int cbUserActionRank = 0;
            // 优先级别
            int cbTargetActionRank = 0;
            if (table._playerStatus[i].has_action()) {
                if (table._playerStatus[i].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
                            + GameConstants.GAME_PLAYER - p;
                } else {
                    // 获取最大的动作的优先级
                    cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
                            table._playerStatus[i]._action) + GameConstants.GAME_PLAYER - p;
                }

                if (table._playerStatus[target_player].is_respone()) {
                    // 获取已经执行的动作的优先级
                    cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
                            + target_p;
                } else {
                    // 获取最大的动作的优先级
                    cbTargetActionRank = table._logic.get_action_list_rank(
                            table._playerStatus[target_player]._action_count,
                            table._playerStatus[target_player]._action) + target_p;
                }

                // 优先级别
                // int cbTargetActionRank =
                // table._logic.get_action_rank(target_action) +
                // target_p;//table._playerStatus[target_player].get_perform()

                // 动作判断 优先级最高的人和动作
                if (cbUserActionRank > cbTargetActionRank) {
                    target_player = i;// 最高级别人
                    target_action = table._playerStatus[i].get_perform();
                    target_p = GameConstants.GAME_PLAYER - p;
                }
            }
        }
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
			return true;
		// 变量定义
		//int target_card = table._playerStatus[target_player]._operate_card;
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}

		int jie_pao_count = 0;
		table.GRR._chi_hu_rights[seat_index].set_valid(true);// 胡牌生效
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
				continue;
			}
			jie_pao_count++;
		}

		if (jie_pao_count > 0) {
			table._cur_banker = (table._cur_banker + 1) % table.getTablePlayerNumber();
			
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if ((table.GRR._chi_hu_rights[i].is_valid() == false)) {
					continue;
				}

				table.GRR._chi_hu_card[i][0] = _center_card;
				table.process_chi_hu_player_operate(seat_index, _center_card, false);// 效果

				table.process_chi_hu_player_score(i, _seat_index, _center_card, false);
				// 记录
				table._player_result.jie_pao_count[i]++;
				table._player_result.dian_pao_count[_provide_player]++;
			}

			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					GameConstants.GAME_FINISH_DELAY, TimeUnit.SECONDS);

		} else {
			// 选择了不胡  发牌给杠的玩家
			this.exe_gang(table);
		}

		return true;
	}

	/**
	 * 执行杠
	 * 
	 * 
	 ***/
	@Override
	protected boolean exe_gang(MJTable_SXKDD table) {
		int cbCardIndex = table._logic.switch_to_card_index(_center_card);
		int cbWeaveIndex = -1;

		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			// 暗杠
			// 设置变量
			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 别人打的牌

			cbWeaveIndex = table.GRR._weave_count[_seat_index];
			table.GRR._weave_count[_seat_index]++;

			// 删掉出来的那张牌
			table.operate_remove_discard(this._provide_player, table.GRR._discard_count[_provide_player]);

		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			// 看看是不是有碰的牌，明杠
			// 寻找组合
			for (int i = 0; i < table.GRR._weave_count[_seat_index]; i++) {
				int cbWeaveKind = table.GRR._weave_items[_seat_index][i].weave_kind;
				int cbCenterCard = table.GRR._weave_items[_seat_index][i].center_card;
				if ((cbCenterCard == _center_card) && (cbWeaveKind == GameConstants.WIK_PENG)) {
					cbWeaveIndex = i;// 第几个组合可以碰
					break;
				}
			}

			if (cbWeaveIndex == -1) {
				table.log_player_error(_seat_index, "杠牌出错");
				return false;
			}

		}

		table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = _p == true ? 1 : 0;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = _center_card;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _provide_player;
		table.GRR._weave_items[_seat_index][cbWeaveIndex].type = _type;

		// 设置用户
		table._current_player = _seat_index;

		// 删除手上的牌
		table.GRR._cards_index[_seat_index][cbCardIndex] = 0;
		table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
		// 刷新手牌包括组合
		int cards[] = new int[GameConstants.MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.changeCard(cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);

		///////////////////////////////////////////////////// 算分
		int cbGangIndex = table.GRR._gang_score[_seat_index].gang_count++;
		if (GameConstants.GANG_TYPE_AN_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				// 暗杠每人2分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = - 2;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 2;// 一共加分
			}

			table._player_result.an_gang_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = 2;
		} else if (GameConstants.GANG_TYPE_JIE_GANG == _type) {
			// 放杠三分
			int score = 1;//table.getTablePlayerNumber() - 1;

			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] = score * GameConstants.CELL_SCORE;// m_pGameServiceOption->lCellScore*2;//配置参数乘
			table.GRR._gang_score[_seat_index].scores[cbGangIndex][_provide_player] = -score * GameConstants.CELL_SCORE;// -m_pGameServiceOption->lCellScore*2;//配置参数乘

			table._player_result.ming_gang_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = score;
			if(table._playerStatus[_seat_index].is_bao_ting()){
				table.GRR._weave_items[_seat_index][cbWeaveIndex].can_add_gang = false;
			}
		} else if (GameConstants.GANG_TYPE_ADD_GANG == _type) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == _seat_index)
					continue;

				// 明杠每人1分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][i] = -1;// 暗杠，其他玩家扣分
				table.GRR._gang_score[_seat_index].scores[cbGangIndex][_seat_index] += 1;// 一共加分
			}
			table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_score = 1;
			table._player_result.ming_gang_count[_seat_index]++;
		}
		

		// 从后面发一张牌给玩家
		table.exe_dispatch_card(_seat_index, GameConstants.HU_CARD_TYPE_GANG_KAI, 0);

		return true;
	}
}
