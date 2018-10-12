/**
 * 
 */
package com.cai.game.gxzp.handler.bayi;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysParamDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.gxzp.GXZPTable;
import com.cai.game.gxzp.handler.GXZPHandlerDispatchCard;

public class BYZPHandlerDispatchAddCard_GL extends GXZPHandlerDispatchCard<GXZPTable> {

	@Override
	public void exe(GXZPTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了

		// 荒庄结束
//		if (table.GRR._left_card_count == 0) {
//			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
//				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
//			}
//			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//					% table.getTablePlayerNumber();
//			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
//			// 流局
//			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
//
//			return;
//		}
		boolean is_hu = false;
//		table._current_player = _seat_index;// 轮到操作的人是自己
		
		// 从牌堆拿出一张牌
		table._send_card_count++;
		_send_card_data = table._repertory_card[table._all_card_len - table.GRR._left_card_count];
		--table.GRR._left_card_count;
		table._send_card_data = _send_card_data;
		table._provide_player = _seat_index;
		int send_index  = table._logic.switch_to_card_index(_send_card_data);
		boolean is_fa_pai = false;
		table._ti_mul_long[_seat_index] --;
		
		table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,true);
		int ti_count = 0;
		int sao_count =0;
		int hong_pai_count = 0;
		int hei_pai_count = 0;
		int all_cards_count = 0;
		for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
		{
			if(table.GRR._cards_index[_seat_index][j] == 4||(table.GRR._cards_index[_seat_index][j] == 3 && j == send_index))
			{
				ti_count++;
				
			}
			if((table.GRR._cards_index[_seat_index][j] == 3&&j != send_index)||(table.GRR._cards_index[_seat_index][j] == 2 && j == send_index))
			{
				sao_count++;
				
			}
			
		}
		if((ti_count >= 3)||(sao_count >= 5))
		{
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			chr.set_empty();
		  	int all_hu_xi = 0;
			for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
			{
				
				if(table.GRR._cards_index[_seat_index][j] == 4||(table.GRR._cards_index[_seat_index][j] == 3 && j == send_index))
				{
					if(j<10)
						all_hu_xi +=12;
					else
						all_hu_xi += 9;
				}
				if((table.GRR._cards_index[_seat_index][j] == 3&&j != send_index)||(table.GRR._cards_index[_seat_index][j] == 2 && j == send_index))
				{
					if(j<10)
						all_hu_xi +=6;
					else
						all_hu_xi += 3;
				}
			}
			int max_hu_xi = 10;
        	if(all_hu_xi>=max_hu_xi)
        	{
        		
				int weave_count = 0;
				for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
				{
				
  
					if(table.GRR._cards_index[_seat_index][j] == 4||(table.GRR._cards_index[_seat_index][j] == 3 && j == send_index))
						
					{
						table._hu_weave_items[_seat_index][weave_count].center_card = table._logic.switch_to_card_data(j);
						table._hu_weave_items[_seat_index][weave_count].weave_kind = GameConstants.WIK_AN_LONG;
						table._hu_weave_items[_seat_index][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[_seat_index][weave_count],false);
						
						weave_count++;
						table._hu_xing_count[_seat_index] = 4;
					}
			
					if((table.GRR._cards_index[_seat_index][j] == 3&& j != send_index) ||(table.GRR._cards_index[_seat_index][j] == 2 && j == send_index))
					{
						table._hu_weave_items[_seat_index][weave_count].center_card = table._logic.switch_to_card_data(j);
						table._hu_weave_items[_seat_index][weave_count].weave_kind = GameConstants.WIK_KAN;
						table._hu_weave_items[_seat_index][weave_count].hu_xi = table._logic.get_weave_hu_xi(table._hu_weave_items[_seat_index][weave_count],false);	
					
						weave_count++;
						table._hu_xing_count[_seat_index] = 2;
					}
					
					
				}
				int hu_card = table._hu_weave_items[_seat_index][weave_count-1].center_card;
        		table._hu_weave_count[_seat_index] = weave_count;
        		all_cards_count = hong_pai_count +hei_pai_count;
        		
        		if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chr.set_empty();
					chr.opr_or(GameConstants.CHR_TIAN_HU);
				}
				
        		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
        		curPlayerStatus.reset();
 
				// 添加动作
				curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
				curPlayerStatus.add_zi_mo(hu_card, _seat_index);	
				GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data),
						600, TimeUnit.MILLISECONDS);
				return ;

        	}
        	else{
    			chr.set_empty();	
    		}
                	
        }
		if(_seat_index == table._cur_banker)
    	{
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			chr.set_empty();
		  	int all_hu_xi = 0;
    		int action = GameConstants.WIK_NULL;
    		if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]!=0)
    		{
    			int hu_xi[] = new int [1];
    			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
    					table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_send_card_data, chr, card_type,hu_xi,true);// 自摸
    		}
    		else
    		{
    			int hu_xi[] = new int [1];
    			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
    			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
    					table.GRR._weave_count[_seat_index],_seat_index,_seat_index, _send_card_data, chr, card_type,hu_xi,true);// 自摸
    		}
    		if (action != GameConstants.WIK_NULL) {
    			if (card_type == GameConstants.HU_CARD_TYPE_ZIMO ) {
					chr.set_empty();
					chr.opr_or(GameConstants.CHR_TIAN_HU);
				}
    			// 添加动作
    			table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
    			table._playerStatus[_seat_index].add_zi_mo(table._send_card_data, _seat_index);
    			GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,table._send_card_data),
    					600, TimeUnit.MILLISECONDS);
    			//发 操作
    			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
    			table.operate_player_action(_seat_index,false);
    			return ;

    		}
    		else{
    			chr.set_empty();	
    		}	
    	}
    
		int loop = 0; 
		int next_player = _seat_index;
		while(loop < table.getTablePlayerNumber())
		{
			 next_player = (next_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			if(next_player == table._cur_banker)
				break;
			if(table._ti_mul_long[next_player] > 0)
			{
				// 用户切换
			
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
//						table.operate_remove_discard(table._current_player, table.GRR._discard_count[table._current_player]);
				//没有人要就加入到牌堆
				int hand_card_update_time = 2000;
				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
				SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
						.get(1104);
				if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
					hand_card_update_time = sysParamModel1104.getVal4();
				}
				
				table.exe_hand_card_update(_seat_index,_send_card_data,hand_card_update_time);
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
				_send_card_data = 0;
				int dispatch_time = 3000;
				SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
						.get(1105);
				if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
					dispatch_time = sysParamModel1105.getVal2()+200;
				}
				table.exe_dispatch_add_card(next_player, GameConstants.WIK_NULL, dispatch_time);
				return ;
			}
			loop ++;
		}
		
		
		int cards[]= new int[GameConstants.MAX_GXZP_COUNT];
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		_send_card_data = 0;
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);			
		table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		table.operate_player_status();
		return;
	}
	/***
	 * //用户出牌
	 */
	@Override
	public boolean handler_player_out_card(GXZPTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != table._cur_banker) {
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[table._cur_banker].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			table.log_error("状态不对不能出牌");
			return false;
		}
		if (table.GRR._cards_index[table._cur_banker][table._logic.switch_to_card_index(card)] >= 3) {
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_GXZP_COUNT];
			// 显示出牌
			table.operate_out_card(table._cur_banker, 0, null, GameConstants.OUT_CARD_TYPE_MID, GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[table._cur_banker], cards);
			table.operate_player_cards(table._cur_banker, hand_card_count, cards, table.GRR._weave_count[table._cur_banker], table.GRR._weave_items[table._cur_banker]);

			
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[table._cur_banker], card) == false) {
			table.log_error("出牌删除出错");
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
	public boolean handler_operate_card(GXZPTable table,int seat_index, int operate_code, int operate_card,int luoCode){
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

			table._cur_banker = target_player;
//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
			table._shang_zhuang_player = target_player;
//			table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_bayi(target_player, _seat_index, operate_card, true);

		
			table.countChiHuTimes_bayi(target_player, true);

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
	public boolean handler_player_be_in_room(GXZPTable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		
	 return true;
	}
}
