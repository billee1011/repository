package com.cai.game.xpbh.handler.bh;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.xpbh.XPBHTable;
import com.cai.game.xpbh.handler.BHHandlerChiPeng;

public class BHHandlerChiPeng_XP extends BHHandlerChiPeng<XPBHTable> {
	
	private GangCardResult m_gangCardResult;
	
	public BHHandlerChiPeng_XP(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(XPBHTable table) {
//		// 组合扑克
//		int wIndex = table.GRR._weave_count[_seat_index]++;
//		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
//		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
//		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
//
//		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
//		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);
//		// 设置用户
		table._current_player = _seat_index;

	
		if(_lou_card == -1 )
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_BH_CHI_LUO}, 1,GameConstants.INVALID_SEAT);
		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			//table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}
		if(_type == GameConstants.CHI_PENG_TYPE_DISPATCH){
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT,false);
		}
		
		table.can_out_xian_ming_zhao_card(_seat_index);
		
		
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.XPBH_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
	
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		int pai_count =0;
		for(int i = 0; i<GameConstants.XPBH_MAX_INDEX ;i++) {
			if(table._logic.yao_card(table._logic.switch_to_card_data(i)) == true)
				continue;
	       	if(table.GRR._cards_index[_seat_index][i]<3&&table.GRR._cannot_out_index[_seat_index][i] == 0)
	       		pai_count += table.GRR._cards_index[_seat_index][i];
		 }
		if(pai_count == 0)
		{
			for(int i = 0; i<GameConstants.XPBH_MAX_INDEX;i++)
			{
				if(table.GRR._cannot_out_index[_seat_index][i] != 0){
					pai_count += table.GRR._cards_index[_seat_index][i];
					table.GRR._can_ting_out_index[_seat_index][i] = 1;
				}
			}
		}
		if(pai_count != 0){
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();
		
			
		}
		else {
			table._is_xiang_gong[_seat_index] = true;	 	
//        	table.operate_player_xiang_gong_flag(_seat_index,table._is_xiang_gong[_seat_index]);
//			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
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
			int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
			table._current_player = next_player;
			table._last_player = next_player;
			table._last_card = 0;
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 500);
			
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
	public boolean handler_operate_card(XPBHTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_info("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		if(operate_code == GameConstants.WIK_NULL){
			table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_NULL}, 1);
		}
		if(seat_index!=_seat_index){
			table.log_info("HHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(XPBHTable table,int seat_index) {
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
