/**
 * 
 */
package com.cai.game.schcp.handler.dazhui;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcp.SCHCPTable;
import com.cai.game.schcp.handler.SCHCPHandlerDispatchCard;

public class SCHCPHandlerDispatchTouCard_DAZHUI extends SCHCPHandlerDispatchCard<SCHCPTable> {

	@Override
	public void exe(SCHCPTable table) {
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
//			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}
//		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
//		if(table.DEBUG_CARDS_MODE)
//			_send_card_data = 0x04;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		table._ti_mul_long[_seat_index] --;
		
		table.operate_player_get_card(_seat_index, 1,  new int[]{_send_card_data}, GameConstants.INVALID_SEAT,true);
		
		
		if(table.estimate_player_hua(_seat_index, _send_card_data) != GameConstants.CP_WIK_NULL)
		{
			return ;
		}
		ChiHuRight	chr = table.GRR._chi_hu_rights[_seat_index];
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_send_card_data, chr, card_type,true);// 自摸
		if (action_hu != GameConstants.WIK_NULL) {
			
			table._playerStatus[_seat_index].add_action(GameConstants.CP_WIK_ZI_MO);
			table._playerStatus[_seat_index].add_zi_mo(_send_card_data, _seat_index);
			table._playerStatus[_seat_index].add_action(GameConstants.CP_WIK_NULL);
			table._playerStatus[_seat_index].add_pass(_send_card_data, _seat_index);
		
		}
		else {
				chr.set_empty();
		}	
		

		int action = table.estimate_player_zhao(_seat_index, _send_card_data);
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		if(table._ti_mul_long[_seat_index] > 0 && action == 0)
    	{

			curPlayerStatus.add_action(GameConstants.CP_WIK_TOU);
			curPlayerStatus.add_tou(_send_card_data, GameConstants.CP_WIK_TOU, _seat_index);
    	}
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
			table.operate_player_get_card(_seat_index, 0,  null, GameConstants.INVALID_SEAT,true);
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_send_card_data)]++;

			int cards[] = new int[GameConstants.MAX_CP_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
			table.must_out_card();
			int s_cards[] = new int[3];
			if(_seat_index != table._xiao_jia)
			{
				int count = table._logic.switch_to_value_to_card(7,s_cards);
				for(int i = 0; i<count ;i++)
				{
					if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
					{
						table.cannot_outcard(_seat_index, s_cards[i],1, true);
					}
				}
			}
			int temp_must_count = 0;
			if(table._playerStatus[_seat_index]._hu_card_count != 0&&action_hu == GameConstants.CP_WIK_NULL){
				int dot = table._logic.get_dot(table._send_card_data);
				int must_cards [] = new int[3];
				int must_count = table._logic.switch_to_value_to_card(dot, must_cards);
				for(int i = 0; i<must_count ; i++){
					if(table.GRR._cannot_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
						continue;
					if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
					{
						if(table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])] == 0)
							table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]++;
						temp_must_count ++;
					}
				}			
				if(temp_must_count >0)
				{
					table.operate_must_out_card(_seat_index, true);
					if(table.check_out_card(_seat_index)==false)
					{
						table.no_card_out_game_end(_seat_index, 0);
						return  ;
					}
					curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
					_send_card_data = 0;
					return ;
				}
				else{
					
					table.no_card_out_game_end(_seat_index, _send_card_data);
					return ;
					
				}
				
			}
			if(table.check_out_card(_seat_index)==false)
			{
				table.no_card_out_game_end(_seat_index, 0);
				return  ;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			_send_card_data = 0;
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
	public boolean handler_operate_card(SCHCPTable table,int seat_index, int operate_code, int operate_card,int luoCode){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		
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
		// 记录玩家的操作
		playerStatus.operate(operate_code, operate_card);
		playerStatus.clean_status();
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}



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
		case GameConstants.CP_WIK_NULL:
		{
			// 用户状态
			if(table._playerStatus[_seat_index].has_cp_zhao())
			{
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
				
				
				// 刷新自己手牌
				int cards[] = new int[GameConstants.MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
				table.must_out_card();
				int s_cards[] = new int[3];
				int count = table._logic.switch_to_value_to_card(7,s_cards);
				for(int i = 0; i<count ;i++)
				{
					if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
					{
						table.cannot_outcard(_seat_index, s_cards[i],1, true);
					}
				}

				
			}
			
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
	
				table.operate_player_action(i, true);
			}
			table.operate_player_get_card(_seat_index, 0,  null, GameConstants.INVALID_SEAT,true);
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				
				if (curPlayerStatus.has_action() ) {// 有动作
					if (table.isTrutess(_seat_index)) {
						GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, operate_card),
								GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
					}
					curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
					table.operate_player_action(_seat_index, false);
				} else {
					if (table.isTrutess(_seat_index)) {
						
						GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, operate_card),
								GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
					}
					int temp_must_count = 0;
					if(table._playerStatus[_seat_index]._hu_card_count != 0){
						int dot = table._logic.get_dot(table._send_card_data);
						int must_cards [] = new int[3];
						int must_count = table._logic.switch_to_value_to_card(dot, must_cards);
						for(int i = 0; i<must_count ; i++){
							if(table.GRR._cannot_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
								continue;
							if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]!=0)
							{
								if(table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])] == 0)
									table.GRR._must_out_index[_seat_index][table._logic.switch_to_card_index(must_cards[i])]++;
								temp_must_count ++;
							}
						}			
						if(temp_must_count >0)
						{
							table.operate_must_out_card(_seat_index, true);
							if(table.check_out_card(_seat_index)==false)
							{
								table.no_card_out_game_end(_seat_index, 0);
								return true ;
							}
							curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
							table.operate_player_status();
							return true;
						}
						else{
							if(table.check_out_card(_seat_index)==false)
							{
								table.no_card_out_game_end(_seat_index, 0);
								return true ;
							}
							table.no_card_out_game_end(_seat_index, _send_card_data);
							return true;
							
						}
						
					}
					if(table.check_out_card(_seat_index)==false)
					{
						table.no_card_out_game_end(_seat_index, 0);
						return true ;
					}
//					GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _send_card_data,true),
//							800, TimeUnit.MILLISECONDS);
					_send_card_data = 0;
					curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
					
					
				}
			
			}
			return true;

		}
		case GameConstants.CP_WIK_TOU:
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
	
				table.operate_player_action(i, true);
			}
			table.exe_Dispatch_tou_card_data(_seat_index,GameConstants.WIK_NULL,0);
			return true;
		}
		case GameConstants.CP_WIK_ZHAO:
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();
	
				table.operate_player_action(i, true);
			}
			
			table.exe_zhao(target_player, _seat_index, target_action, target_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,target_lou_code);
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
//			table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_schcp(target_player, _seat_index, operate_card, true);
		
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
	public boolean handler_player_be_in_room(SCHCPTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		
	 return true;
	}
}
