package com.cai.game.fls.handler.twentyfls;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.fls.FLSTable;
import com.cai.game.fls.handler.FLSHandlerChiPeng;

public class FLSHandlerChiPeng_LX_Twenty extends FLSHandlerChiPeng {
	
	private GangCardResult m_gangCardResult;
	
	public FLSHandlerChiPeng_LX_Twenty(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(FLSTable table) {
		for (int i = 0; i < GameConstants.GAME_PLAYER; i++) {
			table._playerStatus[i].clean_action();
			//table._playerStatus[i].clean_status();
			table.change_player_status(i,GameConstants.INVALID_VALUE);
			table.operate_player_action(i, true);
		}

		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		
		// 设置用户
		table._current_player = _seat_index;

		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT,_card,true);
		
		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);
		}
		
		
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.MAX_FLS_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		table._playerStatus[_seat_index].chi_hu_round_valid();//可以胡了
		
		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > 0) {
			
			int cbActionMask=GameConstants.WIK_NULL;
			
			cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], m_gangCardResult,false);
			
			if(cbActionMask!=GameConstants.WIK_NULL){//有杠
				//添加动作 长沙麻将是补张
//				if(!curPlayerStatus.lock_huan_zhang()){
//					curPlayerStatus.add_action(GameConstants.WIK_ZHAO);
//				}
				
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
//					//加上补张
//					if(!curPlayerStatus.lock_huan_zhang()){
//						curPlayerStatus.add_zhao(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
//					}
					//加上杠
					curPlayerStatus.add_action(GameConstants.WIK_ZHAO);//听牌的时候可以杠
					curPlayerStatus.add_zhao(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
					
					
				}
			}
	
		}
		
		if (curPlayerStatus.has_action()) {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OPR_CARD);
			//curPlayerStatus.set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_seat_index,false);
			
		} else {
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
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
	public boolean handler_operate_card(FLSTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("FLSHandlerChiPeng_LX 没有这个操作:"+operate_code);
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("FLSHandlerChiPeng_LX 不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table.change_player_status(_seat_index,GameConstants.INVALID_VALUE);
			//table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			//table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		// 执行动作
		switch (operate_code) {
			case GameConstants.WIK_GANG: // 杠牌操作
			case GameConstants.WIK_ZHAO: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_seat_index, _seat_index, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
			}
			break;
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(FLSTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		table.be_in_room_trustee(seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
