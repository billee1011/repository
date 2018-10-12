/**
 * 
 */
package com.cai.game.schcpdz.handler.cqydr;

import java.util.Arrays;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.handler.SCHCPDZHandlerDispatchCard;


public class SCHCPHandlerDispatchAddCard_YDR extends SCHCPDZHandlerDispatchCard<SCHCPDZTable> {

	@Override
	public void exe(SCHCPDZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		// 从牌堆拿出一张牌
		
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		if(table._ti_mul_long[_seat_index]>0)
			table._ti_mul_long[_seat_index] --;
		
		
		table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,false);

		GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,false),
				800, TimeUnit.MILLISECONDS);
		return;
	}
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(SCHCPDZTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != table._cur_banker) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[table._cur_banker].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI状态不对不能出牌");
			return false;
		}
		if (table.GRR._cards_index[table._cur_banker][table._logic.switch_to_card_index(card)] >= 3) {
			// 刷新手牌
			int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
			// 显示出牌
			table.operate_out_card(table._cur_banker, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[table._cur_banker], cards);
			table.operate_player_cards(table._cur_banker, hand_card_count, cards, table.GRR._weave_count[table._cur_banker], table.GRR._weave_items[table._cur_banker]);

			
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[table._cur_banker], card) == false) {
			table.log_error("SCHCPHandlerDispatchAddCard_DAZHUI出牌删除出错");
			return false;
		}

		// 出牌
		table.exe_out_card(table._cur_banker, card, GameConstants.WIK_NULL);

		return true;
	}

	/***
	 * //用户操作
	 * 
	 * @param seat_index
	 * @param operate_code
	 * @param operate_card
	 * @param luoCode
	 * @return
	 */
	@Override
	public boolean handler_operate_card(SCHCPDZTable table,int seat_index, int operate_code, int operate_card,int luoCode){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家操作已失效");
			return true;
		}
		//if (seat_index != _seat_index) {
		//	table.log_error("DispatchCard 不是当前玩家操作");
		//	return false;
		//}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}

		if(playerStatus.has_zi_mo() == true)
			table._guo_hu_pai_cards[seat_index][table._logic.switch_to_card_index(operate_card)] ++;

		
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();



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
		int cbActionRank[] = new int[table.getTablePlayerNumber()];
		for (int p = 0; p < table.getTablePlayerNumber(); p++) {
			int i = (_seat_index + p) % table.getTablePlayerNumber();
			// 获取动作
			int cbUserActionRank = 0;
			// 优先级别
			int cbTargetActionRank = 0;
			if (table._playerStatus[i].has_action()) {
				if (table._playerStatus[i].is_respone()) {
					// 获取已经执行的动作的优先级
					cbUserActionRank = table._logic.get_action_rank(table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(
							table._playerStatus[target_player]._action_count,
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
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 执行动作
		switch (target_action) {
		case GameConstants.DZ_WIK_NULL:
		{
		
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
			}
			// 用户状态
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
				
				// 刷新自己手牌
				int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
				table.exe_chuli_first_card(_seat_index,GameConstants.WIK_NULL,1000);
			}
			
			
			return true;

		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
				}
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;


//			table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_chq_ydr(target_player, _seat_index, operate_card, true);
		
			table.countChiHuTimes(target_player, true);

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(SCHCPDZTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		table.set_qi_player();
	 return true;
	}
}
