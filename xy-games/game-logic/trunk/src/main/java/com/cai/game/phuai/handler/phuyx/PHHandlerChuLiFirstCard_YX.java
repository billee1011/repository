/**
 * 
 */
package com.cai.game.phuai.handler.phuyx;

import java.util.Arrays;

/**
 * @author xwy
 *
 */

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.SysParamModel;
import com.cai.dictionary.SysGameTypeDict;
import com.cai.dictionary.SysParamServerDict;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.HuPaiRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phuai.PHAITable;
import com.cai.game.phuai.handler.PHHandlerDispatchCard;

public class PHHandlerChuLiFirstCard_YX extends PHHandlerDispatchCard<PHAITable> {

	@Override
	public void exe(PHAITable table) {
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
//		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
//		table._send_card_data = 0;
//		_send_card_data = 0;
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		table._current_player = _seat_index;// 轮到操作的人是自己

		
		boolean ti_send_card = false;
		int  an_long_Index[][] = new int [table.getTablePlayerNumber()][5];
		int  an_long_count[] = new int[table.getTablePlayerNumber()];
		Arrays.fill(an_long_count, 0);
		int an_sao_index[][] = new int [table.getTablePlayerNumber()][5];
		int an_sao_count[] = new int[table.getTablePlayerNumber()];
		Arrays.fill(an_sao_count, 0);
		boolean is_warn = false;
		int peng_sao_count[][] = new int[table.getTablePlayerNumber()][1];
		for(int j = 0; j<table.getTablePlayerNumber();j++)
		{
			for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++)
			{
				if(table.GRR._cards_index[j][i] == 4)
				{
					
					an_long_Index[j][an_long_count[j]++] = i;
							
				}
				if(table.GRR._cards_index[j][i] == 3)
				{
					if(_seat_index == j && table._logic.switch_to_card_index(this._send_card_data) == i)
					{
						ti_send_card = true;
						an_long_Index[j][an_long_count[j]++] = i;
					}
					else
						an_sao_index[j][an_sao_count[j]++] = i;
							
				}
				if(table.GRR._cards_index[j][i] == 2)
				{
					if(_seat_index == j && table._logic.switch_to_card_index(this._send_card_data) == i)
					{
						ti_send_card = true;
						an_sao_index[j][an_sao_count[j]++] = i;
					}
				}
			}
		}
	    for(int j = 0; j<table.getTablePlayerNumber();j++)
	    {
	    	
	    	
			
			if(an_long_count[j] > 0 )
			{			
				for(int i = 0; i< an_long_count[j];i++)
				{
					int cbWeaveIndex = table.GRR._weave_count[j];
					table.GRR._weave_items[j][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[j][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_long_Index[j][i]);
					table.GRR._weave_items[j][cbWeaveIndex].weave_kind = GameConstants.WIK_AN_LONG;
					table.GRR._weave_items[j][cbWeaveIndex].provide_player = j;
					int score[] = new int[table.getTablePlayerNumber()];
                    Arrays.fill(score, 0);
                    int temp_score = table._logic.get_weave_hu_fen(table.GRR._weave_items[j], table.GRR._weave_count[j], table.GRR._weave_items[j][cbWeaveIndex].weave_kind,peng_sao_count[j]);
                    
                    table._logic.calculate_game_mid_score(j,j,table.getTablePlayerNumber(),true,score,temp_score);
                    table._logic.calculate_game_weave_score(table._game_mid_score,table._game_weave_score,score,table.getTablePlayerNumber(),true);
					table.GRR._weave_count[j]++;
					table._long_count[j]++;
					// 删除手上的牌
					table.GRR._cards_index[j][an_long_Index[j][i]] = 0;
					
					table.GRR._card_count[j] = table._logic.get_card_count_by_index(table.GRR._cards_index[j]);
						
				}
				

			}
			if(an_sao_count[j] > 0)
			{

				for(int i = 0; i< an_sao_count[j];i++)
				{
					int cbWeaveIndex = table.GRR._weave_count[j];
					table.GRR._weave_items[j][cbWeaveIndex].public_card = 1;
					table.GRR._weave_items[j][cbWeaveIndex].center_card = table._logic.switch_to_card_data(an_sao_index[j][i]);
					table.GRR._weave_items[j][cbWeaveIndex].weave_kind = GameConstants.WIK_SAO;
					table.GRR._weave_items[j][cbWeaveIndex].provide_player = j;
				    table.GRR._weave_count[j]++;
                    int temp_score = table._logic.get_weave_hu_fen(table.GRR._weave_items[j], table.GRR._weave_count[j], table.GRR._weave_items[j][cbWeaveIndex].weave_kind,peng_sao_count[j]);
            		int score[] = new int[table.getTablePlayerNumber()];
                    Arrays.fill(score, 0);
                    table._logic.calculate_game_mid_score(j,j,table.getTablePlayerNumber(),true,score,temp_score);
                    table._logic.calculate_game_weave_score(table._game_mid_score,table._game_weave_score,score,table.getTablePlayerNumber(),true);
					
                   
                   
					// 删除手上的牌
					table.GRR._cards_index[j][an_sao_index[j][i]] = 0;
					
					table.GRR._card_count[j] = table._logic.get_card_count_by_index(table.GRR._cards_index[j]);
					table._is_first_sao[j] = true;	
				}
				
			}

	    }
	    boolean temp_score= false;
//	    if(ti_send_card == true)
//	    {
//	    	table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false,1);
//	    }
//	    else 
//	    {
//	    	table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]++;
//	    	table.operate_player_get_card(_seat_index, 1, new int[] { _send_card_data }, GameConstants.INVALID_SEAT,false,0);
//	    }
	    for(int i = 0 ;i< table.getTablePlayerNumber();i++)
	    {
	    	hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);
			table.operate_player_cards(i, hand_card_count, cards, table.GRR._weave_count[i],
					table.GRR._weave_items[i],1);
			if(table._game_mid_score[i]!=0)
				temp_score = true;
//			if(an_long_count[i] +an_sao_count[i]  > 0 )
//			 table.operate_effect_action(i, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { GameConstants.WIK_SAO }, 1,
//						GameConstants.INVALID_SEAT);
	    }
	  
	    if(temp_score ==  true)
	    {
			//效果
			table.operate_game_mid_score(true);
	
	    }
	    for(int j = 0; j<table.getTablePlayerNumber();j++ )
	    {
 	    	  table._is_first_sao[j] = false;	
	    	if(table._logic.is_si_qing(table.GRR._cards_index[j],table.GRR._weave_items[j],table.GRR._weave_count[j],table._warning[j]))
	 		{
	 			table._warning[j] = 1;
	 			is_warn = true;
	 			table.operate_is_warning(j,true);
	 			
	 			
	 		}
	    }
	   
	    if(is_warn == true)
	    {
	    	 table.set_timer(GameConstants.PH_WARING , 5, true);
	    	return ;
	    }
	    // 刷新手牌包括组合
		
//	    ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
//		chr.set_empty();
//		int action = 0;
//		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
//		if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(table._send_card_data)]!=0)
//		{
//			int hu_xi[] = new int [1];
//			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
//					table.GRR._weave_count[_seat_index],_seat_index,_seat_index,0, chr, card_type,hu_xi,true);// 自摸
//		}
//		else
//		{
//			int hu_xi[] = new int [1];
////			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
//			action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
//					table.GRR._weave_count[_seat_index],_seat_index,_seat_index, 0, chr, card_type,hu_xi,true);// 自摸
//		}
//		
//		if (action != GameConstants.WIK_NULL) {
//			// 添加动作
//			table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
//			table._playerStatus[_seat_index].add_zi_mo(table._send_card_data, _seat_index);
//			if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON))
//			{
//				GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,table._send_card_data),
//						600, TimeUnit.MILLISECONDS);
//				return ;
//			}
//			if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON)== false){
//				table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
//				table._playerStatus[_seat_index].add_pass(table._send_card_data, _seat_index);
//			}
//			//发 操作
//			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
//			table.operate_player_action(_seat_index,false);
//			table.set_timer(GameConstants.PH_OPERATE_CARD , 5, true);
//			
//
//		} else
//		{
//			chr.set_empty();
			// 加到手牌
			table._banker_first_out_card = true;
//		    hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
//			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
			// 发牌
			
//		}
		
		return;
	}
	public boolean handler_ask_player(PHAITable table, int seat_index,boolean  is_ask){
			
			
		if(table._warning[seat_index] != 1)
		{
			table.log_error("HHHandlerChiPeng_YX 没有这个操作:"+is_ask +table._warning[seat_index]);
			return false;
		}
		if(is_ask == true)
		{
			table._warning[seat_index] = 2;
			table.operate_player_xiang_gong_flag(seat_index,true);
			int temp_index = 0;
			int card = temp_index>=10?(table._logic.switch_to_card_data(temp_index))%16 :(table._logic.switch_to_card_data(temp_index))+16;
			if(table._logic.is_card_to_weave(card,table.GRR._weave_items[seat_index],table.GRR._weave_count[seat_index]))
			{
				table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = card;
			}
		}
		else
		{
			table._warning[seat_index] = 3;
		}
		table.operate_is_warning(seat_index,true);
		boolean flag = false;
		for(int i =0; i<table.getTablePlayerNumber();i++)
		{
			if(table._warning[i] == 1)
				flag = true;
		}
		if(flag == true)
			return true;
		// 刷新手牌包括组合
		
	    ChiHuRight chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int action = 0;
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		
		int hu_xi[] = new int [1];
//			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
		action = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],_seat_index,_seat_index, 0, chr, card_type,hu_xi,true);// 自摸
	
		
		if (action != GameConstants.WIK_NULL) {
			// 添加动作
			table._playerStatus[_seat_index].add_action(GameConstants.WIK_ZI_MO);
			table._playerStatus[_seat_index].add_zi_mo(table._send_card_data, _seat_index);
			table._playerStatus[_seat_index].set_operate_card(table._send_card_data);
			if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON))
			{
				SysParamModel sysParamModel13 = SysParamServerDict.getInstance()
						.getSysParamModelDictionaryByGameId(SysGameTypeDict.getInstance().getGameIDByTypeIndex(table._game_type_index)).get(13);
				int dalay = 1000;
				if(sysParamModel13!=null&&sysParamModel13.getVal5() > 0 && sysParamModel13.getVal5() <5000)
					dalay = sysParamModel13.getVal5() ;
				GameSchedule.put(new HuPaiRunnable(table.getRoom_id(), _seat_index, GameConstants.WIK_ZI_MO,table._send_card_data),
						dalay, TimeUnit.MILLISECONDS);
				return true;
			}
			if(table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON)== false){
				table._playerStatus[_seat_index].add_action(GameConstants.WIK_NULL);
				table._playerStatus[_seat_index].add_pass(table._send_card_data, _seat_index);
				table._playerStatus[_seat_index].set_operate_card(table._send_card_data);
			}
			//发 操作
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
			table.set_timer(GameConstants.PH_OPERATE_CARD , 5, true);

		} else {
			chr.set_empty();
			// 加到手牌
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
			table._banker_first_out_card = true;
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
//			table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
			curPlayerStatus.set_operate_card(table._send_card_data);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
			
			// 发牌
			
		}
		return true;
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
	public boolean handler_operate_card(PHAITable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
//		table.log_error(_seat_index+"  " + table._current_player +"  " +"下次 出牌用户" +seat_index+"操作用户" );
		// 效验操作
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);

//		if(playerStatus.has_zi_mo() == true)
//		{
//			table._guo_hu_pai_cards[seat_index][table._guo_hu_pai_count[seat_index]++] = operate_card;
//			table._guo_hu_xt[seat_index]  = seat_index;
//		}
		
	
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
		if(operate_code == GameConstants.WIK_NULL){
			if(playerStatus.has_zi_mo() == true&&table.has_rule(GameConstants.GAME_RULE_QZ_HU_PAI_ON))
			{
				table.log_player_error(seat_index,"强制胡牌操作空操作无效");
				return true;
			}
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
//			table.log_error("最用户操作");
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
		table.operate_effect_action(target_player,GameConstants.EFFECT_ACTTON_CACEL_DISPLAY_CARD, 1, new long[]{target_action}, _seat_index,GameConstants.INVALID_SEAT);
		switch (target_action) {
		case GameConstants.WIK_NULL:
		{
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
		
			if (table._playerStatus[_seat_index].lock_huan_zhang()) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, table._send_card_data),
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
					table.log_error(next_player + "可以胡，而不胡的情况 "+_seat_index);
					return true;
				}
//				table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
				int cards[]= new int[GameConstants.MAX_HH_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index],cards);
				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
				PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
				curPlayerStatus.reset();
				curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
				table.operate_player_status();
				table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
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
			if(table.has_rule(GameConstants.GAME_RULE_NO_XING) != true)
				table.set_niao_card(target_player,GameConstants.INVALID_VALUE,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
			table.process_chi_hu_player_score_ph(target_player, _seat_index, operate_card, true);
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			

		
			table.countChiHuTimes_ph(target_player, _seat_index, true);

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
	public boolean handler_player_be_in_room(PHAITable table, int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
	
		return true;
	}
}
