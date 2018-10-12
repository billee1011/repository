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
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.game.schcpdz.SCHCPDZTable;
import com.cai.game.schcpdz.handler.SCHCPDZHandlerDispatchCard;



public class SCHCPHandlerChuLiFirstCard_YDR extends SCHCPDZHandlerDispatchCard<SCHCPDZTable> {

	@Override
	public void exe(SCHCPDZTable table) {
		// 用户状态
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
			table._playerStatus[i].clean_action();
			table._playerStatus[i].clean_status();
		}

		table._playerStatus[_seat_index].chi_hu_round_valid();// 可以胡了
		if(table.GRR == null)
			return ;
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
		
		
		
		//刷新手牌包括组合
//		_send_card_data = table._send_card_data;

		
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
    
		
	
		// 加到手牌
		int next_player = _seat_index;
		while(table._qi_yang_count < table.getTablePlayerNumber()){
			int tuo_shu = table.caculate_tuo_shu(next_player);
			int mao_shu = table.caculate_mao_shu(next_player);
			boolean is_qi = false;
			if(table._is_qi == false){
				for(int i = 0; i<GameConstants.MAX_CP_INDEX;i++){
					if(table.GRR._cards_index[next_player][i] >=2 )
						if(table._logic.get_times_cards_ydr(table._logic.switch_to_card_data(i)) == 1)
						{
							is_qi = true;
							break;
						}
						else if(table.GRR._cards_index[next_player][i]>=4){
							is_qi = true;
							break;
						}
				}
					
			}
			if( mao_shu >=7&&((table._cur_banker == next_player  && tuo_shu >= 34)||(table._cur_banker != next_player  && tuo_shu >= 33)))
			{
				if(is_qi==true){
					table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_QI);
					table._playerStatus[next_player].add_qi(0, GameConstants.DZ_WIK_QI , next_player);
					table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_YANG);
					table._playerStatus[next_player].add_yang(0, GameConstants.DZ_WIK_YANG , next_player);
					table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_NULL);
					table._playerStatus[next_player].add_pass(0, next_player);
				}
				else{
					table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_YANG);
					table._playerStatus[next_player].add_yang(0, GameConstants.DZ_WIK_YANG , next_player);
					table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_NULL);
					table._playerStatus[next_player].add_pass(0, next_player);
				}
				table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(next_player, false);
				return ;
			}
			else{
				table._playerStatus[next_player].add_action(GameConstants.DZ_WIK_YANG);
				table._playerStatus[next_player].add_yang(0, GameConstants.DZ_WIK_YANG , next_player);
				table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(next_player, false);
				return ;
			}
//			next_player = (next_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//			table._current_player = next_player;
//			table._qi_yang_count ++;
		}
		boolean flag = false;
		for(int i = 0; i< table.getTablePlayerNumber();i++){
			if(table._is_yang[i] == false)
			{
				flag = true;
				break;
			}
		}
		if(table._is_qi == false)
			flag = false;
		if(flag == false)
		{
			table._cur_round--;
			table.is_record = false;
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				table.GRR._chi_hu_card[i][0] = GameConstants.INVALID_VALUE;
			}
			// 显示胡牌
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
				int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[i], cards);

				table.operate_show_card(i, GameConstants.Show_Card_HU, hand_card_count, cards, table.GRR._weave_items[i],
						table.GRR._weave_count[i], GameConstants.INVALID_SEAT);

			}
			table._cur_banker = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
					% table.getTablePlayerNumber();
			
			table._shang_zhuang_player = GameConstants.INVALID_SEAT;
			// 流局
			table.handler_game_finish(table._cur_banker, GameConstants.Game_End_DRAW);
			return ;
		}		
		table._is_start = true;
		for(int i = 0; i < table.getTablePlayerNumber();i++)
		{
			if(table._is_yang[i] == true)
				continue;
//			table.operate_effect_action(i, GameConstants.EFFECT_ACTION_CP, 1, new long[] { GameConstants.CP_DISPLAY_KOU }, 1,
//					i);
		}
		if(table.is_zhang_zhua_pai == true && _seat_index == table._cur_banker&&table.get_is_kou_player(table._cur_banker) == false)
		{
			table.is_zhang_zhua_pai = false;
			table.exe_dispatch_add_card(table._current_player, GameConstants.WIK_NULL, 0);
			return ;
		}
		next_player = _seat_index;
		while(table._guo_peng_count < table.getTablePlayerNumber())
		{
			int count = 0;
			if(table._ti_mul_long[next_player] > 0){
				table.exe_dispatch_add_card(next_player);
				return ;
			}
			if(table._ti_mul_long[next_player] == 0)
			{
				count = table.estimate_player_tu_huo(next_player);
				if(count == 0)
				{
					count = table.estimate_player_ming_tu_huo(next_player);
						
				}
			}
			table._ti_mul_long[next_player] += count;
			if(table._ti_mul_long[next_player] > 0)
			{
				table._playerStatus[next_player].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(next_player, false);
		
				return ;
			}
			next_player = table.get_cur_index(next_player);
			table._current_player = next_player;
			table._guo_peng_count ++;
		}
		Arrays.fill(table._is_sha_index, -1);
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
		
		
		_seat_index = table._cur_banker;
		
//      int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
//		SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
//				.get(1104);
//		
//		//延时5秒发牌
//		int dispatch_time = 3000;
//		if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
//			dispatch_time = sysParamModel1104.getVal5();
//		}
        if(table._is_yang[table._cur_banker] == true)
        {
        	table._current_player = table.get_cur_index(table._cur_banker);
        	table.exe_dispatch_card(table._current_player, GameConstants.WIK_NULL, 0);
        	
        }
        else
        {
        	table._playerStatus[table._cur_banker].set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
        }
        	
		
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
	public boolean handler_operate_card(SCHCPDZTable table, int seat_index, int operate_code, int operate_card,int luoCode) {
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		// 效验操作
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		if ((operate_code != GameConstants.WIK_NULL) && (playerStatus.has_action_by_code(operate_code) == false)) {
			table.log_error("DispatchCard 没有这个操作:" + operate_code);
			return false;
		}
		

		if(playerStatus.has_zi_mo() == true)
			table._guo_hu_pai_cards[seat_index][table._logic.switch_to_card_index(operate_card)] ++;
	
	
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
		case GameConstants.WIK_NULL:
		{

			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	          
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
			// 用户状态
			if(table._playerStatus[target_player].has_zi_mo())
			{
				table._playerStatus[target_player].clean_action();
				table._playerStatus[target_player].clean_status();
				_seat_index = table._cur_banker;
				table._current_player = _seat_index;
		        table._is_display = true;
		        _send_card_data = 0;
		        
//				int gameId = table.getGame_id() == 0 ? 5 : table.getGame_id();
//				SysParamModel sysParamModel1104 = SysParamDict.getInstance().getSysParamModelDictionaryByGameId(gameId)
//						.get(1104);
//				
//				//延时5秒发牌
//				int dispatch_time = 3000;
//				if (sysParamModel1104 != null && sysParamModel1104.getVal5() > 0 && sysParamModel1104.getVal5() < 10000) {
//					dispatch_time = sysParamModel1104.getVal5();
//				}
				table.exe_dispatch_card(table._cur_banker, GameConstants.WIK_NULL, 0);
				return true ;
				
			}
			if(table._qi_yang_count < table.getTablePlayerNumber())
			{
				table._playerStatus[target_player].clean_action();
				table._playerStatus[target_player].clean_status();
				int next_player = table.get_cur_index(target_player);
				table._qi_yang_count ++;
				table._current_player = next_player;
				table.exe_chuli_first_card(next_player,GameConstants.WIK_NULL,0);
				return true;
			}
			else{
				
			}table._playerStatus[target_player].clean_action();
			table._playerStatus[target_player].clean_status();
			int next_player = table.get_cur_index(target_player);
			table._guo_peng_count ++;
			table._current_player = next_player;
			table.exe_chuli_first_card(next_player,GameConstants.WIK_NULL,0);
			
		
			
			return true;

		}
		case GameConstants.DZ_WIK_QI:{
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	          
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
			table._is_qi = true;
			table._qi_player = target_player;
			table.set_qi_player();
			target_player =table.get_cur_index(target_player);
			table._current_player = target_player;
			table._qi_yang_count ++;
			table.exe_chuli_first_card(target_player,GameConstants.WIK_NULL,0);
			return true;
		}
		case GameConstants.DZ_WIK_YANG:{
        for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	          
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
    		table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { target_action }, 1,
				GameConstants.INVALID_SEAT);
			table._is_yang[target_player] = true;
			table.operate_player_xiang_gong_flag(target_player, true);
			target_player = table.get_cur_index(target_player);
			table._current_player = target_player;
			table._qi_yang_count ++;
			table.exe_chuli_first_card(target_player,GameConstants.WIK_NULL,0);
			return true;
		}
		case GameConstants.DZ_WIK_TU_HUO:
		case GameConstants.DZ_WIK_MTU_HUO:{
			// 用户状态
			for (int i = 0; i < table.getTablePlayerNumber(); i++) {
	          
				
				table._playerStatus[i].clean_action();
				table._playerStatus[i].clean_status();

				table.operate_player_action(i, true);
			}
			Arrays.fill(table._is_sha_index, -1);
			if(target_action == GameConstants.DZ_WIK_TU_HUO){
				table._is_sha_index[target_player]=table._logic.switch_to_card_index(operate_card);
			}
			if(target_action == GameConstants.DZ_WIK_MTU_HUO){
				int cbWeaveIndex = -1;
				for(int i = 0; i< table.GRR._weave_count[target_player];i++){
					if(table.GRR._weave_items[target_player][i].weave_kind==GameConstants.DZ_WIK_TU_HUO 
							&& operate_card  == table.GRR._weave_items[target_player][i].center_card){
						cbWeaveIndex = i;
						break;
					}
				}
				if(cbWeaveIndex == -1)
					cbWeaveIndex = table.GRR._weave_count[target_player];
				table.GRR._weave_items[target_player][cbWeaveIndex].public_card = 1;
				table.GRR._weave_items[target_player][cbWeaveIndex].center_card =operate_card;
				table.GRR._weave_items[target_player][cbWeaveIndex].weave_kind = target_action;
				table.GRR._weave_items[target_player][cbWeaveIndex].provide_player = target_player;
				table.GRR._weave_items[target_player][cbWeaveIndex].hu_xi = table._logic.get_analyse_tuo_shu(target_action,
						operate_card);
				int cbCardIndex = table._logic.switch_to_card_index(operate_card);
		
				table.GRR._cards_index[target_player][cbCardIndex] = 0;
				if(cbWeaveIndex == table.GRR._weave_count[target_player])
					table.GRR._weave_count[target_player]++;
				// 效果
				table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { target_action }, 1,
						GameConstants.INVALID_SEAT);
				// 刷新手牌包括组合
				int cards[] = new int[GameConstants.DZ_MAX_CP_COUNT];
				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
				table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
						table.GRR._weave_items[target_player]);
			}
			else{
				// 效果
				table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { target_action }, 1,
						GameConstants.INVALID_SEAT);
				table._tu_huo_index[seat_index][table._logic.switch_to_card_index(operate_card)]++;
			}
			table.exe_dispatch_add_card(target_player);
			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			// 用户状态
			table._playerStatus[target_player].clean_action();
			table._playerStatus[target_player].clean_status();
					
			table.GRR._chi_hu_rights[target_player].set_valid(true);

//			if (table.has_rule(GameConstants.GAME_RULE_LIXIANG_FLS_ZHUANG)) {// 轮装
//				if (table.GRR._banker_player == target_player) {
//					table._banker_select = target_player;
//				} else {
//					table._banker_select = (table.GRR._banker_player + table.getTablePlayerNumber() + 1)
//							% table.getTablePlayerNumber();
//				}
//			}
		
			table._shang_zhuang_player = target_player;
			table.GRR._chi_hu_card[target_player][0] = operate_card;
			table._xing_player[target_player] = 1;
			table.set_niao_card(target_player,operate_card,true);// 结束后设置鸟牌
			table._shang_zhuang_player = target_player;
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
		int user_index = -1; 
		for(int i = 0; i<table.getTablePlayerNumber();i++)
		{
	        if(table._playerStatus[i].has_cp_tou())
	        {
	        	user_index = i ;
	        	break;
	        }
		}
		if(user_index!=-1)
			table.operate_effect_action(user_index, GameConstants.EFFECT_ACTION_CP, 1, new long[] { GameConstants.CP_WAIT_TOU }, 1,
				seat_index);
		
		
		
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index] = false;
		table.set_qi_player();
		return true;
	}
}
