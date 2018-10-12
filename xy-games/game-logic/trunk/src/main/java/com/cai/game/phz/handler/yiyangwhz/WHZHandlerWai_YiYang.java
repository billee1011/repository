package com.cai.game.phz.handler.yiyangwhz;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.util.PerformanceTimer;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phz.handler.PHZHandlerChiPeng;
import com.cai.game.phz.handler.PHZHandlerWai;


public class WHZHandlerWai_YiYang extends PHZHandlerWai<YiYangWHZTable> {
	
	private GangCardResult m_gangCardResult;
	
	public WHZHandlerWai_YiYang(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(YiYangWHZTable table) {
		table.operate_player_get_card(_seat_index, 0, null, GameConstants.INVALID_SEAT,false);
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]-=2;
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yiyangwhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);
		
		int cards[] = new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);
		
		
		
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
		
		table._mo_card_index[_seat_index][table._logic.switch_to_card_index(_card)]++;
		
		
		table.estimate_player_chipeng_qing_piao_respond_yywhz(_seat_index,_seat_index,_card);
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
			
			for (int j = 0; j < 7; j++) {
				
				if(table._hu_weave_items[_seat_index][j].center_card > GameConstants.CARD_ESPECIAL_TYPE_NIAO){
					table._hu_weave_items[_seat_index][j].center_card-=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
					if(GameConstants.WIK_LEFT == table._hu_weave_items[_seat_index][j].weave_kind){
						table._hu_weave_items[_seat_index][j].weave_kind=GameConstants.WIK_YYWHZ_SHUNZI;
					}
					if(GameConstants.WIK_EQS == table._hu_weave_items[_seat_index][j].weave_kind){
						table._hu_weave_items[_seat_index][j].weave_kind=GameConstants.WIK_YYWHZ_SHUNZI_EQS;
					}
				}
				if(table._hu_weave_items[_seat_index][j].weave_kind == GameConstants.WIK_YYWHZ_MENZI_GUANG){
					table._hu_weave_items[_seat_index][j].weave_kind=GameConstants.WIK_YYWHZ_MENZI;
				}
				if (table._hu_weave_items[_seat_index][j].weave_kind == GameConstants.WIK_YYWHZ_WAI
				&& table._hu_weave_items[_seat_index][j].center_card == _card){
					table._hu_weave_items[_seat_index][j].center_card+=GameConstants.CARD_ESPECIAL_TYPE_NIAO;
				}

			}
		}
		else {
			chr.set_empty();
		}
		if(curPlayerStatus.has_action()){
			table.operate_player_action(_seat_index,false);
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		}else{
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_action(_seat_index, false);
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
	public boolean handler_operate_card(YiYangWHZTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		if(seat_index!=_seat_index){
			table.log_error("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
		table.record_effect_action(seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { operate_code }, 1);
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
 					cbUserActionRank = table._logic.get_action_rank_yiyang(table._playerStatus[i].get_perform(),target_player,_seat_index)
							+ table.getTablePlayerNumber() - p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbUserActionRank = table._logic.get_action_list_rank_yiyang(table._playerStatus[i]._action_count,
							table._playerStatus[i]._action,i,_seat_index) + table.getTablePlayerNumber() - p;
				}

				if (table._playerStatus[target_player].is_respone()) {
					// 获取已经执行的动作的优先级
					cbTargetActionRank = table._logic.get_action_rank_yiyang(table._playerStatus[target_player].get_perform(),target_player,_seat_index)
							+ target_p;
					cbActionRank[i] = cbUserActionRank;
				} else {
					// 获取最大的动作的优先级
					cbTargetActionRank = table._logic.get_action_list_rank_yiyang(
							table._playerStatus[target_player]._action_count,
							table._playerStatus[target_player]._action,target_player,_seat_index) + target_p;
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
		case GameConstants.WIK_YIYANGWHZ_QING_NEI: // 清牌
		case GameConstants.WIK_YIYANGWHZ_QING_WAI: // 清牌
		{
			table.exe_liu(target_player, _seat_index, target_action, operate_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,0);	
			return true;
		}
		case GameConstants.WIK_YIYANGWHZ_PIAO:{
			table.exe_piao(target_player, _seat_index, target_action, operate_card,
					GameConstants.CHI_PENG_TYPE_DISPATCH,0);	
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

			
			GameSchedule.put(new GameFinishRunnable(table.getRoom_id(), target_player, GameConstants.Game_End_NORMAL),
					delay, TimeUnit.SECONDS);

			return true;
		}
		}
		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(YiYangWHZTable table,int seat_index) {
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
