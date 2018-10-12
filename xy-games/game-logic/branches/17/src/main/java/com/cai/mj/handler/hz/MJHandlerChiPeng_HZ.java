package com.cai.mj.handler.hz;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerChiPeng;

public class MJHandlerChiPeng_HZ extends MJHandlerChiPeng {
	private GangCardResult m_gangCardResult;
	
	public MJHandlerChiPeng_HZ(){
		m_gangCardResult = new GangCardResult();
	}
	
	@Override
	public void exe(MJTable table) {
		//回放
		PlayerStatus curPlayerStatus = table._playerStatus[_current_player];
		curPlayerStatus.reset();
		
		table._playerStatus[_current_player].chi_hu_round_valid();//可以胡了
		
		//至少要留抓鸟的牌
		int llcard = table.get_niao_card_num(true,0);
		
		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > llcard) {
			int cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_current_player],
					table.GRR._weave_items[_current_player], table.GRR._weave_count[_current_player], m_gangCardResult,false);
			
			if(cbActionMask!=0){
				curPlayerStatus.add_action(MJGameConstants.WIK_GANG);//转转就是杠
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					//加上刚
					curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _current_player, m_gangCardResult.isPublic[i]);
				}
			}
		}
		
		if (curPlayerStatus.has_action()) {
			curPlayerStatus.set_status(MJGameConstants.Player_Status_OPR_CARD);// 操作状态
			table.operate_player_action(_current_player,false);
			
		} else {
			curPlayerStatus.set_status(MJGameConstants.Player_Status_OUT_CARD);// 出牌状态
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
	public boolean handler_operate_card(MJTable table,int seat_index, int operate_code, int operate_card){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		
		// 效验操作 
		if((operate_code != MJGameConstants.WIK_NULL) &&(playerStatus.has_action_by_code(operate_code)==false)){
			table.log_error("没有这个操作");
			return false;
		}
		
		if(seat_index!=_current_player){
			table.log_error("不是当前玩家操作");
			return false;
		}
		
		// 放弃操作
		if (operate_code == MJGameConstants.WIK_NULL) {
			// 用户状态
			table._playerStatus[_current_player].clean_action();
			table._playerStatus[_current_player].clean_status();
			
			table._playerStatus[_current_player].set_status(MJGameConstants.Player_Status_OUT_CARD);
			table.operate_player_status();
			
			return true;
		}

		// 执行动作
		switch (operate_code) {
			case MJGameConstants.WIK_GANG: // 杠牌操作
			{
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					if(operate_card == m_gangCardResult.cbCardData[i]){
						//是否有抢杠胡
						table.exe_gang(_current_player, _current_player, operate_card, operate_code, m_gangCardResult.type[i], true,false);
						return true;
					}
				}
				
			}
			break;
		}

		return true;
	}
	
	@Override
	public boolean handler_player_be_in_room(MJTable table,int seat_index) {
		super.handler_player_be_in_room(table, seat_index);
		int ting_cards[] = table._playerStatus[seat_index]._hu_cards;
		int ting_count = table._playerStatus[seat_index]._hu_card_count;
		
		if(ting_count>0){
			table.operate_chi_hu_cards(seat_index, ting_count, ting_cards);
		}
		return true;
	}
}
