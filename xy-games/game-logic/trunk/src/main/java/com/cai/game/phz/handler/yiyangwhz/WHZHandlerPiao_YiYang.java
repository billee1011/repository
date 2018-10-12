package com.cai.game.phz.handler.yiyangwhz;

import java.util.concurrent.TimeUnit;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.future.GameSchedule;
import com.cai.future.runnable.GameFinishRunnable;
import com.cai.future.runnable.OutCardRunnable;
import com.cai.game.phz.handler.PHZHandlerChiPeng;
import com.cai.game.phz.handler.PHZHandlerLiu;


public class WHZHandlerPiao_YiYang extends PHZHandlerLiu<YiYangWHZTable> {
	
	private GangCardResult m_gangCardResult;
	
	public WHZHandlerPiao_YiYang(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(YiYangWHZTable table) {
		
		table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]=0;
		// 组合扑克
		int wIndex = -1;
		for(int i=0;i<table.GRR._weave_count[_seat_index];i++){
			if(table.GRR._weave_items[_seat_index][i].weave_kind == GameConstants.WIK_PENG
			&& table.GRR._weave_items[_seat_index][i].center_card == _card){
				wIndex=i;
			}
		}
		if(_seat_index == _provider){
			table.operate_player_get_card(_provider, 0, null, GameConstants.INVALID_SEAT,false);
		}else{
			table.operate_player_get_card(_provider, 0, null, GameConstants.INVALID_SEAT,false);
			table.operate_out_card(_provider, 0, null, GameConstants.OUT_CARD_TYPE_MID,
					GameConstants.INVALID_SEAT);
		}
		
		

		
		if(wIndex == -1){
			table.GRR._cards_index[_seat_index][table._logic.switch_to_card_index(_card)]=0;
			// 组合扑克
			wIndex = table.GRR._weave_count[_seat_index]++;
			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
				
			table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

			table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;
			table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yiyangwhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);
		}else{
				
			table.GRR._weave_items[_seat_index][wIndex].public_card = 1;
			table.GRR._weave_items[_seat_index][wIndex].center_card = _card;
			table.GRR._weave_items[_seat_index][wIndex].weave_kind = _action;

			table.GRR._weave_items[_seat_index][wIndex].provide_player = _seat_index;
			table.GRR._weave_items[_seat_index][wIndex].hu_xi = table._logic.get_weave_hu_xi_yiyangwhz_yywzh(table.GRR._weave_items[_seat_index][wIndex]);	
		}

		table._chi_card_index[_seat_index][table._logic.switch_to_card_index(_card)]++;

		// 效果
		table.operate_effect_action(_seat_index, GameConstants.EFFECT_ACTION_TYPE_ACTION, 1, new long[] { _action }, 1,
				GameConstants.INVALID_SEAT);
		
		int cards[]= new int[GameConstants.MAX_YYWHZ_COUNT];
		int hand_card_count = table._logic.switch_to_cards_data(table.GRR._cards_index[_seat_index], cards);

		table.operate_player_cards(_seat_index, hand_card_count, cards, table.GRR._weave_count[_seat_index],
				table.GRR._weave_items[_seat_index]);
		//溜后听牌刷新
		table._playerStatus[_seat_index]._hu_card_count = table.get_yywhz_ting_card(
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
		
		//下一玩家 用户状态
		int next_player = (_seat_index + table.getTablePlayerNumber() + 1) % table.getTablePlayerNumber();
		// 用户状态
		table._playerStatus[_seat_index].clean_action();
		table._playerStatus[_seat_index].clean_status();
		table._current_player = next_player;
		table._last_player = next_player;
		table.exe_dispatch_card(next_player, GameConstants.WIK_NULL, 1000);

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
