package com.cai.game.gzp.handler.gzpddwf;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.gzp.GZPTable;
import com.cai.game.gzp.handler.GZPHandlerChiPeng;

public class GZPHandlerChiPeng_DDWF extends GZPHandlerChiPeng {
	
	private GangCardResult m_gangCardResult;
	
	public GZPHandlerChiPeng_DDWF(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(GZPTable table) {
		for (int i = 0; i < table.getTablePlayerNumber(); i++) {
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
		table.GRR._weave_items[_seat_index][wIndex].weave_card=_copy;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi =  table._logic.get_weave_items_gzp(table.GRR._weave_items[_seat_index],table.GRR._weave_count[_seat_index]);
		// 设置用户
		table._current_player = _seat_index;

		
		//效果
		table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
		
		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider],_seat_index);
		}
		
		
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.GZP_MAX_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
		
	
		
		int left = table.getTablePlayerNumber() == GameConstants.GAME_PLAYER ? 4 : 3;
		

		//curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
		table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
		table.operate_player_status();
		
	
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
	public boolean handler_operate_card(GZPTable table,int seat_index, int operate_code, int operate_card){
		
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("GZPHandlerChiPeng_TC 没有这个操作:"+operate_code);
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("GZPHandlerChiPeng_TC 不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			
			
			//add by tan 通知客户端 落牌
			table.operate_remove_discard(GameConstants.INVALID_SEAT, GameConstants.INVALID_CARD);
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			//table._playerStatus[_seat_index].clean_status();
			table.change_player_status(_seat_index,GameConstants.INVALID_VALUE);
			table.change_player_status(_seat_index, GameConstants.Player_Status_OUT_CARD);
			//table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		return true;
	}
	/***
	 * //用户出牌--吃碰之后的出牌
	 */
	@Override
	public boolean handler_player_out_card(GZPTable table, int seat_index, int card) {
		// 错误断言
		card = table.get_real_card(card);
		boolean is_out = false;
		if((card&0x100)>>8 == 1)
		{
			card&=0xFF;
			is_out = true;
		}
		if(is_out == true && table._pick_up_index[seat_index][table._logic.switch_to_card_index(card)] == 0)
		{
			table.log_error("出捡牌,牌型出错");
			return false;
		}
		if (table._logic.is_valid_card(card) == false) {
			table.log_error("出牌,牌型出错");
			return false;
		}
		// 错误断言
		if (table._logic.is_valid_card(card) == false) {
			
			//刷新手牌包括组合
			table.log_error("出牌,牌型出错");
			return false;
		}

		// 效验参数
		if (seat_index != _seat_index) {
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_error("出牌,没到出牌");
			return false;
		}
		if(table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("状态不对不能出牌");
			return false;
		}
		if(is_out == false &&  table.GRR._cannot_out_index[seat_index][table._logic.switch_to_card_logic_index(card)]>0)
		{
			
			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
	
			table.log_info("当前牌不能出");
			return false;
		}
		// if (card == MJGameConstants.ZZ_MAGIC_CARD &&
		// table.is_mj_type(MJGameConstants.GAME_TYPE_HZ)) {
		// table.send_sys_response_to_player(seat_index, "癞子牌不能出癞子");
		// table.log_error("癞子牌不能出癞子");
		// return false;
		// }

		// 删除扑克
		// 删除扑克
		int flower_index = table._logic.switch_to_card_flower_index(card);
		int common_index =  table._logic.switch_to_card_common_index(card);
		int card_index = table._logic.switch_to_card_index(card);
		if(flower_index != -1)
		{
			if(table._pick_up_index[_seat_index][flower_index]>0)
			{
				table._pick_up_index[_seat_index][flower_index]--;
				table.operate_pick_up_card(_seat_index);
			}
			else if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		else if(common_index != -1)
		{
			if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);
			}
			else if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		else {
			if(table._pick_up_index[_seat_index][card_index]>0)
			{
				table._pick_up_index[_seat_index][card_index]--;
				table.operate_pick_up_card(_seat_index);	
			}
		}
		if (table._logic.remove_card_by_index(table.GRR._cards_index[_seat_index],card) == false) {

			//刷新手牌包括组合
			int cards[]= new int[GameConstants.GZP_MAX_COUNT];
			int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[seat_index], cards);
			table.operate_player_cards(seat_index, hand_card_count, cards, table.GRR._weave_count[seat_index], table.GRR._weave_items[seat_index],false);
			
			table.log_error("出牌删除出错");
			return false;
		}

		// 出牌--切换到出牌handler
		table.exe_out_card(_seat_index, card, _action);

		return true;
	}

	@Override
	public boolean handler_player_be_in_room(GZPTable table,int seat_index) {
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
