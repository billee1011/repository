/**
 * 
 */
package com.cai.game.xpbh.handler.bh;

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
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.xpbh.XPBHTable;
import com.cai.game.xpbh.handler.BHHandlerDispatchCard;


public class BHHandlerChuLiFirstCard_XP extends BHHandlerDispatchCard<XPBHTable> {

	@Override
	public void exe(XPBHTable table) {
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
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}
		
		
		
		//刷新手牌包括组合
		_send_card_data = table._send_card_data;
		table._can_hu_pai_card = _send_card_data;
		int cards[]= new int[GameConstants.XPBH_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		int  an_long_Index[][] = new int[table.getTablePlayerNumber()][6];
		int  an_long_count[] = new int[table.getTablePlayerNumber()];
		int an_she_index[][]  = new int[table.getTablePlayerNumber()][8];
		int an_she_count[] = new int[table.getTablePlayerNumber()];
		boolean ti_send_card = false;
		//// 玩家出牌 响应判断,是否有提 暗龙
		int zhua_long_index = -1;
		
			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
		for(int j = 0; j<table.getTablePlayerNumber();j++)
		{
			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
			for(int i = 0; i< GameConstants.XPBH_MAX_INDEX;i++)
			{
				if(table.GRR._cards_index[j][i] == 4)
				{
					an_long_Index[j][an_long_count[j]++] = i;		
				}
				
				else if(table.GRR._cards_index[j][i] == 3)
				{
					an_she_index[j][an_she_count[j]++] = i;		
				}
				else{
//					if(table._logic.is_yao_card(table._logic.switch_to_card_data(i))){
//						table.GRR._cannot_out_index[j][i]++;
//					}
				}
			}
		
			for(int i = 0; i< an_long_count[j];i++)
			{
				int cbWeaveIndex = table.GRR._weave_count[j];
				table.GRR._weave_items[j][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[j][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[j][i]);
				table.GRR._weave_items[j][cbWeaveIndex].weave_kind = GameConstants.WIK_BH_ZHUA_LONG;
				table.GRR._weave_items[j][cbWeaveIndex].provide_player = _seat_index;
				Arrays.fill(table.GRR._weave_items[j][cbWeaveIndex].weave_card,0);
				for(int card_index = 0; card_index < 4;card_index++){
					table.GRR._weave_items[j][cbWeaveIndex].weave_card[card_index] = table._logic.switch_to_card_data(an_long_Index[j][i])|GameConstants.WIK_BH_ZHUA_LONG;
				}
				table.GRR._weave_items[j][cbWeaveIndex].weave_card_count  = 4;
				table.GRR._weave_count[j]++;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[j], table.GRR._weave_count[j]-1, table.GRR._weave_count[j]);
				// 删除手上的牌
				table.GRR._cards_index[j][an_long_Index[j][i]] = 0;
				
				table.GRR._card_count[j] = table._logic.get_card_count_by_index(table.GRR._cards_index[j]);

					
			}
			for(int i = 0; i< an_she_count[j];i++)
			{
				int cbWeaveIndex = table.GRR._weave_count[j];
				table.GRR._weave_items[j][cbWeaveIndex].public_card = 1;
				if(table._logic.switch_to_card_data(an_she_index[j][i]) == _send_card_data&&j==_seat_index)
					zhua_long_index = cbWeaveIndex;
				table.GRR._weave_items[j][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_she_index[j][i]);
				table.GRR._weave_items[j][cbWeaveIndex].weave_kind = GameConstants.WIK_BH_SHE;
				table.GRR._weave_items[j][cbWeaveIndex].provide_player = _seat_index;
				Arrays.fill(table.GRR._weave_items[j][cbWeaveIndex].weave_card,0);
				for(int card_index = 0; card_index < 3;card_index++){
					table.GRR._weave_items[j][cbWeaveIndex].weave_card[card_index] = table._logic.switch_to_card_data(an_she_index[j][i])|GameConstants.WIK_BH_SHE;
				}
				table.GRR._weave_items[j][cbWeaveIndex].weave_card_count  = 3;
				table.GRR._weave_count[j]++;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[j], table.GRR._weave_count[j]-1, table.GRR._weave_count[j]);
				// 删除手上的牌
				table.GRR._cards_index[j][an_she_index[j][i]] = 0;
				
				table.GRR._card_count[j] = table._logic.get_card_count_by_index(table.GRR._cards_index[j]);

					
			}
			
		}
		for(int i = 0 ;i< table.getTablePlayerNumber();i++)
	    {
			table.operate_cannot_card(i,true);
	    	hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
			table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i],
					table.GRR._weave_items[i]);

	    }
		ChiHuRight chr = new ChiHuRight(); 
		chr = table.GRR._chi_hu_rights[_seat_index];

		
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int bHupai = 0;
			

		int action_hu = GameConstants.WIK_BH_NULL;
		int card_data = table._send_card_data;
		if(ti_send_card == true)
		{
			card_data = 0;
		}
		PlayerStatus tempPlayerStatus = table._playerStatus[_seat_index]; 
		tempPlayerStatus.reset();
		int hu_xi[] = new int[1];
//		action_hu = table.estimate_player_hu_pai(_seat_index, _send_card_data, true);
		action_hu =	table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],_seat_index,_seat_index,card_data, chr, card_type,hu_xi,true);// 自摸
		
		if (action_hu!= GameConstants.WIK_NULL) {
			// 添加动作
			tempPlayerStatus.add_action(GameConstants.WIK_BH_ZI_MO);
			tempPlayerStatus.add_zi_mo(table._send_card_data, _seat_index);
//			table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false);
//			GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,_send_card_data),
//					600, TimeUnit.MILLISECONDS);
//			return ;
//			tempPlayerStatus.add_action(GameConstants.WIK_NULL);
//			tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
			table.operate_player_action(_seat_index, false);
			bHupai = 1;
			return ;
		} else {
			chr.set_empty();
		}
		
	     if(zhua_long_index != -1)
		{
	    	 int cbWeaveIndex = zhua_long_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._send_card_data;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_BH_ZHUA_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				Arrays.fill(table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card,0);
				for(int card_index = 0; card_index < 4;card_index++){
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[card_index] = table._send_card_data|GameConstants.WIK_BH_ZHUA_LONG;
				}
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card_count  = 4;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]-1, table.GRR._weave_count[_seat_index]);
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)] = 0;
				
				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
				ti_send_card = true;
		}
	    if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)] == 2)
		{
	    	 int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._send_card_data;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_BH_SHE;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				Arrays.fill(table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card,0);
				for(int card_index = 0; card_index < 3;card_index++){
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card[card_index] = table._send_card_data|GameConstants.WIK_BH_SHE;
				}
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_card_count  = 3;
				table.GRR._weave_count[_seat_index]++;
				table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index]-1, table.GRR._weave_count[_seat_index]);
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)] = 0;
				
				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
				ti_send_card = true;
		}
		if(bHupai == 0){
			
			
	
			// 加到手牌
			if(ti_send_card == false)
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
			_send_card_data = 0;
		    hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			// 发牌
			
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
	public boolean handler_operate_card(XPBHTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("DispatchCard 没有这个操作:" + operate_code);
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
		//	table.log_info("DispatchCard 不是当前玩家操作");
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


		if(luoCode != -1)
			playerStatus.set_lou_pai_kind(luoCode);
	

		

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
					cbUserActionRank = table._logic.get_action_rank(table.GRR._weave_items[i],table.GRR._weave_count[i],table._playerStatus[i].get_perform())
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank(table.GRR._weave_items[i],table.GRR._weave_count[i],table._playerStatus[i]._action_count,
							table._playerStatus[i]._action) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank(table.GRR._weave_items[target_player],table.GRR._weave_count[target_player],table._playerStatus[target_player].get_perform())
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank(table.GRR._weave_items[target_player],table.GRR._weave_count[target_player],
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
		//判断可不可以吃的上家用户
		int last_player = (target_player + 3+1)%3;
		
		
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_NULL:
		{
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			boolean is_an_long = false;
			for(int i = 0; i < table.GRR._weave_count[_seat_index];i++)
			{
				if( table.GRR._weave_items[_seat_index][i].center_card == table._send_card_data)
					is_an_long = true;
			}
			if(is_an_long == false)
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
			if(table._long_count[_seat_index] > 0 )
			{
				int _action = GameConstants.WIK_AN_LONG;
				//效果
				table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
				
				// 刷新手牌包括组合
				int cards[]= new int[GameConstants.XPBH_MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);

			}
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				int pai_count =0;
				for(int i = 0; i<GameConstants.XPBH_MAX_INDEX ;i++) {
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
				int cards[]= new int[GameConstants.XPBH_MAX_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
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

			if(table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI) == false)
				table.operate_player_get_card(_seat_index, 1, new int[] { table._send_card_data }, GameConstants.INVALID_SEAT,false);
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_bh(target_player, _seat_index, operate_card, true);

			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}
			table.countChiHuTimes(target_player, true,_seat_index);

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
	public boolean handler_player_be_in_room(XPBHTable table, int seat_index) {
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
