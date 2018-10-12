package com.cai.game.phz.handler.yywhz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phz.handler.PHZHandlerChiPeng;
import com.cai.game.phz.handler.PHZHandlerWai;


public class WHZHandlerWai_YY extends PHZHandlerWai<YYWHZTable> {
	
	private GangCardResult m_gangCardResult;
	
	public WHZHandlerWai_YY(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(YYWHZTable table) {
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]-=2;
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yywhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);
		
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);
		
		table._mo_card_index[_seat_index][table._logic.switch_to_card_index(_card)]++;
		
		
		int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];
		for (int j = 0; j < GameConstants.MAX_HH_INDEX; j++) {
			cbCardIndexTemp[j] = table.GRR._cards_index[_seat_index][j];
		}
		if(cbCardIndexTemp[table._logic.switch_to_card_index(_card)] == 1){
			int cbRemoveCard[] = new int[] { _card, _card };
			table.cannot_outcard(_seat_index, 1, cbRemoveCard,_card, true);
		}
		
		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);
		
		
		
		GangCardResult gangCardResult = new GangCardResult();
		int cbActionMask=table.estimate_player_liu_nei_respond_yywhz(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],table._send_card_data,
				gangCardResult);
			
		if(cbActionMask!=GameConstants.WIK_NULL){//有溜
			for(int i= 0; i < gangCardResult.cbCardCount; i++){
				curPlayerStatus.add_liu(gangCardResult.cbCardData[i], _seat_index, gangCardResult.isPublic[i],cbActionMask);
				curPlayerStatus.add_action(cbActionMask);//溜牌
				curPlayerStatus.add_action(GameConstants.WIK_NULL);
				curPlayerStatus.add_pass(_card, _seat_index);
					
				curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
					
			}
		}
		//外溜
		gangCardResult.cbCardCount=0;
		cbActionMask =table.estimate_player_liu_wai_respond_yywhz(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],0,
				gangCardResult,false);

		if(cbActionMask!=GameConstants.WIK_NULL){//有溜
			for(int i= 0; i < gangCardResult.cbCardCount; i++){
				curPlayerStatus.add_liu(gangCardResult.cbCardData[i], _seat_index, gangCardResult.isPublic[i],cbActionMask);
				curPlayerStatus.add_action(cbActionMask);//溜牌
				curPlayerStatus.add_action(GameConstants.WIK_NULL);
				curPlayerStatus.add_pass(_card, _seat_index);
					
				curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
					
			}
		}
		//歪后判断胡牌
		int card_type = GameConstants.HU_CARD_TYPE_ZIMO;
		int hu_xi_chi[] = new int[1];
		ChiHuRight chr = new ChiHuRight(); 
		hu_xi_chi[0] = 0;
		chr = table.GRR._chi_hu_rights[_seat_index];
		chr.set_empty();
		int action_hu = table.analyse_chi_hu_card(table.GRR._cards_index[_seat_index], table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],_seat_index,_seat_index,_card, chr, card_type,hu_xi_chi,false);// 自摸
		if(table._is_xiang_gong[_seat_index] == true)
			action_hu = GameConstants.WIK_NULL;
		if (action_hu != GameConstants.WIK_NULL) {
			curPlayerStatus.add_action(GameConstants.WIK_ZI_MO);
			curPlayerStatus.add_zi_mo(_card, _seat_index);
			curPlayerStatus.add_action(GameConstants.WIK_NULL);
			curPlayerStatus.add_pass(_card, _seat_index);
		}
		else {
			chr.set_empty();
		}
		if(curPlayerStatus.has_action()){
			table.operate_player_action(_seat_index,false);
		}else if(table.is_can_out_card(_seat_index) ){
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
			//table.log_player_error(_seat_index, "吃或碰出牌状态");
		}else{
			table._is_xiang_gong[_seat_index] = true;	 	
			table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
		
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
		}
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
	public boolean handler_operate_card(YYWHZTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		if(seat_index!=_seat_index){
			table.log_error("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			int next_player = _seat_index; 
			table._current_player = next_player;
			table._last_player = next_player;
			
			PlayerStatus curPlayerStatus = table._playerStatus[next_player];
			curPlayerStatus.reset();
			if(table.is_can_out_card(_seat_index)){
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
				table.operate_player_action(_seat_index, false);
			}else{
				next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
				// 用户状态
				table._playerStatus[_seat_index].clean_action();
				table._playerStatus[_seat_index].clean_status();
				table._current_player = next_player;
				table._last_player = next_player;
				
				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			}
			
			return true;
		}
		
		// 是否已经响应
		if (playerStatus.is_respone()) {
			table.log_player_error(seat_index, "DispatchCard 出牌,玩家已操作");
			return true;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
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
					target_p = table.getTablePlayerNumber() - p;
					cbMaxActionRand = cbUserActionRank;
				} 
			}
		}
	
			
		// 优先级最高的人还没操作
		if (table._playerStatus[target_player].is_respone() == false)
		{
			table.log_error("优先级最高的人还没操作");
			return true;
		}
		// 执行动作
		switch (target_action) {
		case GameConstants.WIK_YYWHZ_LIU_WAI: // 溜牌
		{
			table.exe_liu(target_player, _seat_index, target_action, operate_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,0);
//			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(operate_card)]=0;
//			// 组合扑克
//			int wIndex = -1;
//			for(int i=0;i<table.GRR._weave_count[_seat_index];i++){
//				if(table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_YYWHZ_WAI
//				&& table.GRR._weave_items[_seat_index][i].center_card == operate_card){
//					wIndex=i;
//				}
//			}
//			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
//			table.GRR._weave_items[_seat_index][wIndex].center_card = operate_card;
//			table.GRR._weave_items[_seat_index][wIndex].weave_kind = target_action;
//
//			table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;
//			table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yywhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);
//
//			int _action = GameConstants.WIK_YYWHZ_LIU_NEI;
//			// 效果
//			table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
//					GameConstants.INVALID_SEAT);
//			
//			int cards[]= new int[GameConstants.MAX_YYWHZ_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
//					table.GRR._weave_items[_seat_index]);
//			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//			// 用户状态
//			table._playerStatus[_seat_index].clean_action();
//			table._playerStatus[_seat_index].clean_status();
//			table._current_player = next_player;
//			table._last_player = next_player;
//			
//			
//			//溜后听牌刷新
//			table._playerStatus[_seat_index]._hu_card_count = table.get_yywhz_ting_card(
//					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
//					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);
//
//			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
//			int ting_count = table._playerStatus[_seat_index]._hu_card_count;
//
//			if (ting_count > 0) {
//				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
//			} else {
//				ting_cards[0] = 0;
//				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
//			}
//				
//			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			
			return true;
		}
		case GameConstants.WIK_YYWHZ_LIU_NEI: // 溜牌
		{
			if(_card != operate_card){
				table._user_out_card_count[_seat_index]++;
			}
			table.exe_liu(target_player, _seat_index, target_action, operate_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,0);
//			if(table.GRR._cards_index[target_player][table._logic.switch_to_card_index(operate_card)] != 4){
//				// 用户状态
//				int next_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//				table._current_player = next_player;
//				table._last_player = next_player;
//				table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
//				int cards[]= new int[GameConstants.MAX_YYWHZ_COUNT];
//				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
//
//				table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
//						table.GRR._weave_items[target_player]);
//				return true;
//			}else{
//				table.GRR._cards_index[target_player][table._logic.switch_to_card_index(operate_card)]=0;
//				// 组合扑克
//				int wIndex = table.GRR._weave_count[target_player]++;
//				table.GRR._weave_items[target_player][wIndex].public_card = 1;
//				table.GRR._weave_items[target_player][wIndex].center_card = operate_card;
//				table.GRR._weave_items[target_player][wIndex].weave_kind = operate_code;
//
//				table.GRR._weave_items[target_player][wIndex].provide_player = target_player;
//				table.GRR._weave_items[target_player][wIndex].hu_xi = table._logic.get_weave_hu_xi_yywhz_yywzh(table.GRR._weave_items[target_player][wIndex]);
//			}
//			
//			table._user_out_card_count[_seat_index]++;
//			int _action = GameConstants.WIK_YYWHZ_LIU_NEI;
//			// 效果
//			table.operate_effect_action(target_player, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
//					GameConstants.INVALID_SEAT);
//			
//			int cards[]= new int[GameConstants.MAX_YYWHZ_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[target_player], cards);
//
//			table.operate_player_cards(target_player, hand_card_count, cards, table.GRR._weave_count[target_player],
//					table.GRR._weave_items[target_player]);
//			
//			//下一玩家 用户状态
//			int next_player = (target_player + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
//			// 用户状态
//			table._playerStatus[target_player].clean_action();
//			table._playerStatus[target_player].clean_status();
//			table._current_player = next_player;
//			table._last_player = next_player;
//				
//			//溜后听牌刷新
//			table._playerStatus[_seat_index]._hu_card_count = table.get_yywhz_ting_card(
//					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
//					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);
//
//			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
//			int ting_count = table._playerStatus[_seat_index]._hu_card_count;
//
//			if (ting_count > 0) {
//				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
//			} else {
//				ting_cards[0] = 0;
//				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
//			}
//			
//			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);

			return true;
		}
		case GameConstants.WIK_ZI_MO: // 自摸
		{
			table.GRR._chi_hu_rights[target_player].set_valid(true);

			table.GRR._chi_hu_card[target_player][0] = operate_card;

			table._cur_banker = target_player;

			table._shang_zhuang_player = target_player;
			
			table.process_chi_hu_player_operate(target_player, operate_card, true);
			table.process_chi_hu_player_score_yywhz(target_player, _seat_index, operate_card, true);
			table.countChiHuTimes(target_player,true);
			// 记录
			if (table.GRR._chi_hu_rights[target_player].da_hu_count > 0) {
				table._player_result.da_hu_zi_mo[target_player]++;
			} else {
				table._player_result.xiao_hu_zi_mo[target_player]++;
			}

			int delay = GameConstants.GAME_FINISH_DELAY_FLS;
			if (table.GRR._chi_hu_rights[target_player].type_count > 2) {
				delay += table.GRR._chi_hu_rights[target_player].type_count - 2;
			}
			
			if(!(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_WU_DUI_WHZ)).is_empty()
			|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_SHI_DUI_WHZ)).is_empty()
			|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_YI_DUI_WHZ)).is_empty()
			|| !(table.GRR._chi_hu_rights[target_player].opr_and(GameConstants.CHR_JIU_DUI_WHZ)).is_empty()){
				table._hu_weave_count[target_player]=0;
			}
			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}
		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(YYWHZTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.istrustee[seat_index]=false;
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
