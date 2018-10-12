package com.cai.game.schcp.handler.dazhui;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.DisplayCardRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.schcp.SCHCPTable;
import com.cai.game.schcp.handler.SCHCPHandlerChiPeng;


public class SCHCPHandlerZhao_DAZHUI extends SCHCPHandlerChiPeng<SCHCPTable> {
	
	
	public SCHCPHandlerZhao_DAZHUI(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(SCHCPTable table) {
		// 组合扑克
	
		// 设置用户
		table._current_player = _seat_index;
	

		
		table.operate_player_get_card(table._last_player, 1, new int[]{_card} , GameConstants.INVALID_SEAT,false);
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]++;
		table._zhao_guo_card[_seat_index][table._logic.switch_to_card_index(_card)]++;

	
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		int chi_peng_cards[] = new int[3];
		int chi_peng_count = table._logic.switch_to_value_to_card(table._logic.get_dot(_card), chi_peng_cards);
		for(int i = 0; i < chi_peng_count; i++){
			table._cannot_chi_index[_seat_index][table._logic.switch_to_card_index(chi_peng_cards[i])] = 0;
			table._cannot_peng_index[_seat_index][table._logic.switch_to_card_index(chi_peng_cards[i])] = 0;
		}
		table.cannot_outcard(_seat_index, _card,-1, true);
		int s_cards[] = new int[3];
		int count = table._logic.switch_to_value_to_card(7,s_cards);
		for(int i = 0; i<count ;i++)
		{
			if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(s_cards[i])] == 2)
			{
				table.cannot_outcard(_seat_index, s_cards[i],1, true);
			}
		}

		if(table._ti_mul_long[_seat_index]>0)
		{
			curPlayerStatus.add_action(GameConstants.CP_WIK_TOU);
			curPlayerStatus.add_tou(_card, GameConstants.CP_WIK_TOU, _seat_index);
		}
	
		table.operate_cannot_card(_seat_index, true);
		if (curPlayerStatus.has_action() ) {// 有动作
			if (table.isTrutess(_seat_index)) {
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _card),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index, false);
		} else {
			if (table.isTrutess(_seat_index)) {
				
				GameSchedule.put(new OutCardRunnable(table.getRoom_id(), _seat_index, _card),
						GameConstants.DELAY_AUTO_OUT_CARD_TRUTESS, TimeUnit.MILLISECONDS);
				return;
			}
			if(table.check_out_card(_seat_index)==false)
			{
				table.no_card_out_game_end(_seat_index, 0);
				return ;
			}
//			table.operate_player_get_card(table._last_player, 0, null , GameConstants.INVALID_SEAT,false,1);
//			// 刷新自己手牌
//			int cards[] = new int[GameConstants.MAX_CP_COUNT];
//			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//			table.must_out_card();
//			if(table.check_out_card(_seat_index)==false)
//			{
//				table.no_card_out_game_end(_seat_index, 0);
//				return  ;
//			}
//			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
//			table.operate_player_status();
			GameSchedule.put(new DisplayCardRunnable(table.getRoom_id(), _seat_index, _card,true),
					800, TimeUnit.MILLISECONDS);
			
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
	public boolean handler_operate_card(SCHCPTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
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
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			if(table.check_out_card(_seat_index)==false)
			{
				table.no_card_out_game_end(_seat_index, 0);
				return true ;
			}
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}
		else if(operate_code == GameConstants.CP_WIK_TOU){
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			table.exe_Dispatch_tou_card_data(seat_index,GameConstants.WIK_NULL,0);
			return true;
		}
	

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(SCHCPTable table,int seat_index) {
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
