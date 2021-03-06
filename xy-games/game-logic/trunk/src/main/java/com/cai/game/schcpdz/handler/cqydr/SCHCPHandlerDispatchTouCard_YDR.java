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
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.handler.SCHCPDZHandlerDispatchCard;

public class SCHCPHandlerDispatchTouCard_YDR extends SCHCPDZHandlerDispatchCard<SCHCPDZTable> {

	@Override
	public void exe(SCHCPDZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
		if (table.get_end()) {
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
//			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			

			return;
		}
//		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
//		if(table.DEBUG_CARDS_MODE)
//			_send_card_data = 0x05;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		if(table._ti_mul_long[_seat_index]>0)
			table._ti_mul_long[_seat_index] --;
		int cur_index = table._logic.switch_to_card_index(_send_card_data);
		table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,false);
		
		if(table._cannot_peng_index[_seat_index][cur_index]>0 && table.GRR._cards_index[_seat_index][cur_index] == 2)
		{
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DZ_NO_PENG_TO_KAN|_send_card_data }, 1,
		
					GameConstants.INVALID_SEAT);
		}
		else if(table._cannot_peng_index[_seat_index][cur_index]>0 && table.GRR._cards_index[_seat_index][cur_index] == 1){
			table._cannot_peng_index[_seat_index][cur_index] = 0;
			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DZ_NOW_PENG_TO_PENG|_send_card_data }, 1,
					GameConstants.INVALID_SEAT);
		}
		
		int action = table.estimate_player_tu_huo(_seat_index, _send_card_data);
		if(action == GameConstants.DZ_WIK_NULL){
			action = table.estimate_player_ming_tu_huo(_seat_index, _send_card_data);
		}
		Arrays.fill(table._is_sha_index, -1);
		ChiHuRight	chr = table.GRR._chi_hu_rights[_seat_index];
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_send_card_data, chr, card_type,true);// 自摸
		if (action_hu != GameConstants.WIK_NULL && action == 0) {
			
			table._playerStatus[_seat_index].add_action(GameConstants.DZ_WIK_ZI_MO);
			table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);
			table._playerStatus[_seat_index].add_action(GameConstants.DZ_WIK_NULL);
			table._playerStatus[_seat_index].add_pass(_send_card_data, _seat_index);
		
		}
		else {
				chr.set_empty();
		}	
		
	
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		if (curPlayerStatus.has_action() ) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}

			table.operate_player_action(_seat_index, false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_status();
			
		} else {
			if (table.isTrutess(_seat_index)) {
				
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,true),
					800, TimeUnit.MILLISECONDS);
			
		}
    
		return;
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

		for(int i = 0; i < table.getTablePlayerNumber();i++)
		{
			if(table._is_bao_zi[i] == true)
				continue;
			if(table._is_system_bao_zi[i] ==true)
				continue;
			if(table._is_yang[i] == true)
				continue;
			if(table.is_bao_zi(i)){
				table._is_system_bao_zi[i] = true;
				table.operate_player_xiang_gong_flag(i, true);
				table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.DZ_WIK_YANG }, 1,
						GameConstants.INVALID_SEAT);
			}
		}
		if(table.is_game_end()==false)
			return true;
		// 变量定义 优先级最高操作的玩家和操作--不通炮的算法
		int target_player = seat_index;
		int target_action = operate_code;
		int target_lou_code =luoCode;
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
					target_lou_code = table._playerStatus[i].get_lou_kind();
					target_p = table.getTablePlayerNumber() - p;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
	
		

		// 用户状态
//		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//			
//			table._playerStatus[i].clean_action();
//			table._playerStatus[i].clean_status();
//
//			table.operate_player_action(i, true);
//		}
		// 执行动作
		switch (target_action) {
		case GameConstants.DZ_WIK_NULL:
		{
			int card_count = 0;
			table.operate_player_get_card(_seat_index, 0,  null, GameConstants.INVALID_SEAT,true);
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
	
			for(int i = 0; i<GameConstants.DZ_MAX_CP_INDEX;i++)
				if(table.GRR._cards_index[seat_index][i] < 3)
					card_count += table.GRR._cards_index[seat_index][i];
			if(card_count == 0 && table._playerStatus[seat_index].has_zi_mo()){
				table._is_bao_zi[seat_index] = true;
				table.operate_player_xiang_gong_flag(seat_index, true);
				if(table.is_game_end()==false)
					return true;
				// 用户切换
				table._current_player = table._current_player = table.get_cur_index(seat_index) ; 
				
				table._last_player = table._current_player ;
				table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);
				// 用户状态	
				for (int i = 0; i < table.getTablePlayerNumber(); i++) {
					
					table._playerStatus[i].clean_action();
					table._playerStatus[i].clean_status();
		
					table.operate_player_action(i, true);
				}
				return true;
			}
			// 用户状态	
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
	
				table.operate_player_action(i, true);
			}
			table._playerStatus[seat_index].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			_send_card_data = 0;
			

			return true;

		}
		case GameConstants.DZ_WIK_TU_HUO:
		case GameConstants.DZ_WIK_MTU_HUO:
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
	
				table.operate_player_action(i, true);
			}
			if(target_action == GameConstants.DZ_WIK_TU_HUO)
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;
			table.exe_gang(target_player, _seat_index, operate_card, target_action,GameConstants.CHR_DZ_ADD_CARD,true, true, false,1000);
		
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


			table._shang_zhuang_player = target_player;
			table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,false);
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
