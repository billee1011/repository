/**
 * 
 */
package com.cai.game.hh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.PlayerStatus;
import com.cai.common.domain.WeaveItem;
import com.cai.game.hh.HHGameLogic.AnalyseItem;



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
class KindItemAI {
	public int cbWeaveKind;// 组合类型
	public int cbCenterCard;// 中心扑克
	public int cbHuXi ; //胡息
	public KindItemAI() {

	}
};

public class HHAIGameLogic {
	

	

	public static void AI_Out_Card(HHTable table,int seat_index){
		
		if (table._playerStatus[seat_index].get_status() != GameConstants.Player_Status_OUT_CARD)
			return ;
		int card = 0;
		if(table.isCoinRoom()){
			card = get_card_coin(table,seat_index);
		}
		else{
			card = get_card(table,seat_index);
		}
		
		table.handler_player_out_card(seat_index, card);
	}
	// 分析扑克
	public static boolean analyse_card_phz(HHTable table,int cards_index[],List<AnalyseItem> analyseItemArray, boolean yws_type,int analyse_count[]) {
		// 计算数目
		int cbCardCount = table._logic.get_card_count_by_index(cards_index);
		// 跑胡判断
		analyse_count[0] = 0;
		// 需求判断
		if (cbCardCount == 0)
			return false;
		int cbLessKindItem = (cbCardCount) / 3;
		boolean bNeedCardEye = ((cbCardCount + 1) % 3 == 0);

		// 单吊判断
		if(cbCardCount < 3)
			return false;

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[76];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 拆分分析
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 0)
				continue;
			int card_date = table._logic.switch_to_card_data(i);
			// 大小搭吃
			if ((i < 10) && ((cards_index[i] == 2) )
					&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] >= 1) ) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = i;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = i;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i >= 10 ? GameConstants.WIK_DDX : GameConstants.WIK_XXD;
				kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
				cbKindItemCount++;
			}
			// 大小搭吃
			if ((i < 10) && (cards_index[i] >= 1)
					&& (cards_index[(i + 10) % GameConstants.MAX_HH_INDEX] == 2)) {
				kindItem[cbKindItemCount].cbCardIndex[0] = i;
				kindItem[cbKindItemCount].cbCardIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbCardIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[0] = i;
				kindItem[cbKindItemCount].cbValidIndex[1] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbValidIndex[2] = (i + 10) % GameConstants.MAX_HH_INDEX;
				kindItem[cbKindItemCount].cbWeaveKind = i > 10 ? GameConstants.WIK_XXD : GameConstants.WIK_DDX;
				kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);

				cbKindItemCount++;
			}
			if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x02) {
				for (int j = 1; j <= cards_index[i]; j++) {
					if ((cards_index[i + 5] >= j)&& (cards_index[i + 8] >= j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 5;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 8;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_EQS;
						kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}
			if (yws_type == true) {
				if ((card_date & GameConstants.LOGIC_MASK_VALUE) == 0x01) {
					for (int j = 1; j <= cards_index[i]; j++) {
						if ((cards_index[i + 4] >= j)&& (cards_index[i + 9] >= j)){
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbValidIndex[0] = i;
							kindItem[cbKindItemCount].cbValidIndex[1] = i + 4;
							kindItem[cbKindItemCount].cbValidIndex[2] = i + 9;
							kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_YWS;
							kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
							cbKindItemCount++;
						}
					}
				}
			}
			// 顺子判断
			if ((i < (GameConstants.MAX_HH_INDEX - 2)) && (cards_index[i] > 0) && ((i % 10) <= 7)) {
				for (int j = 1; j <= cards_index[i]; j++) {

					if ((cards_index[i + 1] >= j)&& (cards_index[i + 2] >= j)) {
						kindItem[cbKindItemCount].cbCardIndex[0] = i;
						kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbValidIndex[0] = i;
						kindItem[cbKindItemCount].cbValidIndex[1] = i + 1;
						kindItem[cbKindItemCount].cbValidIndex[2] = i + 2;
						kindItem[cbKindItemCount].cbWeaveKind = GameConstants.WIK_LEFT;
						kindItem[cbKindItemCount].cbCenterCard = table._logic.switch_to_card_data(i);
						cbKindItemCount++;
					}
				}
			}

		}
		// 组合分析
		boolean is_max_weave = false;
		if(cbKindItemCount < cbLessKindItem)
			cbLessKindItem = cbKindItemCount;
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[GameConstants.MAX_HH_INDEX];

			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6 };
			KindItem pKindItem[] = new KindItem[cbIndex.length];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}
			// 把剩余需要判断的组合开始分析 组合
			// 开始组合
			do {
				int hh_count = GameConstants.MAX_HH_INDEX;
				// 设置变量
				for (int i = 0; i < hh_count; i++) {
					cbCardIndexTemp[i] = cards_index[i];
				}
				for (int i = 0; i < cbLessKindItem; i++) {
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 3; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}

				}

				// 数量判断
				boolean bEnoughCard = true;
				for (int i = 0; i < cbLessKindItem * 3; i++) {
					// 存在判断
					int cbCardIndex = pKindItem[i / 3].cbValidIndex[i % 3];

					if (cbCardIndexTemp[cbCardIndex] == 0) {
						bEnoughCard = false;
						break;
					} else
						cbCardIndexTemp[cbCardIndex]--;
				}

				if ((bEnoughCard == true)) {
					// 牌眼判断
					AnalyseItem analyseItem = new AnalyseItem();
					// 设置组合
					int count = 0;
					// 设置牌型
					for (int i = 0; i < cbLessKindItem; i++) {
						analyseItem.cbWeaveKind[i] = pKindItem[i].cbWeaveKind;
						analyseItem.cbCenterCard[i] = pKindItem[i].cbCenterCard;
						WeaveItem weave_item = new WeaveItem();
						weave_item.weave_kind = pKindItem[i].cbWeaveKind;
						weave_item.center_card = pKindItem[i].cbCenterCard;
						analyseItem.hu_xi[i ] = table._logic.get_weave_hu_xi(weave_item);
						table._logic.get_weave_card(pKindItem[i].cbWeaveKind,  pKindItem[i].cbCenterCard, analyseItem.cbCardData[i]);
						count++;

					}
					is_max_weave = true;
					analyse_count[0] = cbLessKindItem;
					// 插入结果
					analyseItemArray.add(analyseItem);
				}
				if(cbLessKindItem<=1)
					break;
				// 设置索引
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++)
								cbIndex[j] = cbNewIndex + j - i + 2;
							break;
						}
					}
					if (i == 0)
					{
						if(is_max_weave == true)
							break;
						
						cbLessKindItem--;
						for(int j = 0; j<7;j++)
							cbIndex[j] = j;
						if(cbLessKindItem<2)
							break;
					}
				} else
					cbIndex[cbLessKindItem - 1]++;
			} while (true);
		}
		
		return (analyseItemArray.size() > 0 ? true : false);
	} 
	public static int get_card_coin(HHTable table,int seat_index){
		KindItemAI kind_item[] = new KindItemAI[GameConstants.MAX_WEAVE_HH];
		for(int i = 0; i<GameConstants.MAX_WEAVE_HH;i++)
		{
			kind_item[i] = new KindItemAI();
		}
		int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
		Arrays.fill(cards_index, 0);
		int kind_count = 0;
		int hu_xi = 0;
		int add_peng_hu_xi = 0;
		int add_sao_hu_xi = 0;
		for(int i = 0; i < table.GRR._weave_count[seat_index];i++){
			hu_xi += table.GRR._weave_items[seat_index][i].hu_xi;
			if(table.GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_PENG
					&&table._all_out_card_index[table._logic.switch_to_card_index(table.GRR._weave_items[seat_index][i].center_card)]==0)
			{
				if(table.GRR._weave_items[seat_index][i].center_card>16)
					add_peng_hu_xi += 6;
				else 
					add_peng_hu_xi += 5;
			}
			if(table.GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_SAO
					&&table._all_out_card_index[table._logic.switch_to_card_index(table.GRR._weave_items[seat_index][i].center_card)]==0)
			{
				if(table.GRR._weave_items[seat_index][i].center_card>16)
					add_sao_hu_xi += 3;
				else 
					add_sao_hu_xi += 6;
			}
		}
		for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++){
			if(table.GRR._cards_index[seat_index][i]==4)
			{
				kind_item[kind_count].cbCenterCard = table._logic.switch_to_card_data(i);
				kind_item[kind_count].cbWeaveKind = GameConstants.WIK_AN_LONG;
				kind_item[kind_count].cbHuXi = (i>=10)?12:9;
				hu_xi+=kind_item[kind_count].cbHuXi; 
				kind_count++;
			}
			else if(table.GRR._cards_index[seat_index][i]==3)
			{
				kind_item[kind_count].cbCenterCard = table._logic.switch_to_card_data(i);
				kind_item[kind_count].cbWeaveKind = GameConstants.WIK_KAN;
				kind_item[kind_count].cbHuXi = (i>=10)?6:3;
				if(table._all_out_card_index[i]==0)
				{
					if(i >= 10)
						add_sao_hu_xi += 3;
					else 
						add_sao_hu_xi += 6;
				}
				hu_xi+=kind_item[kind_count].cbHuXi;
				kind_count++;
			}
			else cards_index[i] = table.GRR._cards_index[seat_index][i];
		}
		boolean yws_type = table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1?true:false;
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		int analyse_count[] = new int[1];
		boolean value = analyse_card_phz( table,cards_index, analyseItemArray,  yws_type,analyse_count );
		int max_hu_index = 0;
		int max_hu_xi = 0;
		if(value == true)
		{		
			for(int i = 0; i< analyseItemArray.size()&&analyseItemArray.size()>1;i++)
			{
				int temp_hu_xi = 0;
				AnalyseItem analyseItem = analyseItemArray.get(i);
				for(int j = 0; j<analyse_count[0];j++)
				{
					if(analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
						continue;
					temp_hu_xi += analyseItem.hu_xi[j];	
				}
				if(temp_hu_xi>max_hu_xi)
				{
					max_hu_xi = temp_hu_xi;
					max_hu_index = i;
				}
			}
		}
		boolean is_delete_all = false;
		int temp_count = table._logic.get_card_count_by_index(cards_index);
		if((temp_count - analyse_count[0]*3)<=3)
		    is_delete_all = true;
		if(table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_HUXI)==1)
		{
			if(hu_xi+max_hu_xi>9&&add_sao_hu_xi+add_peng_hu_xi+hu_xi+max_hu_xi>=18)
				is_delete_all = true;
		}
		else
		{
			if(hu_xi+max_hu_xi>6&&add_sao_hu_xi+add_peng_hu_xi+hu_xi+max_hu_xi>=15)
				is_delete_all = true;
		}
		if(analyseItemArray.size()>0)
		{
			AnalyseItem analyseItem = analyseItemArray.get(max_hu_index);
			for(int j = 0; j<analyse_count[0];j++)
			{
				if(analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
					continue;
				if(is_delete_all == true ||analyseItem.hu_xi[j] != 0)
				{
					for(int i = 0 ;i < 3; i++)
					{
						cards_index[table._logic.switch_to_card_index(analyseItem.cbCardData[j][i])]--;
					}
				}
					
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			
			if (cards_index[i] == 1)
			{
				if(i == 0)
				{
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[4] ==1)
							continue;
						if(cards_index[9] == 1)
							continue;
					}
				}
				if(i == 1)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
					
				}
				if(i == 4)
				{
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[0] ==1)
							continue;
						if(cards_index[9] == 1)
							continue;
					}
				}
				if(i == 6)
				{
					if(cards_index[1] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
				}
				if(i == 9)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[1] == 1)
						continue;
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[0] ==1)
							continue;
						if(cards_index[4] == 1)
							continue;
					}
				}
				if(i-1>=0&&cards_index[i-1]==1)
					continue;
				if(i-2>=0&&cards_index[i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX/2&&cards_index[i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX/2&&cards_index[i+2]==1)
					continue;
				if(cards_index[i]==1&&cards_index[i+10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if(i == 10){
				if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
				{
					if(cards_index[14] ==1)
						continue;
					if(cards_index[19] == 1)
						continue;
				}
				if(cards_index[11] == 1)
					continue;
				if(cards_index[12] ==1)
					continue;
			}
			if(i == 11)
			{
				if(cards_index[16] == 1)
					continue;
				if(cards_index[19] == 1)
					continue;
				if(cards_index[10] == 1)
					continue;
				if(cards_index[12] ==1)
					continue;
			}
			if(i == 13)
			{
				if(cards_index[10] == 1)
					continue;
				if(cards_index[11] ==1)
					continue;
			}
			if(i == 14)
			{
				if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
				{
					if(cards_index[10] ==1)
						continue;
					if(cards_index[19] == 1)
						continue;
				}
			}
			if(i == 16)
			{
				if(cards_index[11] == 1)
					continue;
				if(cards_index[19] == 1)
					continue;
			}
			if(i == 19)
			{
				if(cards_index[16] == 1)
					continue;
				if(cards_index[11] == 1)
					continue;
				if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
				{
					if(cards_index[10] ==1)
						continue;
					if(cards_index[14] == 1)
						continue;
				}
			}
			if (cards_index[i] == 1)
			{
				if(i-1>=10&&cards_index[i-1]==1)
					continue;
				if(i-2>=10&&cards_index[i-2]==1)
					continue;
				if(i+1<GameConstants.MAX_HH_INDEX&&cards_index[i+1]==1)
					continue;
				if(i+2<GameConstants.MAX_HH_INDEX&&cards_index[i+2]==1)
					continue;
				if(cards_index[i]==1&&cards_index[i-10]>=1)
					continue;
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX/2;i++){
			if (cards_index[i] == 1)
			{
				
				if((i+1<GameConstants.MAX_HH_INDEX/2&&cards_index[i+1]==1)&&(i+2<GameConstants.MAX_HH_INDEX/2&&cards_index[i+2]==1))
				{
					i+=1;
					continue;
				}
				if(cards_index[i] + cards_index[i+10] == 3)
				{
					continue;
				}
				if(i == 0)
				{
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[4] ==1)
							continue;
						if(cards_index[9] == 1)
							continue;
						
					}
					if(cards_index[1] ==1)
						continue;
					if(cards_index[2] ==1)
						continue;
				}
				if(i == 1)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
					if(cards_index[0] ==1)
						continue;
					if(cards_index[2] ==1)
						continue;
					
				}
				if(i == 2)
				{
					if(cards_index[0] ==1)
						continue;
					if(cards_index[1] ==1)
						continue;
				}
				if(i == 4)
				{
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[0] ==1)
							continue;
						if(cards_index[9] == 1)
							continue;
					}
				}
				if(i == 6)
				{
					if(cards_index[1] == 1)
						continue;
					if(cards_index[9] == 1)
						continue;
				}
				if(i == 9)
				{
					if(cards_index[6] == 1)
						continue;
					if(cards_index[1] == 1)
						continue;
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[0] ==1)
							continue;
						if(cards_index[4] == 1)
							continue;
					}
				}
				return table._logic.switch_to_card_data(i);
			}
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX/2;i++){
			if (cards_index[i] == 1)
			{
				
				if(cards_index[i] + cards_index[i+10] == 3)
				{
					return table._logic.switch_to_card_data(i);
				}
				
				
			}
		}
		for(int i = 10; i < GameConstants.MAX_HH_INDEX;i++){
			if (cards_index[i] == 1)
			{
				
				if(cards_index[i] + cards_index[i-10] == 3)
				{
					return table._logic.switch_to_card_data(i);
				}
				
				
			}
		}
		for(int i = 10 ; i< GameConstants.MAX_HH_INDEX;i++)
		{
			if (cards_index[i] == 1)
			{
				if((i+1<GameConstants.MAX_HH_INDEX&&cards_index[i+1]==1)&&i+2<GameConstants.MAX_HH_INDEX&&cards_index[i+2]==1)
				{
					i += 1;
					continue;
				}
				if(cards_index[i] + cards_index[i-10] == 3)
				{
					continue;
				}
				if(i == 10){
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[14] ==1)
							continue;
						if(cards_index[19] == 1)
							continue;
					}
				}
				if(i == 11)
				{
					if(cards_index[16] == 1)
						continue;
					if(cards_index[19] == 1)
						continue;
				}
				if(i == 14)
				{
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[10] ==1)
							continue;
						if(cards_index[19] == 1)
							continue;
					}
				}
				if(i == 16)
				{
					if(cards_index[11] == 1)
						continue;
					if(cards_index[19] == 1)
						continue;
				}
				if(i == 19)
				{
					if(cards_index[16] == 1)
						continue;
					if(cards_index[11] == 1)
						continue;
					if( table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1)
					{
						if(cards_index[10] ==1)
							continue;
						if(cards_index[14] == 1)
							continue;
					}
				}
			return table._logic.switch_to_card_data(i);
			}
				
		}
		
		for(int i = 0; i < GameConstants.MAX_HH_INDEX;i++)
		{
			if (cards_index[i] == 1)
			{
				if(get_special_cards(table._logic.switch_to_card_data(i),table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1))
					continue;
				return table._logic.switch_to_card_data(i);
			}
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX;i++)
		{
			if (cards_index[i] == 1)
			{
				return table._logic.switch_to_card_data(i);
			}
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			if (cards_index[i] == 2&&cards_index[i+10] == 2)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (cards_index[i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		for(int i = 0; i < GameConstants.MAX_HH_INDEX;i++)
		{
			if (table.GRR._cards_index[seat_index][i] == 1)
			{
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2&&table.GRR._cards_index[seat_index][i+10] == 2)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		return 1;
	}
	public static int get_card(HHTable table,int seat_index) {
		
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
			{
				return table._logic.switch_to_card_data(i);
			}
				
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX/2; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2&&table.GRR._cards_index[seat_index][i+10] == 2)
				return table._logic.switch_to_card_data(i);
		}
		for (int i = 0; i < GameConstants.MAX_HH_INDEX; i++) {
			if (table.GRR._cards_index[seat_index][i] == 2)
				return table._logic.switch_to_card_data(i);
		}
		return 1;
	}
	public static boolean get_special_cards(int card,boolean is_yws){
		switch(card){
		case 0x01:
		case 0x02:
		case 0x03:
		case 0x07:
		case 0x0a:
		case 0x11:
		case 0x12:
		case 0x13:
		case 0x17:
		case 0x1a: return true; 
		case 0x05:
		case 0x15: return ((is_yws == true)?( true):( false));
		}
		return false;
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
		case GameConstants.WIK_YWS:
		{
			if(center_card>16)
			{
				cards[0] = 0x11;
				cards[1] = 0x15;
				cards[2] = 0x1a;
			}
			else{
				cards[0] = 0x01;
				cards[1] = 0x05;
				cards[2] = 0x0a;
			}
		}
		case GameConstants.WIK_PENG:
		{
			cards[0] = center_card;
			cards[1] = center_card;
			cards[2] = center_card;
		}
		}
	}
	public static int  AI_out_card(int cards_index[]){
		
		return 0;
	}
	public static void AI_Coin_Operate_card(HHTable table,int seat_index){
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = GameConstants.WIK_ZI_MO;
		int lou_code = -1;
		int operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, lou_code);
			return ;
		}
		operate_code = GameConstants.WIK_CHI_HU;
		operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, lou_code);
			return;
		}
		operate_code = GameConstants.WIK_NULL;
		KindItemAI kind_item[] = new KindItemAI[GameConstants.MAX_WEAVE_HH];
		for(int i = 0; i<GameConstants.MAX_WEAVE_HH;i++)
		{
			kind_item[i] = new KindItemAI();
		}
		int cards_index[] = new int[GameConstants.MAX_HH_INDEX];
		Arrays.fill(cards_index, 0);
		int kind_count = 0;
		int hu_xi = 0;
		int add_peng_hu_xi = 0;
		int add_sao_hu_xi = 0;
		for(int i = 0; i < table.GRR._weave_count[seat_index];i++){
			hu_xi += table.GRR._weave_items[seat_index][i].hu_xi;
			if(table.GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_PENG
					&&table._all_out_card_index[table._logic.switch_to_card_index(table.GRR._weave_items[seat_index][i].center_card)]==0)
			{
				if(table.GRR._weave_items[seat_index][i].center_card>16)
					add_peng_hu_xi += 6;
				else 
					add_peng_hu_xi += 5;
			}
			if(table.GRR._weave_items[seat_index][i].weave_kind == GameConstants.WIK_SAO
					&&table._all_out_card_index[table._logic.switch_to_card_index(table.GRR._weave_items[seat_index][i].center_card)]==0)
			{
				if(table.GRR._weave_items[seat_index][i].center_card>16)
					add_sao_hu_xi += 3;
				else 
					add_sao_hu_xi += 6;
			}
		}
		for(int i = 0; i< GameConstants.MAX_HH_INDEX;i++){
			if(table.GRR._cards_index[seat_index][i]==4)
			{
				kind_item[kind_count].cbCenterCard = table._logic.switch_to_card_data(i);
				kind_item[kind_count].cbWeaveKind = GameConstants.WIK_AN_LONG;
				kind_item[kind_count].cbHuXi = (i>=10)?12:9;
				hu_xi+=kind_item[kind_count].cbHuXi; 
				kind_count++;
			}
			else if(table.GRR._cards_index[seat_index][i]==3)
			{
				kind_item[kind_count].cbCenterCard = table._logic.switch_to_card_data(i);
				kind_item[kind_count].cbWeaveKind = GameConstants.WIK_KAN;
				kind_item[kind_count].cbHuXi = (i>=10)?6:3;
				if(table._all_out_card_index[i]==0)
				{
					if(i >= 10)
						add_sao_hu_xi += 3;
					else 
						add_sao_hu_xi += 6;
				}
				hu_xi+=kind_item[kind_count].cbHuXi;
				kind_count++;
			}
			else cards_index[i] = table.GRR._cards_index[seat_index][i];
		}
		boolean yws_type = table.getRuleValue(GameConstants.GAME_RULE_YOUXIAN_PHZ_SELECT_YWS)==1?true:false;
		List<AnalyseItem> analyseItemArray = new ArrayList<AnalyseItem>();
		int analyse_count[] = new int[1];
		boolean value = analyse_card_phz( table,cards_index, analyseItemArray,  yws_type,analyse_count );
		int max_hu_index = 0;
		int max_hu_xi = 0;
		if(value == true)
		{		
			for(int i = 0; i< analyseItemArray.size()&&analyseItemArray.size()>1;i++)
			{
				int temp_hu_xi = 0;
				AnalyseItem analyseItem = analyseItemArray.get(i);
				for(int j = 0; j<analyse_count[0];j++)
				{
					if(analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
						continue;
					temp_hu_xi += analyseItem.hu_xi[j];	
				}
				if(temp_hu_xi>max_hu_xi)
				{
					max_hu_xi = temp_hu_xi;
					max_hu_index = i;
				}
			}
		}
		int last_card_count = table._logic.get_card_count_by_index(cards_index)-analyse_count[0]*3;
		int mid_hu_index = 0;
		int mid_hu_xi = 0;
		int mid_operate_code = 0;
		int mid_lou_code = -1;
		int min_operate_code = 0;
		int min_lou_code = -1;
		int min_card_count = last_card_count;
		int min_hu_xi = 0;
		for(int i = 0; i<playerStatus._action_count;i++){
			if(playerStatus._action[i] == GameConstants.WIK_NULL)
				continue;
			int temp_hu_xi = 0;
			int temp_cards_index[]  = new int[GameConstants.MAX_HH_INDEX];
			Arrays.fill(temp_cards_index, 0);
			int count = 0;
			int temp_cards[] = new int[3];
			int chi_pai_type = playerStatus._action[i];
			int temp_operate_code = playerStatus._action[i];
			operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
			temp_hu_xi = table._logic.get_analyse_hu_xi(temp_operate_code, operate_card);
			get_chi_cards(chi_pai_type,operate_card,temp_cards);
			for(int j = 0 ;j < 3; j++){
				temp_cards_index[table._logic.switch_to_card_index(temp_cards[j])] ++ ;
			}
			int chi_luo_pai = -1;
			int chi_type = table._logic.get_chi_index(temp_operate_code);
			for(int j = 0; chi_type!=-1&&j<table._lou_weave_item[seat_index][chi_type].nCount; j++)
			{
				
				if(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][1] != 0)
				{
					chi_luo_pai = j;
					get_chi_cards(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][1],operate_card,temp_cards);
					temp_hu_xi += table._logic.get_analyse_hu_xi(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][1], operate_card);
					for(int k = 0 ;k < 3; k++){
						temp_cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
						count++;
					}
					break;
				}
				if(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][0] != 0)
				{
					chi_luo_pai = j;
					get_chi_cards(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][0],operate_card,temp_cards);
					temp_hu_xi += table._logic.get_analyse_hu_xi(table._lou_weave_item[seat_index][chi_type].nLouWeaveKind[j][0], operate_card);
					for(int k = 0 ;k < 3; k++){
						temp_cards_index[table._logic.switch_to_card_index(temp_cards[k])] ++ ;
						count++;
					}
					break;
				}
				
			}
			temp_cards_index[table._logic.switch_to_card_index(operate_card)]--;
			int mid_cards_index[] = new int[GameConstants.MAX_HH_INDEX];
			Arrays.fill(mid_cards_index, 0);
			for(int j = 0; j<GameConstants.MAX_HH_INDEX;j++)
			{
				mid_cards_index[j] = cards_index[j] - temp_cards_index[j];
			}
			min_operate_code = temp_operate_code;
			min_lou_code = chi_luo_pai;
			min_card_count = table._logic.get_card_count_by_index(mid_cards_index);
			min_hu_xi = temp_hu_xi;
			mid_hu_xi = temp_hu_xi;
			mid_operate_code = temp_operate_code;
			mid_lou_code = chi_luo_pai;
			List<AnalyseItem> min_analyseItemArray = new ArrayList<AnalyseItem>();
			int mid_analyse_count[] = new int[1];
			boolean min_value = analyse_card_phz( table,mid_cards_index, min_analyseItemArray,  yws_type,mid_analyse_count );
			if(value == true)
			{		
				int mid_min_hu_xi = 0;
				for(int k = 0; k< min_analyseItemArray.size()&&min_analyseItemArray.size()>1;k++)
				{
					int mid_temp_hu_xi = 0;
					AnalyseItem analyseItem = min_analyseItemArray.get(k);
					for(int j = 0; j<mid_analyse_count[0];j++)
					{
						if(analyseItem.cbWeaveKind[j] == GameConstants.WIK_NULL)
							continue;
						mid_temp_hu_xi += analyseItem.hu_xi[j];	
					}
					if(mid_temp_hu_xi>mid_min_hu_xi)
						mid_min_hu_xi = mid_temp_hu_xi;
					if(mid_temp_hu_xi+temp_hu_xi>mid_hu_xi)
					{
						mid_hu_xi = mid_temp_hu_xi+temp_hu_xi;
						mid_hu_index = k;
						mid_operate_code = temp_operate_code;
						mid_lou_code = chi_luo_pai;
					}
				}
				if((table._logic.get_card_count_by_index(mid_cards_index)-mid_analyse_count[0]*3<min_card_count)
						||(table._logic.get_card_count_by_index(mid_cards_index)-mid_analyse_count[0]*3==min_card_count&&min_hu_xi<mid_min_hu_xi+temp_hu_xi)){
					min_operate_code = temp_operate_code;
					min_lou_code = chi_luo_pai;
					min_card_count = table._logic.get_card_count_by_index(mid_cards_index)-mid_analyse_count[0]*3;
					min_hu_xi = mid_min_hu_xi+temp_hu_xi;
				}
					
			}
		}
		
		if(min_card_count <= 3&&min_card_count<=last_card_count&&min_operate_code!=0)
		{	
				operate_code = min_operate_code;
				lou_code = min_lou_code;
				
					
			
		}
		else if(min_card_count<=last_card_count){
			if(mid_hu_xi>max_hu_xi)
			{
				operate_code = mid_operate_code;
				lou_code = mid_lou_code;
			}
			else if(min_card_count<last_card_count &&( (mid_hu_xi+max_hu_xi < 3)||(mid_hu_xi==max_hu_xi)))
			{
				operate_code = mid_operate_code;
				lou_code = mid_lou_code;
			}
			else if(mid_hu_xi<max_hu_xi&&hu_xi+mid_hu_xi>9&&add_sao_hu_xi+add_peng_hu_xi+hu_xi+mid_hu_xi>=18){
				operate_code = mid_operate_code;
				lou_code = mid_lou_code;
			}
			else if(mid_hu_xi<max_hu_xi&&hu_xi+mid_hu_xi>6&&add_sao_hu_xi+add_peng_hu_xi+hu_xi+mid_hu_xi>=15){
				operate_code = mid_operate_code;
				lou_code = mid_lou_code;
			}
			
		}
		else if(min_card_count>last_card_count)
		{
			if(mid_hu_xi>max_hu_xi)
			{
				operate_code = mid_operate_code;
				lou_code = mid_lou_code;
			}
		}
		operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, lou_code);
			return;
		}
	

		
	}

	public static void AI_Operate_Card(HHTable table,int seat_index){
		if(table.isCoinRoom())
		{
			AI_Coin_Operate_card(table , seat_index);
			return ;
		}
		PlayerStatus playerStatus = table._playerStatus[seat_index];
		int operate_code = GameConstants.WIK_ZI_MO;
		int operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return ;
		}
		operate_code = GameConstants.WIK_CHI_HU;
		operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
		operate_code = GameConstants.WIK_PENG;
		operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
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
				operate_card = playerStatus.get_weave_card(playerStatus._action[i]);
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
						table.handler_operate_card(seat_index, operate_code, operate_card, chi_luo_pai);
						return;
					}
				}
				break;
			}
			}
		}		
		operate_code = GameConstants.WIK_NULL;
		operate_card = playerStatus.get_weave_card(operate_code);
		if (playerStatus.has_action_by_code(operate_code) != false) {
			table.handler_operate_card(seat_index, operate_code, operate_card, -1);
			return;
		}
	}

	//拆牌
	
	//机器人出牌算法

}
