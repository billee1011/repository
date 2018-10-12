/**
 * 
 */
package com.cai.game.phu;

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

public class PHAIGameLogic {
	

	

	public static void AI_Out_Card(PHTable table,int seat_index){
		
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return ;
		int card = get_card(table,seat_index);
		table.handler_player_out_card(seat_index, card);
	}
	public static int get_card(PHTable table,int seat_index) {
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			
			if (table.GRR._cards_index[seat_index][i] == 1)
			{
				if(i == 1)
				{
					if(table.GRR._cards_index[seat_index][6] == 1)
						continue;
					if(table.GRR._cards_index[seat_index][9] == 1)
						continue;
				}
				if(i == 6)
				{
					if(table.GRR._cards_index[seat_index][1] == 1)
						continue;
					if(table.GRR._cards_index[seat_index][9] == 1)
						continue;
				}
				if(i == 9)
				{
					if(table.GRR._cards_index[seat_index][6] == 1)
						continue;
					if(table.GRR._cards_index[seat_index][1] == 1)
						continue;
				}
				if(i-1>=0&&table.GRR._cards_index[seat_index][i-1]==1)
					continue;
				if(i-2>=0&&table.GRR._cards_index[seat_index][i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX/2&&table.GRR._cards_index[seat_index][i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX/2&&table.GRR._cards_index[seat_index][i+2]==1)
					continue;
				if(table.GRR._cards_index[seat_index][i]==1&&table.GRR._cards_index[seat_index][i+10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if(i == 11)
			{
				if(table.GRR._cards_index[seat_index][16] == 1)
					continue;
				if(table.GRR._cards_index[seat_index][19] == 1)
					continue;
			}
			if(i == 16)
			{
				if(table.GRR._cards_index[seat_index][11] == 1)
					continue;
				if(table.GRR._cards_index[seat_index][19] == 1)
					continue;
			}
			if(i == 19)
			{
				if(table.GRR._cards_index[seat_index][16] == 1)
					continue;
				if(table.GRR._cards_index[seat_index][11] == 1)
					continue;
			}
			if (table.GRR._cards_index[seat_index][i] == 1)
			{
				if(i-1>=10&&table.GRR._cards_index[seat_index][i-1]==1)
					continue;
				if(i-2>=10&&table.GRR._cards_index[seat_index][i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX&&table.GRR._cards_index[seat_index][i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX&&table.GRR._cards_index[seat_index][i+2]==1)
					continue;
				if(table.GRR._cards_index[seat_index][i]==1&&table.GRR._cards_index[seat_index][i-10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if (table.GRR._cards_index[seat_index][i] == 1)
			{
				if((i+1<GameConstants.MAX_HH_INDEX&&table.GRR._cards_index[seat_index][i+1]==1)&&i+2<GameConstants.MAX_HH_INDEX&&table.GRR._cards_index[seat_index][i+2]==1)
				{
					i += 1;
					continue;
				}
				if(table.GRR._cards_index[seat_index][i] + table.GRR._cards_index[seat_index][i-10] == 3)
				{
					continue;
				}
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX/2;i++){
			if (table.GRR._cards_index[seat_index][i] == 1)
			{
				
				if((i+1<GameConstants.MAX_HH_INDEX/2&&table.GRR._cards_index[seat_index][i+1]==1)&&(i+2<GameConstants.MAX_HH_INDEX/2&&table.GRR._cards_index[seat_index][i+2]==1))
				{
					i+=1;
					continue;
				}
				if(table.GRR._cards_index[seat_index][i] + table.GRR._cards_index[seat_index][i+10] == 3)
				{
					continue;
				}
				return table._logic.switch_to_card_data(i);
			}
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX;i++)
		{
			if (table.GRR._cards_index[seat_index][i] == 1)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		return 1;
	}
	public static void  get_chi_cards(int chi_type,int center_card,int cards[])
	{
		int count = 0;
		switch(chi_type)
		{
		case GameConstants.WIK_LEFT:
		{
			cards[0] = center_card ;
			cards[1] = center_card+1;
			cards[2] = center_card+2;
			break;
		}
		case GameConstants.WIK_CENTER:
		{
			cards[0] = center_card-1;
			cards[1] = center_card ;
			cards[2] = center_card+1;
			break;
		}
		case GameConstants.WIK_RIGHT:
		{
			cards[0] = center_card-2;
			cards[1] = center_card-1 ;
			cards[2] = center_card;
			break;
		}
		case GameConstants.WIK_DDX:
		{
			if(center_card>16)
			{
				cards[0] = center_card-16;
				cards[1] = center_card;
				cards[2] = center_card;
			}
			else
			{
				cards[0] = center_card;
				cards[1] = center_card+16;
				cards[2] = center_card+16;
			}				
			break;
		}
		case GameConstants.WIK_XXD:
		{
			if(center_card>16)
			{
				cards[0] = center_card-16;
				cards[1] = center_card-16;
				cards[2] = center_card;
			}
			else
			{
				cards[0] = center_card;
				cards[1] = center_card;
				cards[2] = center_card+16;
			}				
			break;
		}
		case GameConstants.WIK_EQS:
		{
			if(center_card>16)
			{
				cards[0] = 0x12;
				cards[1] = 0x17;
				cards[2] = 0x1a;
			}
			else{
				cards[0] = 0x02;
				cards[1] = 0x07;
				cards[2] = 0x0a;
			}
		}			
		}
	}
	public static void AI_Operate_Card(PHTable table,int seat_index){

			
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = GameConstants.WIK_ZI_MO;
		int operate_card = playerStatus.get_operate_card();
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_status_operate_card(seat_index, operate_code, operate_card, -1,table._playerStatus[seat_index].get_handler_status());
			return ;
		}
		operate_code = GameConstants.WIK_CHI_HU;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_status_operate_card(seat_index, operate_code, operate_card, -1,table._playerStatus[seat_index].get_handler_status());
			return;
		}
		operate_code = GameConstants.WIK_PENG;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_status_operate_card(seat_index, operate_code, operate_card, -1,table._playerStatus[seat_index].get_handler_status());
			return;
		}
		int chi_pai_type = 0;
		int chi_luo_pai = -1;
		
		
		for(int i = 0; i<playerStatus._action_count;i++)
		{
			switch(playerStatus._action[i])
			{
			case GameConstants.WIK_LEFT:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_LEFT;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
					count++;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][0].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][0].nLouWeaveKind[j][1] != 0)
					{
						chi_luo_pai = j;
						break;
					}
					if(table._lou_weave_item[seat_index][0].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][0].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}
			case GameConstants.WIK_CENTER:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_CENTER;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][1].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][1].nLouWeaveKind[j][1] != 0)
					{
						chi_pai_type = j;
						break;
					}
					if(table._lou_weave_item[seat_index][1].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][1].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}	
			case GameConstants.WIK_RIGHT:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_RIGHT;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
					count++;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][2].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][2].nLouWeaveKind[j][1] != 0)
					{
						chi_luo_pai = j;
						break;
					}
					if(table._lou_weave_item[seat_index][2].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][2].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}
			case GameConstants.WIK_DDX:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_DDX;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
					count++;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][5].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][5].nLouWeaveKind[j][1] != 0)
					{
						chi_luo_pai = j;
						break;
					}
					if(table._lou_weave_item[seat_index][5].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][5].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}
			case GameConstants.WIK_XXD:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_XXD;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
					count++;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][4].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][4].nLouWeaveKind[j][1] != 0)
					{
						chi_luo_pai = j;
						break;
					}
					if(table._lou_weave_item[seat_index][4].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][4].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}
			case GameConstants.WIK_EQS:
			{
				int cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
				Arrays.fill(cards_index, 0);
				int count = 0;
				int temp_cards[] = new int[3];
				chi_pai_type = GameConstants.WIK_EQS;
				get_chi_cards(chi_pai_type,operate_card,temp_cards);
				for(int j = 0 ;j < 3; j++){
					cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
					count++;
				}
				for(int j = 0; j<table._lou_weave_item[seat_index][3].nCount; j++)
				{
					
					if(table._lou_weave_item[seat_index][3].nLouWeaveKind[j][1] != 0)
					{
						chi_luo_pai = j;
						break;
					}
					if(table._lou_weave_item[seat_index][3].nLouWeaveKind[j][0] != 0)
					{
						chi_luo_pai = j;
						get_chi_cards(table._lou_weave_item[seat_index][3].nLouWeaveKind[j][0],operate_card,temp_cards);
						for(int k = 0 ;k < 3; k++){
							cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
							count++;
						}
						break;
					}
					
				}
				cards_index[table._logic.switch_to_card_index(operate_card)]--;
				int flag_count = 0;
				for(int j = 0 ;j < GameConstants.MAX_HH_INDEX;j++)
				{
					if(cards_index[j] == 0)
						continue;
					if(cards_index[j] != table.GRR._cards_index[seat_index][j])
						flag_count ++ ;
					if(flag_count == 3)
						break;
						
				}
				if(flag_count < count/3||flag_count == 0)
				{
					operate_code = chi_pai_type;
					if (playerStatus.has_action_by_code(operate_code) != false) {
						table.handler_status_operate_card(seat_index, operate_code, operate_card, chi_luo_pai,table._playerStatus[seat_index].get_handler_status());
						return;
					}
				}
				break;
			}
			}
		}		
		operate_code = GameConstants.WIK_NULL;
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_status_operate_card(seat_index, operate_code, operate_card, -1,table._playerStatus[seat_index].get_handler_status());
			return;
		}
	}
	public static void AI_WARNING(PHTable table,int seat_index){
		if (table._warning[seat_index] != 1)
			return ;
		table.handler_ask_player(seat_index, true);
	}

	//拆牌
	
	//机器人出牌算法

}
