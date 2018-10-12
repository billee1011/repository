package com.cai.game.phu.handler.phuyx;

import java.util.Arrays;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.game.phu.PHTable;
import com.cai.game.phu.handler.PHHandlerChiPeng;

public class PHHandlerChiPeng_YX extends PHHandlerChiPeng<PHTable> {
	
	private GangCardResult m_gangCardResult;
	
	public PHHandlerChiPeng_YX(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(PHTable table) {
		// 组合扑克
		boolean one_other = true;
		int wIndex = table.GRR._weave_count[_seat_index]++;
		table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
		table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
		table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;
		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			if(_action == GameConstants.WIK_PENG)
			{
				table.GRR._weave_items[_seat_index][wIndex].weave_kind = GameConstants.WIK_PENG_OUT;
				table.exe_add_discard( _provider,  1, new int[]{_card },true,1);
				one_other = false;
			}
		}
		
		
		table.GRR._weave_items[_seat_index][wIndex].provide_player = _provider;
		table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi(table.GRR._weave_items[_seat_index][wIndex]);
		// 设置用户
		table._current_player = _seat_index;
		int peng_sao_count[] = new int[1];
		int temp_score = table._logic.get_weave_hu_fen(table.GRR._weave_items[_seat_index], table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index][wIndex].weave_kind,peng_sao_count);
		
  		int score[] = new int[table.getTablePlayerNumber()];
        Arrays.fill(score, 0);
        table._logic.calculate_game_mid_score(_seat_index,_provider,table.getTablePlayerNumber(),one_other ,score,temp_score);
        table._logic.calculate_game_weave_score(table._game_mid_score,table._game_weave_score,score,table.getTablePlayerNumber(),false,table.get_match_times());

		if(table._game_weave_score[_seat_index] != 0)
		 {
			 table.operate_game_mid_score(true);
		 }
        //效果
		int eat_type = GameConstants.WIK_LEFT | GameConstants.WIK_CENTER | GameConstants.WIK_RIGHT
				| GameConstants.WIK_DDX | GameConstants.WIK_XXD | GameConstants.WIK_EQS|GameConstants.WIK_YWS;
		if(_action == GameConstants.WIK_PENG||_action == GameConstants.WIK_PENG_OUT){
			if(peng_sao_count[0] == 3)
				table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_PENG_SAN_DA}, 1,GameConstants.INVALID_SEAT);
			else if(peng_sao_count[0] == 4)
				table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_PENG_SI_QING}, 1,GameConstants.INVALID_SEAT);
			else 
				table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
		}
		else if(_lou_card == -1 || (eat_type & _action )==0)
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{_action}, 1,GameConstants.INVALID_SEAT);
		else
			table.operate_effect_action(_seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{GameConstants.WIK_LOU}, 1,GameConstants.INVALID_SEAT);

		if(_type == GameConstants.CHI_PENG_TYPE_OUT_CARD){
			//删掉出来的那张牌
			//table.operate_out_card(this._provider, 0, null,MJGameConstants.OUT_CARD_TYPE_MID,MJGameConstants.INVALID_SEAT);
			//table.operate_remove_discard(this._provider, table.GRR._discard_count[_provider]);
//			table.operate_out_card(this._provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
//					GameConstants.INVALID_SEAT);
		}
		if(_type == GameConstants.CHI_PENG_TYPE_DISPATCH){
//			table.operate_player_get_card(table._last_player, 0, null, GameConstants.INVALID_SEAT,false);
		}
		
		
		//刷新手牌包括组合
		int cards[]= new int[GameConstants.MAX_HH_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);
		
		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index], table.GRR._weave_items[_seat_index]);
		
	
						
		
		PlayerStatus curPlayerStatus = table._playerStatus[_seat_index];
		curPlayerStatus.reset();
	
		if(table._ti_two_long[_seat_index] == false){
			curPlayerStatus.set_status(GameConstants.Player_Status_OUT_CARD);// 出牌状态
			table._playerStatus[_seat_index].set_handler_status(GameConstants.PH_STATUS_CHI_PENG);
			table.operate_player_status();
			table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
			table.operate_player_timer(true);
			//table.log_player_error(_seat_index, "吃或碰出牌状态");
		
			
		}
		else {
			if(table._ti_two_long[_seat_index] == true)
				table._ti_two_long[_seat_index] = false;
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
			table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);
			//table.log_player_error(_seat_index, "吃或碰 下家发牌");
			
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
	public boolean handler_status_operate_card(PHTable table,int seat_index, int operate_code, int operate_card,int lou_pai,int handler_status){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != GameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
//			table.log_error("HHHandlerChiPeng_YX 没有这个操作:"+operate_code);
			return false;
		}
		
		if(seat_index!=_seat_index){
			table.log_error("PHHandlerChiPeng_YX 不是当前玩家操作");
			return false;
		}
//		assert handler_status == GameConstants.PH_STATUS_CHI_PENG;
		if(handler_status != GameConstants.PH_STATUS_CHI_PENG){
			table.log_error("PHHandlerChiPeng_YX"+seat_index+" handler_status = "+ handler_status+"!= "+GameConstants.PH_STATUS_CHI_PENG+"seat_index = " + seat_index);
//			return false;
		}
		table.record_effect_action(seat_index,GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[]{operate_code}, 1);
		
		// 放弃操作
		if (operate_code == GameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_seat_index].clean_action();
			table._playerStatus[_seat_index].clean_status();
			
			table._playerStatus[_seat_index].set_status(GameConstants.Player_Status_OUT_CARD);
			table._playerStatus[_seat_index].set_handler_status(GameConstants.PH_STATUS_CHI_PENG);
			table.operate_player_status();
			table.set_timer(GameConstants.PH_OUT_CARD , 5, true);
			table.operate_player_timer(true);
			return true;
		}

	

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(PHTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		if(table.istrustee[seat_index] == true)
			table.handler_request_trustee(seat_index,false,0);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
