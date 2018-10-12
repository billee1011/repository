package com.cai.game.dbd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.domain.ChiHuRight;
import com.cai.common.domain.GangCardResult;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;

public class DBDGameLogic {
	private static Logger logger = Logger.getLogger(DBDGameLogic.class);


	/***
	 * 扑克转换--将实际数据 转换为 索引
	 * 
	 * @param card
	 * @return
	 */
	public void switch_to_card_index(int card_data[],int card_count,int card_index[]) {
		for(int i=0;i<card_count;i++){
			int index = GetCardLogicValue(card_data[i]);
			card_index[index-3]++;
		}
	}
	public int get_card_index(int card_data){
		int index = GetCardLogicValue(card_data);
		return index-3;
	}
	public int get_index_card_count(int card_index[]){
		int card_count=0;
		for(int i=0;i<GameConstants.DBD_MAX_INDEX;i++){
			card_count+=card_index[i];
		}
		return card_count;
	}
	public int get_magic_card_count(int card_index[]){
		return card_index[13];
	}
	public boolean search_card_data(int card_data[],int real_data[], int cardCount,int hand_card_data[],int hand_card_count){
		int card_type=this.GetCardType_DBD(card_data,real_data, cardCount);
		int card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		int hand_index[]=new int[GameConstants.DBD_MAX_INDEX];
		switch_to_card_index(card_data,cardCount,card_index);
		switch_to_card_index(hand_card_data,hand_card_count,hand_index);
		int magic_count=this.get_magic_card_count(hand_index);
		switch(card_type){
		case GameConstants.DBD_CT_SINGLE:{
			int index = get_card_index(card_data[0]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]+magic_count>2){
					return true;
				}
				if(i>index && hand_index[i]>0){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_DOUBLE:{
			int index = get_card_index(card_data[0]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]+magic_count>2){
					return true;
				}
				if(i>index && (hand_index[i]>1 || (hand_index[i]>0 && magic_count>0))){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_BOMB_3:{
			int index = get_card_index(card_data[0]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]+magic_count>3){
					return true;
				}
				if(i>index && (hand_index[i]+magic_count>2)){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_BOMB_4_RUAN:{
			int index = get_card_index(card_data[0]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]>=4){
					return true;
				}
				if(i>index && (hand_index[i]+magic_count>3)){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_BOMB_4_YING:{
			int index = get_card_index(card_data[0]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(i>index && hand_index[i]>=4){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_SINGLE_LINK:{
			int index = get_card_index(card_data[cardCount-1]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]+magic_count>2){
					return true;
				}
			}
			
			int link_count=0;
			for(int i=index+1;i<GameConstants.DBD_MAX_INDEX-2;i++){
				if(hand_index[i]==0){
					if(link_count>=cardCount){
						return true;
					}
					link_count=0;
				}else{
					link_count++;
				}
				if(link_count>=cardCount){
					return true;
				}
			}
			return false;
		}
		case GameConstants.DBD_CT_DOUBLE_LINK:{
			int index = get_card_index(card_data[cardCount-1]);
			for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
				if(hand_index[i]+magic_count>2){
					return true;
				}
			}
			
			int link_count=0;
			for(int i=index+1;i<GameConstants.DBD_MAX_INDEX-2;i++){
				if(hand_index[i]>=2){
					if(link_count>=cardCount/2){
						return true;
					}
					link_count++;
					
				}else{
					link_count=0;
				}
				if(link_count>=cardCount/2){
					return true;
				}
			}
			return false;
		}
		}
		return false;
	}
	public boolean is_have_card(int cbCardData[],int cbMagicCardData[], int cbCardCount){
		int card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		int magic_index[]=new int[GameConstants.DBD_MAX_INDEX];
		switch_to_card_index(cbCardData,cbCardCount,card_index);
		switch_to_card_index(cbMagicCardData,cbCardCount,magic_index);
		
		
		int magic_count=this.get_magic_card_count(card_index);
		if(magic_count == cbCardCount){
			return true;
		}
		for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
			if(magic_index[i]-card_index[i] <= magic_count && magic_index[i]-card_index[i]>=0){
				magic_count-=magic_index[i]-card_index[i];
			}else{
				return false;
			}
		}
		if(magic_count != 0){
			return false;
		}

		return true;
	}
	//获取类型
	public int GetCardType_DBD(int cbCardData[], int cbrealCardData[],int cbCardCount){
		int card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		int real_index[]=new int[GameConstants.DBD_MAX_INDEX];
		switch_to_card_index(cbCardData,cbCardCount,card_index);
		switch_to_card_index(cbrealCardData,cbCardCount,real_index);
		int magic_count=this.get_magic_card_count(real_index);
		if(cbCardCount == magic_count){
			if(cbCardCount == 1){
				return GameConstants.DBD_CT_SINGLE;
			}else if(cbCardCount == 2){
				return GameConstants.DBD_CT_DOUBLE;
			}else if(cbCardCount == 3){
				return GameConstants.DBD_CT_BOMB_3;
			}
		}
		for(int i=0;i<GameConstants.DBD_MAX_INDEX-1;i++){
			int cbCardCountTemp=cbCardCount;
			if(card_index[i]>0){
				if(cbCardCountTemp == card_index[i]){
					if(cbCardCountTemp == 1){
						return GameConstants.DBD_CT_SINGLE;
					}else if(cbCardCountTemp == 2){
						return GameConstants.DBD_CT_DOUBLE;
					}else if(cbCardCountTemp == 3){
						return GameConstants.DBD_CT_BOMB_3;
					}else if(cbCardCountTemp == 4){
						if(magic_count == 0){
							return GameConstants.DBD_CT_BOMB_4_YING;
						}else{
							return GameConstants.DBD_CT_BOMB_4_RUAN;
						}
					}else{
						return GameConstants.DBD_CT_ERROR;
					}
				}
				if(card_index[GameConstants.DBD_MAX_INDEX-2]>0){
					return GameConstants.DBD_CT_ERROR;	
				}
				cbCardCountTemp-=card_index[i];
				if(card_index[i]>=3){
					return GameConstants.DBD_CT_ERROR;	
				}else if(card_index[i]==2){
					for(int j=i+1;j<GameConstants.DBD_MAX_INDEX-2;j++){
						if(card_index[j]==2){
							cbCardCountTemp-=card_index[j];
							if(j == GameConstants.DBD_MAX_INDEX-3){
								if(cbCardCountTemp == 0){
									if(j-i<1){
										return GameConstants.DBD_CT_ERROR;
									}else{
										return GameConstants.DBD_CT_DOUBLE_LINK;
									}
								}else{
									return GameConstants.DBD_CT_ERROR;
								}
							}
						}else{
							if(cbCardCountTemp == 0){
								if(j-i<1){
									return GameConstants.DBD_CT_ERROR;
								}else{
									return GameConstants.DBD_CT_DOUBLE_LINK;
								}
							}else{
								return GameConstants.DBD_CT_ERROR;
							}	
						}
					}
				}else{
					for(int j=i+1;j<GameConstants.DBD_MAX_INDEX-2;j++){
						if(card_index[j]==1){
							cbCardCountTemp-=card_index[j];
						}else{
							if(cbCardCountTemp == 0){
								if(j-i<4){
									return GameConstants.DBD_CT_ERROR;
								}else{
									return GameConstants.DBD_CT_SINGLE_LINK;
								}
							}else{
								return GameConstants.DBD_CT_ERROR;
							}
							
						}
						if(j == GameConstants.DBD_MAX_INDEX-3 && cbCardCountTemp == 0){
							return GameConstants.DBD_CT_SINGLE_LINK;
						}
					}
				}
			}	
		}
		return GameConstants.DBD_CT_ERROR;
	}
	
	public boolean comparecarddata(int first_card[],int first_real_card[],int first_count,int next_card[],int next_real_card[],int next_count){
		int first_card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		int next_card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		switch_to_card_index(first_card,first_count,first_card_index);
		switch_to_card_index(next_card,next_count,next_card_index);
		
		int first_card_type=GetCardType_DBD(first_card,first_real_card,first_count);
		int next_card_type=GetCardType_DBD(next_card,next_real_card,next_count);
		
		if(next_card_type >= GameConstants.DBD_CT_BOMB_3 && first_card_type<GameConstants.DBD_CT_BOMB_3){
			return false;
		}
		if(next_card_type < GameConstants.DBD_CT_BOMB_3 && first_card_type>=GameConstants.DBD_CT_BOMB_3){
			return true;
		}
		if(next_card_type >= GameConstants.DBD_CT_BOMB_3 && first_card_type>=GameConstants.DBD_CT_BOMB_4_RUAN){
			if(next_card_type>first_card_type){
				return false;
			}else if(next_card_type<first_card_type){
				return true;
			}else{
				return this.GetCardLogicValue(first_card[0])>this.GetCardLogicValue(next_card[0]);
			}
		}
		if(next_card_type!=first_card_type){
			return false;
		}
		if(first_count!=next_count){
			return false;
		}
		switch(first_card_type){
		case GameConstants.DBD_CT_SINGLE:
		case GameConstants.DBD_CT_DOUBLE:{
			int first_value=this.GetCardLogicValue(first_card[0]);
			int next_value=this.GetCardLogicValue(next_card[0]);

			return first_value>next_value;
		}
		case GameConstants.DBD_CT_SINGLE_LINK:
		case GameConstants.DBD_CT_DOUBLE_LINK:{
			int first_value=this.GetCardLogicValue(first_card[0]);
			int next_value=this.GetCardLogicValue(next_card[0]);
			return first_value>next_value;
		}
		case GameConstants.DBD_CT_BOMB_3:
		case GameConstants.DBD_CT_BOMB_4_RUAN:
		case GameConstants.DBD_CT_BOMB_4_YING:{
			int first_value=this.GetCardLogicValue(first_card[2]);
			int next_value=this.GetCardLogicValue(next_card[2]);
			return first_value>next_value;
		}

	}
		
		
		return false;
	}
	public boolean isAllMagic(int cbCardData[], int cbCardCount){
		int card_index[]=new int[GameConstants.DBD_MAX_INDEX];
		switch_to_card_index(cbCardData,cbCardCount,card_index);
		if(cbCardCount == this.get_magic_card_count(card_index)){
			return true;
		}
		return false;
	}
	
	public int GetCardLogicValue(int CardData){
		//扑克属性
		int cbCardColor=GetCardColor(CardData);
		int cbCardValue=GetCardValue(CardData);

		//转换数值
		if (cbCardColor>=0x40) return cbCardValue+2;
		return (cbCardValue<=2)?(cbCardValue+13):cbCardValue;
	}
	//获取数值
	public int GetCardValue(int cbCardData){
		return cbCardData&GameConstants.LOGIC_MASK_VALUE; 
	}
	//获取花色
	public int GetCardColor(int cbCardData){
		return cbCardData&GameConstants.LOGIC_MASK_COLOR; 
	}
	
	
	// 洗牌
	public void random_card_data(int return_cards[], final int mj_cards[]) {
		int card_count = return_cards.length;
		int card_data[] = new int[card_count];
		for (int i = 0; i < card_count; i++) {
			card_data[i] = mj_cards[i];
		}
		random_cards(card_data, return_cards, card_count);

	}
	// 混乱准备
	private static void random_cards(int card_data[], int return_cards[], int card_count) {
		// 混乱扑克
		int bRandCount = 0, bPosition = 0;
		do {
			bPosition = (int) (RandomUtil.getRandomNumber(Integer.MAX_VALUE) % (card_count - bRandCount));
			return_cards[bRandCount++] = card_data[bPosition];
			card_data[bPosition] = card_data[card_count - bRandCount];
		} while (bRandCount < card_count);

	}
	/***
	 * 	//排列扑克
	 * 
	 * @param card_date
	 * @param card_count
	 * @return
	 */
	public void SortCardList(int cbCardData[], int cbCardCount)
	{
		//转换数值
		int cbLogicValue[]=new int[cbCardCount];
		for (int i=0;i<cbCardCount;i++) cbLogicValue[i]=GetCardLogicValue(cbCardData[i]);	

		//排序操作
		boolean bSorted=true;
		int cbTempData,bLast=cbCardCount-1;
		do
		{
			bSorted=true;
			for (int i=0;i<bLast;i++)
			{
				if ((cbLogicValue[i]<cbLogicValue[i+1])||
					((cbLogicValue[i]==cbLogicValue[i+1])&&(cbCardData[i]<cbCardData[i+1])))
				{
					//交换位置
					cbTempData=cbCardData[i];
					cbCardData[i]=cbCardData[i+1];
					cbCardData[i+1]=cbTempData;
					cbTempData=cbLogicValue[i];
					cbLogicValue[i]=cbLogicValue[i+1];
					cbLogicValue[i+1]=cbTempData;
					bSorted=false;
				}	
			}
			bLast--;
		} while(bSorted==false);

		return;
	}
	
	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[card_count];

		for (int i = 0; i < card_count; i++) {
			cbTempCardData[i] = cards[i];
		}

		// 置零扑克
		for (int i = 0; i < remove_count; i++) {
			for (int j = 0; j < card_count; j++) {
				if (remove_cards[i] == cbTempCardData[j]) {
					cbDeleteCount++;
					cbTempCardData[j] = 0;
					break;
				}
			}
		}

		// 成功判断
		if (cbDeleteCount != remove_count) {
			return false;
		}

		// 清理扑克
		int cbCardPos = 0;
		for (int i = 0; i < card_count; i++) {
			if (cbTempCardData[i] != 0)
				cards[cbCardPos++] = cbTempCardData[i];
		}

		return true;
	}

}
