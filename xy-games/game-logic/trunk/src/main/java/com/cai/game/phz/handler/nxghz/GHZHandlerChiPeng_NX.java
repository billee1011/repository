package com.cai.game.phz.handler.nxghz;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.phz.handler.PHZHandlerChiPeng;


public class GHZHandlerChiPeng_NX extends PHZHandlerChiPeng<NXGHZTable> {
	
	private GangCardResult m_gangCardResult;
	
	public GHZHandlerChiPeng_NX(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(NXGHZTable table) {
		// 组合扑克
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yywhz_nxghz(table.GRR._weave_items[_seat_index][wIndex]);
		int cbMingIndexTemp[] = new int [GameConstants.MAX_HH_INDEX];
		table._logic.ming_index_temp(cbMingIndexTemp,table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],false,0);
		table._hu_xi[_seat_index] = table._logic.get_all_hu_xi_weave(table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index], cbMingIndexTemp);
		// 设置用户
		table._current_player = _seat_index;
		table.cannot_outcard(_seat_index, 1, _card, true);
		
		
		//效果
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS|GameConstants.WIK_YWS;
	
		if(_lou_card == -1 || (eat_type & _action )==0)
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_LOU}, 1,GameConstants.INVALID_SEAT);
		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			//table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);
			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}
		if(_type == GameConstants.CHI_PENG_TYPE_DISPATCH){
			table.log_error(table._last_player+"CHI_PENG_TYPE_DISPATCH");
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT,false);
		}
		
		
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
	
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
		int cbActionMask=table.estimate_player_liu_respond_yywhz(table.GRR._cards_index[_seat_index],table.GRR._weave_items[_seat_index],
				table.GRR._weave_count[_seat_index],table._send_card_data,
				m_gangCardResult);
		if(cbActionMask!=GameConstants.WIK_NULL){//有溜
			for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
				curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _seat_index, m_gangCardResult.isPublic[i]);
				curPlayerStatus.add_action(GameConstants.WIK_YYWHZ_LIU_NEI);//转转就是杠
				
				table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OPR_CARD);// 操作状态
				table.operate_player_action(_seat_index,false);
			}
		}
		
	if(table.is_can_out_card(_seat_index)){
		curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		table.operate_player_status();
		//table.log_player_error(_seat_index, "吃或碰出牌状态");
	}
	else
	{
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
	public boolean handler_operate_card(NXGHZTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
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
	public boolean handler_player_be_in_room(NXGHZTable table,int seat_index) {
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
