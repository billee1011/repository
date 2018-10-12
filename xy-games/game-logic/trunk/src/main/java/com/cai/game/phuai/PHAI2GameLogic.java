/**
 * 
 */
package com.cai.game.phuai;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;



//手牌权值结构  
class HandCardValue{  
  int SumValue;        //手牌总价值  
  int NeedRound;       // 需要打几手牌  
};  

//牌型组合数据结构  
class CardGroupData{  
	//枚举类型  
	int cgType=GameConstants.DDZ_CT_ERROR;  
	//该牌的价值  
	int  nValue=0;  
	//含牌的个数  
	int  nCount=0;  
	//牌中决定大小的牌值，用于对比  
	int nMaxCard=0;    
}; 

public class PHAI2GameLogic {
	
	public PHAIGameLogic _logic = null;
	public PHAI2GameLogic() {
		_logic = new PHAIGameLogic();
	}
	

	public void AI_Out_Card(PHAITable table,int seat_index){
		
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return ;
		int card = get_card(table,seat_index);
		table.handler_player_out_card(seat_index, card);
	}
	public int get_card(PHAITable table,int seat_index) {
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 1)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		return 1;
	}
	public void AI_Operate_Card(PHAITable table,int seat_index){
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OPR_CARD)
			return ;
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = GameConstants.WIK_ZI_MO;
		int operate_card = playerStatus.get_operate_card();
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return ;
		}
		operate_code = GameConstants.WIK_CHI_HU;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
		operate_code = GameConstants.WIK_PENG;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
		operate_code = GameConstants.WIK_NULL;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
	}
	public void AI_WARNING(PHAITable table,int seat_index){
		if (table._warning[seat_index] != 1)
			return ;
		table.handler_ask_player(seat_index, true);
	}

	//拆牌
	
	//机器人出牌算法

}
