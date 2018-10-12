package com.cai.game.hh.handler.xtphz;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.hh.HHTable;
import com.cai.game.hh.handler.HHHandlerChiPeng;

public class PHZHandlerChiPeng_XT extends HHHandlerChiPeng<HHTable> {
	
	private GangCardResult m_gangCardResult;
	
	public PHZHandlerChiPeng_XT(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(HHTable table) {
		// 组合扑克
		if(table._guo_hu_xt[_seat_index]!= -1)
		{
			table._guo_hu_xt[_seat_index] = -1;
		}
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);
		// 设置用户
		table._current_player = _seat_index;

		
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
			table.log_info(table._last_player+"CHI_PENG_TYPE_DISPATCH");
			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT,false);
		}
		
		if( table._xt_display_an_long[_seat_index] == true){
			
			table._xt_display_an_long[_seat_index]  = false;
			for(int i = 0; i< table.GRR._weave_count[_seat_index]; i++)
			{
				if(table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_AN_LONG_LIANG)
					table.GRR._weave_items[_seat_index][i].weave_kind = GameConstants.WIK_AN_LONG;
			}
		
			
		}
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
	
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();

		Arrays.fill(table.GRR._can_ting_out_index[_seat_index],0);
		if(table.has_rule(GameConstants.GAME_RULE_DI_MING_WEI))
		{
			for(int i = 0; i<GameConstants.MAX_HH_INDEX;i++)
			{
				if(table.GRR._cannot_out_index[_seat_index][i] >= 1&&table.GRR._cards_index[_seat_index][i]>0)
				{
					int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
					Arrays.fill(cards_index,0);
					for(int j = 0; j< GameConstants.MAX_HH_INDEX;j++)
					{
						if( j == i && table.GRR._cards_index[_seat_index][j]>0)
						{
							cards_index[j] = table.GRR._cards_index[_seat_index][j];
							cards_index[j] --;
						}
						else{
							cards_index[j] = table.GRR._cards_index[_seat_index][j];
						}
					}
					table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
							table._playerStatus[_seat_index]._hu_cards, cards_index ,
							table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

					int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
					int ting_count = table._playerStatus[_seat_index]._hu_card_count;

					if (ting_count > 0) {
						table.GRR._can_ting_out_index[_seat_index][i] = 1;
						
					} 
				}
			}
			table.cannot_outcard(_seat_index, 0, 0, true);
		}
			
		if(table._ti_mul_long[_seat_index] == 0){
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table.operate_player_status();

		
			
		}
		
		else {
			if(table._ti_mul_long[_seat_index] >0)
				table._ti_mul_long[_seat_index] --;
			table._playerStatus[_seat_index]._hu_card_count = table.get_hh_ting_card_twenty(
					table._playerStatus[_seat_index]._hu_cards, table.GRR._cards_index[_seat_index],
					table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index],_seat_index,_seat_index);

			int ting_cards[] = table._playerStatus[_seat_index]._hu_cards;
			int ting_count = table._playerStatus[_seat_index]._hu_card_count;

			if (ting_count > 0) {
				table.operate_chi_hu_cards(_seat_index, ting_count, ting_cards);
			} else {
				ting_cards[0] = 0;
				table.operate_chi_hu_cards(_seat_index, 1, ting_cards);
			}
		
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
	public boolean handler_operate_card(HHTable table,int seat_index, int operate_code, int operate_card,int lou_pai){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_info("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		
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
	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(HHTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);

		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			table.log_info("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			table.log_info("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[_seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(table.has_rule(GameConstants.GAME_RULE_DI_MING_WEI)&&table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_index(card)] >= 1 )
		{
//			if(table.GRR._can_ting_out_index[seat_index][table._logic.switch_to_card_index(card)] == 0)
//			{
//				// 刷新手牌
//				int cards[] = new int[GameConstants.MAX_HH_COUNT];
//				// 显示出牌
//				table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_MID,
//						GameConstants.INVALID_SEAT);
//				// 刷新自己手牌
//				int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
//				table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);
//
//				table.log_info(_seat_index+"出牌出错 HHHandlerDispatchCard "+_seat_index);
//				return true;
//			}
//			else
//			{
				if(table.has_rule(GameConstants.GAME_RULE_DI_MING_WEI))
					table._xian_ming_zhao_not[seat_index] = true;
//			}
		}
		if(table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(card)] >= 3)
		{
			// 刷新手牌
			int cards[] = new int[GameConstants.MAX_HH_COUNT];
			// 显示出牌
			table.operate_out_card(_seat_index, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
			// 刷新自己手牌
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
			table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],table.GRR._weave_items[_seat_index]);

			table.log_info(_seat_index+"出牌出错 HHHandlerDispatchCard "+_seat_index);
			return true;
		}
		// 删除扑克
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index], card) == false) {
			table.log_info("出牌删除出错");
			return false;
		}

		// 出牌--切换到出牌handler
		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(HHTable table,int seat_index) {
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
