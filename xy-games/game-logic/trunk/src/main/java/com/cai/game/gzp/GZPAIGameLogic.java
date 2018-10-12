/**
 * 
 */
package com.cai.game.gzp;

import java.util.Arrays;

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

public class GZPAIGameLogic {
	

	
	public static void Set_Pao(GZPTable table, int seat_index){
		table._handler_piao.handler_pao_qiang(table, seat_index, 0, 0);
	}
	public static void AI_Out_Card(GZPTable table,int seat_index){
		
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return ;
		int card = get_card(table,seat_index);
		table.handler_player_out_card(seat_index, card);
	}
	public static int get_card(GZPTable table,int seat_index) {
		
		for(int i = 0; i < GameConstants.GZP_MAX_INDEX;i++)
		{
			if(table._pick_up_card != 0 && table._logic.switch_to_card_index(table._pick_up_card) == i)
				continue;
			if (table.GRR._cards_index[seat_index][i] == 1)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.GZP_MAX_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		return 1;
	}
	public static void AI_Operate_Card(GZPTable table,int seat_index){
			
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = GameConstants.GZP_WIK_ZI_MO;
		int operate_card =  playerStatus.get_weave_card( operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return ;
		}
		operate_code = GameConstants.GZP_WIK_CHI_HU;
		operate_card =  playerStatus.get_weave_card( operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
		operate_code = GameConstants.GZP_WIK_PENG;
		operate_card =  playerStatus.get_weave_card( operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
//		operate_code = GameConstants.GZP_WIK_GUAN;
		
		operate_code = GameConstants.WIK_NULL;
		operate_card =  playerStatus.get_weave_card( operate_code);
		if (playerStatus.has_action() != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
	}

	//拆牌
	
	//机器人出牌算法

}
