package com.cai.mj.handler.cs;

import com.cai.common.constant.MJGameConstants;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.PlayerStatus;
import com.cai.mj.MJTable;
import com.cai.mj.handler.MJHandlerChiPeng;

public class MJHandlerChiPeng_CS extends MJHandlerChiPeng {
	
	private GangCardResult m_gangCardResult;
	
	public MJHandlerChiPeng_CS(){
		m_gangCardResult = new GangCardResult();
	}
	
	
	@Override
	public void exe(MJTable table) {
		
		PlayerStatus curPlayerStatus = table._playerStatus[_current_player];
		curPlayerStatus.reset();
		
		table._playerStatus[_current_player].chi_hu_round_valid();//可以胡了
		
		m_gangCardResult.cbCardCount = 0;
		// 如果牌堆还有牌，判断能不能杠
		if (table.GRR._left_card_count > 1) {
			
			int cbActionMask=MJGameConstants.WIK_NULL;
			
			cbActionMask = table._logic.analyse_gang_card_all(table.GRR._cards_index[_current_player],
					table.GRR._weave_items[_current_player], table.GRR._weave_count[_current_player], m_gangCardResult,true);
			
			if(cbActionMask!=MJGameConstants.WIK_NULL){//有杠
				//添加动作 长沙麻将是补张
				curPlayerStatus.add_action(MJGameConstants.WIK_BU_ZHNAG);
				
				for(int i= 0; i < m_gangCardResult.cbCardCount; i++){
					//加上补张
					curPlayerStatus.add_bu_zhang(m_gangCardResult.cbCardData[i], _current_player, m_gangCardResult.isPublic[i]);
					
					if(table.GRR._left_card_count > 2){
						
						//把可以杠的这张牌去掉。看是不是听牌
						int bu_index = table._logic.switch_to_card_index(m_gangCardResult.cbCardData[i]);
						int save_count = table.GRR._cards_index[_current_player][bu_index];
						table.GRR._cards_index[_current_player][bu_index]=0;
						
						boolean is_ting = table.is_cs_ting_card(table.GRR._cards_index[_current_player],
								table.GRR._weave_items[_current_player], table.GRR._weave_count[_current_player]);
						
						table.GRR._cards_index[_current_player][bu_index] = save_count;
						
						if(is_ting == true){
							//加上杠
							curPlayerStatus.add_action(MJGameConstants.WIK_GANG);//听牌的时候可以杠
							curPlayerStatus.add_gang(m_gangCardResult.cbCardData[i], _current_player, m_gangCardResult.isPublic[i]);
						}
					}
					
					
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
			case MJGameConstants.WIK_BU_ZHNAG: // 杠牌操作
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
}
