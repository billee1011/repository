/**
 * 
 */
package com.cai.game.hh.handler.thkhy;

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
import com.cai.game.hh.HHTable;
import com.cai.game.hh.handler.HHHandlerDispatchCard;

public class THKHandlerChuLiFirstCard_HY extends HHHandlerDispatchCard<HHTable> {

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
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);

			return;
		}
		
		
		
		//刷新手牌包括组合
//		_send_card_data = table._send_card_data;
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		int  an_long_Index[] = new int [5];
		int  an_long_count = 0;
		boolean ti_send_card = false;
		boolean ti_sao_card = false;
		//// 玩家出牌 响应判断,是否有提 暗龙
	
			
		
		// 发牌处理,判断发给的这个人有没有胡牌或杠牌
		// 胡牌判断
		if(table.has_rule(GameConstants.GAME_RULE_DI_ERZI_LIANG_PAI)){
			// 发牌处理,判断发给的这个人有没有胡牌或杠牌
			// 胡牌判断
			for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++)
			{
				if(table.GRR._cards_index[_seat_index][i] == 4)
				{
					an_long_Index[an_long_count++] = i;	
					if(i == table._logic.switch_to_card_index(table._send_card_data))
						ti_send_card = true;
				}
		
			}
			for(int i = 0; i< an_long_count;i++)
			{
				if(table._logic.switch_to_card_index(table._send_card_data) == an_long_Index[i])
					continue;
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
				
				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
				
			}	
			ChiHuRight chr[] = new ChiHuRight[table.getTablePlayerNumber()]; 
			for(int i = 0; i< table.getTablePlayerNumber();i++)
			{
				chr[i] = table.GRR._chi_hu_rights[i];
				chr[i].set_empty();
			}
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			int bHupai = 0;
				

			int action_hu[] = new int[table.getTablePlayerNumber()];
			
			if(an_long_count >= 2)
			{
				table._ti_mul_long[_seat_index] = an_long_count - 1 ; 
			}
			int loop = 0;
			while(loop < table.getTablePlayerNumber()){
				int i = (table._current_player+loop) %table.getTablePlayerNumber() ; 
				loop ++;
				int card_data = table._send_card_data;
				if(table._current_player == i)
					card_data = 0;
				if(table.getRuleValue(GameConstants.GAME_RULE_THK_KHLZ_CARD)!=1 && (i != table._cur_banker))
					continue;
//				else if(ti_send_card == true||ti_sao_card==true)
//					continue;
				PlayerStatus tempPlayerStatus = table._playerStatus[i]; 
				tempPlayerStatus.reset();
				int hu_xi[] = new int[1];
				action_hu[i] = table.analyse_chi_hu_card(table.GRR._cards_index[i], table.GRR._weave_items[i],
						table.GRR._weave_count[i],i,_seat_index,card_data, chr[i], card_type,hu_xi,true);// 自摸
				if (action_hu[i] != GameConstants.WIK_NULL) {
					// 添加动作
					if(i == _seat_index)
					{
						tempPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
						tempPlayerStatus.add_zi_mo(table._send_card_data, i);
					}
					else{
						tempPlayerStatus.add_action(GameConstants.WIK_CHI_HU);
						tempPlayerStatus.add_chi_hu(table._send_card_data, _seat_index);
						chr[i].opr_or(GameConstants.CHR_DIAN_PAO_HU);
					}
					int all_hu_xi = 0;
					for (int j = 0; j < table._hu_weave_count[i]; j++) {
						all_hu_xi += table._hu_weave_items[i][j].hu_xi;
					}
					    table._guo_hu_hu_xi[i][table._logic.switch_to_card_index(table._send_card_data)] = all_hu_xi;
					if(table.has_rule(GameConstants.GAME_RULE_QIANG_ZHI_HU_PAI)== false)
					{
						tempPlayerStatus.add_action(GameConstants.WIK_NULL);
						tempPlayerStatus.add_pass(table._send_card_data, _seat_index);
					}
					if(table.has_rule(GameConstants.GAME_RULE_QIANG_ZHI_HU_PAI))
					{
						if(i == _seat_index)
						{
							int hucard_time = 600;
							int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
							SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
									.get(1105);
							if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
								hucard_time = sysParamModel1105.getVal4();
							}
							GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), i, GameConstants.WIK_ZI_MO,table._send_card_data),
									hucard_time, TimeUnit.MILLISECONDS);
						}
						else
						{
							int hucard_time = 600;
							int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
							SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
									.get(1105);
							if (sysParamModel1105 != null && sysParamModel1105.getVal4() > 0 && sysParamModel1105.getVal4() < 10000) {
								hucard_time = sysParamModel1105.getVal4();
							}
							GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), i, GameConstants.WIK_CHI_HU,table._send_card_data),
									hucard_time, TimeUnit.MILLISECONDS);
						}
						
						return ;
					}
					table.operate_player_action(i, false);
					bHupai = 1;
				} else {
					chr[i].set_empty();
				}
				
			}	
			if(bHupai == 0){
				for(int i = 0; i< an_long_count;i++)
				{
					if(table._logic.switch_to_card_index(table._send_card_data) != an_long_Index[i])
						continue;
					int cbWeaveIndex = table.GRR._weave_count[_seat_index];
					table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
					table.GRR._weave_count[_seat_index]++;
					table._long_count[_seat_index]++;
					// 删除手上的牌
					table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
					
					table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
					
				}
				if(an_long_count > 0 )
				{
					int _action = GameConstants.WIK_AN_LONG;
					//效果
					table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
				
					
					// 刷新手牌包括组合
				
					hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
					table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
							table.GRR._weave_items[_seat_index]);
		
				}
		
				// 加到手牌
				if(an_long_count>=2)
				{
					table._ti_mul_long[_seat_index] --;
					// 用户切换
				
					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
//					table.operate_remove_discard(table._current_player, table.GRR._discard_count[table._current_player]);
					//没有人要就加入到牌堆
					int discard_time = 2000;
					int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
					SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
							.get(1104);
					if (sysParamModel1104 != null && sysParamModel1104.getVal4() > 0 && sysParamModel1104.getVal4() < 10000) {
						discard_time = sysParamModel1104.getVal4();
					}
					
					if(table._last_card != 0)
					     table.exe_add_discard( _seat_index,  1, new int[]{table._last_card },true,discard_time);
					
					int next_player=table._current_player = (_seat_index + table.getTablePlayerNumber() + 1)
							% table.getTablePlayerNumber();
					_seat_index = 0;
					table._last_player = _seat_index;
					int dispatch_time = 3000;
					SysParamModel sysParamModel1105 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
							.get(1105);
					if (sysParamModel1105 != null && sysParamModel1105.getVal2() > 0 && sysParamModel1105.getVal2() < 10000) {
						dispatch_time = sysParamModel1105.getVal2();
					}
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, dispatch_time);
				}
				else
				{
					hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
					table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
					table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
					curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
					table.operate_player_status();
				}
			    
				// 发牌
				
			}

		}
		else{
			for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++)
			{
				if(table.GRR._cards_index[_seat_index][i] == 4)
				{
					an_long_Index[an_long_count++] = i;
					
							
				}
			}
			if(an_long_count > 0 )
			{
				int _action = GameConstants.WIK_AN_LONG;
				//效果
				table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
			
				for(int i = 0; i< an_long_count;i++)
				{
					int cbWeaveIndex = table.GRR._weave_count[_seat_index];
					table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[i]);
					table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
					table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
					table.GRR._weave_count[_seat_index]++;
					table._long_count[_seat_index]++;
					// 删除手上的牌
					table.GRR._cards_index[_seat_index][an_long_Index[i]] = 0;
					
					table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
					
				}
				// 刷新手牌包括组合
			
				hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
						table.GRR._weave_items[_seat_index]);
	
			}
			if(an_long_count >= 2)
			{
				table._ti_mul_long[_seat_index] = an_long_count - 1 ; 
			}
			ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
			chr.set_empty();
			int action = 0;
			int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
			if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]!=0)
			{
				int hu_xi[] = new int [1];
				action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index],_seat_index,_seat_index,0, chr, card_type,hu_xi,true);// 自摸
			}
			else
			{
				int hu_xi[] = new int [1];
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
				action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
						table.GRR._weave_count[_seat_index],_seat_index,_seat_index, 0, chr, card_type,hu_xi,true);// 自摸
			}
			
			if (action != GameConstants.WIK_NULL) {
				// 添加动作
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
				table._playerStatus[_seat_index].add_zi_mo(table._send_card_data, _seat_index);
				if(table.has_rule(GameConstants.GAME_RULE_QIANG_ZHI_HU_PAI)== false){
					table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
					table._playerStatus[_seat_index].add_pass(table._send_card_data, _seat_index);
				}
				//发 操作
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(_seat_index,false);
				
	
			} else {
				chr.set_empty();
				// 加到手牌
				
			    hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.operate_player_status();
				// 发牌
				
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
	public boolean handler_operate_card(HHTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_info("DispatchCard :" + operate_code);
			return false;
		}
	
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);	
		if(operate_code == GameConstants.WIK_NULL){
			if(playerStatus.has_zi_mo() == true)
				table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
		}
		else{
			table._guo_hu_xt[seat_index] = -1;
			table._guo_hu_pai_count[seat_index] = 0;
		}
		// 是否已经响应
		if (playerStatus.has_action() == false) {
			table.log_player_error(seat_index, "DispatchCard has_action");
			return true;
		}
		//if (seat_index != _seat_index) {
		//	table.log_info("DispatchCard 不是当前玩家操作");
		//	return false;
		//}
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard is_respone");
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
			table.log_info("target_player ");
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
			if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)] == 4)
			{
				int cbWeaveIndex = table.GRR._weave_count[_seat_index];
				table.GRR._weave_items[_seat_index][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].center_card = table._send_card_data;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].provide_player = _seat_index;
				table.GRR._weave_items[_seat_index][cbWeaveIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][cbWeaveIndex]);
				table.GRR._weave_count[_seat_index]++;
				table._long_count[_seat_index]++;
				// 删除手上的牌
				table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)] = 0;
				
				table.GRR._card_count[_seat_index] = table._logic.get_card_count_by_index(table.GRR._cards_index[_seat_index]);
				int cards[]= new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
			}
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
						GameConstants.DELAY_AUTO_OUT_CARD, TimeUnit.MILLISECONDS);
			} else {
				//要出牌，但是没有牌出设置成相公  下家用户发牌
				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
				int pai_count =0;
				for(int i = 0; i<GameConstants.MAX_HH_INDEX ;i++) {
			       	if(table.GRR._cards_index[_seat_index][i]<3)
			       		pai_count += table.GRR._cards_index[_seat_index][i];
				 }
				if(pai_count == 0||table.GRR._weave_count[_seat_index]>1)
				{
					if(table.GRR._weave_count[_seat_index]>1)
					{
						table._ti_mul_long[_seat_index] --;
					}
					if(pai_count == 0)
					{
						table._is_xiang_gong[_seat_index] = true;	 	
			        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
			        
					}
					int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
					// 用户状态
					table._playerStatus[_seat_index].clean_action();
					table._playerStatus[_seat_index].clean_status();
					table._current_player = next_player;
					table._last_player = next_player;
					
					table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
					return true;
				}
				
				int cards[]= new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.operate_player_status();
			}
			return true;

		}
		case GameConstants.WIK_CHI_HU: // 胡
		{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				if (i == target_player) {
					table.GRR._chi_hu_rights[i].set_valid(true);
				} else {
					table.GRR._chi_hu_rights[i].set_valid(false);
				}
			}
			table._cur_banker = target_player;
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[target_player][0] = operate_card;
			if(table.has_rule(GameConstants.GAME_RULE_NO_XING) != true)
				table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, false);
			table.process_chi_hu_player_score_thk(target_player, _seat_index, operate_card, false);

		
			table.countChiHuTimes(target_player, false);

			
			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay+=table.GRR._chi_hu_rights[target_player].type_count-2;
			}
			GameSchedule.put(
					new GameFinishRunnable(table.getRoom_id(), table._cur_banker, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

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
			table.GRR._chi_hu_card[seat_index][0] = operate_card;
			if(table.has_rule(GameConstants.GAME_RULE_NO_XING) != true)
				table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_thk(target_player, _seat_index, operate_card, true);

		
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
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;

		if (ting_count > 0) {
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}