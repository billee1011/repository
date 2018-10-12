/**
 * 
 */
package com.cai.game.hh.handler.yxphz;

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
import com.cai.game.hh.HHTable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class PHZHandlerDispatchFirstCard_YX extends HHHandlerDispatchCard {

	@Override
	public void exe(HHTable table) {
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
			table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._banker_select, GameConstants.Game_End_DRAW);

			return;
		}
		boolean is_hu = false;
		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;

//		_send_card_data = 0x18;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		int send_index  = table._logic.switch_to_card_index(_send_card_data);
		boolean is_fa_pai = false;
		for(int i = 0;i<table.getTablePlayerNumber();i++)
		{
			
			
			int ti_count = 0;
			int sao_count =0;
			int hong_pai_count = 0;
			int hei_pai_count = 0;
			int all_cards_count = 0;
			for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
			{
				if((i == table._current_player)&&(j == send_index))
					table.GRR._cards_index[i][j]++;
				if(table._logic.color_hei(table._logic.switch_to_card_data(j))==true){
        			hei_pai_count += table.GRR._cards_index[i][j];
        		}
        		else{
        			hong_pai_count += table.GRR._cards_index[i][j];
        		}
				if(table.GRR._cards_index[i][j] == 4)
				{
					ti_count++;
					if((i == table._current_player)&&(j == send_index))
						is_fa_pai = true;
				}
				if(table.GRR._cards_index[i][j] == 3)
				{
					sao_count++;
					if((i == table._current_player)&&(j == send_index))
						is_fa_pai = true;
				}
				if((i == table._current_player)&&(j == send_index))
					table.GRR._cards_index[i][j]--;
			}
			if(((ti_count == 2)&&( sao_count==4))||(ti_count >= 3)||(sao_count >= 5))
			{
				ChiHuRight chr = table.GRR._chi_hu_rights[i];
				int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
				chr.set_empty();
			  	int all_hu_xi = 0;
				for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
				{
					if((i == table._current_player)&&(j == send_index))
						table.GRR._cards_index[i][j]++;
					if(table.GRR._cards_index[i][j] == 4)
					{
						if(j<10)
							all_hu_xi +=12;
						else
							all_hu_xi += 9;
						ti_count++;
					}
					if(table.GRR._cards_index[i][j] == 3)
					{
						if(j<10)
							all_hu_xi +=6;
						else
							all_hu_xi += 3;
						sao_count++;
					}
					if((i == table._current_player)&&(j == send_index))
						table.GRR._cards_index[i][j]--;
				}
				int max_hu_xi = 15;
				if(table.has_rule(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI))
					max_hu_xi = 18;
	        	if(all_hu_xi>=max_hu_xi)
	        	{
	        		
					int weave_count = 0;
					for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
					{
					
						if((i == table._current_player)&&(j == send_index))
							table.GRR._cards_index[i][j]++;
	  
						if(table.GRR._cards_index[i][j] == 4)
						{
							table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
							table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);
							
							weave_count++;
						}
				
						if(table.GRR._cards_index[i][j] == 3)
						{
							table._hu_weave_items[i][weave_count].center_card = table._logic.switch_to_card_data(j);
							table._hu_weave_items[i][weave_count].weave_kind = GameConstants.WIK_SAO;
							table._hu_weave_items[i][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[i][weave_count]);	
						
							weave_count++;
						}
						if((i == table._current_player)&&(j == send_index))
							table.GRR._cards_index[i][j]--;
						
					}
					int hu_card = table._hu_weave_items[i][weave_count-1].center_card;
	        		table._hu_weave_count[i] = weave_count;
	        		all_cards_count = hong_pai_count +hei_pai_count;
	        		
					if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
						chr.opr_or(GameConstants.CHR_TIAN_HU);
					}
		
	        		PlayerStatus curPlayerStatus = table._playerStatus[i];
	        		curPlayerStatus.reset();
	        		if((i == table._current_player)&&(is_fa_pai == true))
	        		{	// 发送数据
	        			// 只有自己才有数值
	        			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
	        		}
					// 添加动作
					curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
					curPlayerStatus.add_zi_mo(hu_card, i);	
					 if (curPlayerStatus.has_action()) {
						  curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);//
						 //操作状态
						  table.operate_player_action(i, false);
						 }
					 is_hu = true;
	        	}
	        	else{
					chr.set_empty();
				}	        	
	        }
		
			
		}
	
		if(is_hu == false)
		{

	



			// 发送数据
			// 只有自己才有数值
			if(table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI))
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
			else
				table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,true);
	
			// 设置变量
			table._provide_card = _send_card_data;// 提供的牌
	
			table.exe_chuli_first_card(_seat_index,GameConstants.WIK_NULL,2500);
		}
		return;
	}

	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(HHTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_error("出牌,没到出牌");
			return false;
		}

		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌
		// if(_type == MJGameConstants.DispatchCard_Type_Gang){
		// table.exe_re_chong(_seat_index,card,_type);
		// }else {
		// table.exe_out_card(_seat_index,card,_type);
		// }
		table.exe_out_card(_seat_index, card, _type);

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
	public boolean handler_operate_card(HHTable table,int seat_index, int operate_code, int operate_card,int luoCode){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		table.log_error(_seat_index+"  " + table._current_player +"  " +"下次 出牌用户" +seat_index+"操作用户" );
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
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
					cbMaxActionRand = cbUserActionRank;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		
		if (table._playerStatus[target_player].is_respone() == false)
		{
			table.log_error("最用户操作");
			return true;
		}
	
		// 变量定义
		int target_card = table._playerStatus[target_player]._operate_card;
	
		

		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();

			table.operate_player_action(i, true);
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL:
		{
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				int pai_count =0;
				for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
			       	if(table.GRR._cards_index[_seat_index][i]<3)
			       		pai_count += table.GRR._cards_index[_seat_index][i];
				 }
				if(pai_count == 0)
				{
					table._is_xiang_gong[_seat_index] = true;	 	
		        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
		        	int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;
					
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					table.log_error(next_player + "可以胡，而不胡的情况                                                                                                                                                                                                                                                       "+_seat_index);
					return true;
				}
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
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

			table._banker_select = target_player;
//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_phz(target_player, _seat_index, operate_card, true);

		
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
	public boolean handler_player_be_in_room(HHTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		
	 return true;
	}
}
