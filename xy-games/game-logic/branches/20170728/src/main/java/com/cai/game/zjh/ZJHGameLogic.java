/**
 * 
 */
package com.cai.game.zjh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.RandomUtil;



public class ZJHGameLogic {


	public ZJHGameLogic() {

	}
	//获取类型
	public int GetCardType(int cbCardData[], int cbCardCount)
	{

		if (cbCardCount==GameConstants.ZJH_MAX_COUNT)
		{
			//变量定义
			boolean cbSameColor=true,bLineCard=true;
			int cbFirstColor=get_card_color(cbCardData[0]);
			int cbFirstValue=GetCardLogicValue(cbCardData[0]);

			//牌形分析
			for (int i=1;i<cbCardCount;i++)
			{
				//数据分析
				if (get_card_color(cbCardData[i])!=cbFirstColor) cbSameColor=false;
				if (cbFirstValue!=(GetCardLogicValue(cbCardData[i])+i)) bLineCard=false;

				//结束判断
				if ((cbSameColor==false)&&(bLineCard==false)) break;
			}

			//特殊A32
			if(!bLineCard)
			{
				boolean bOne=false;
				boolean bTwo=false;
				boolean	bThree=false;
				for(int i=0;i<GameConstants.ZJH_MAX_COUNT;i++)
				{
					if(get_card_value(cbCardData[i])==1)		bOne=true;
					else if(get_card_value(cbCardData[i])==2)	bTwo=true;
					else if(get_card_value(cbCardData[i])==3)	bThree=true;
				}
				if(bOne && bTwo && bThree)bLineCard=true;
			}

			//顺金类型
			if ((cbSameColor)&&(bLineCard)) return GameConstants.ZJH_CT_SHUN_JIN;

			//顺子类型
			if ((!cbSameColor)&&(bLineCard)) return GameConstants.ZJH_CT_SHUN_ZI;

			//金花类型
			if((cbSameColor)&&(!bLineCard)) return GameConstants.ZJH_CT_JIN_HUA;

			//牌形分析
			boolean bDouble=false,bPanther=true;

			//对牌分析
			for (int i=0;i<cbCardCount-1;i++)
			{
				for (int j=i+1;j<cbCardCount;j++)
				{
					if (GetCardLogicValue(cbCardData[i])==GetCardLogicValue(cbCardData[j])) 
					{
						bDouble=true;
						break;
					}
				}
				if(bDouble)break;
			}

			//三条(豹子)分析
			for (int i=1;i<cbCardCount;i++)
			{
				if (bPanther && cbFirstValue!=GetCardLogicValue(cbCardData[i])) bPanther=false;
			}

			//对子和豹子判断
			if (bDouble==true) return (bPanther)?GameConstants.ZJH_CT_BAO_ZI:GameConstants.ZJH_CT_DOUBLE;

			//特殊235
			boolean bTwo=false,bThree=false,bFive=false;
			for (int i=0;i<cbCardCount;i++)
			{
				if(get_card_value(cbCardData[i])==2)	bTwo=true;
				else if(get_card_value(cbCardData[i])==3)bThree=true;
				else if(get_card_value(cbCardData[i])==5)bFive=true;			
			}	
			if (bTwo && bThree && bFive) return GameConstants.ZJH_CT_SPECIAL;
		}
		return GameConstants.ZJH_CT_SINGLE;
	}

	//对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,boolean bCompare)
	{
		//设置变量
		int FirstData[]=new int[GameConstants.ZJH_MAX_COUNT];
		int NextData[]=new int[GameConstants.ZJH_MAX_COUNT];
		for(int i=0;i<GameConstants.ZJH_MAX_COUNT;i++){
			FirstData[i]=cbFirstCard[i];
			NextData[i]=cbNextCard[i];
		}

		//大小排序
		SortCardList(FirstData,cbFirstCount);
		SortCardList(NextData,cbNextCount);

		//获取类型
		int cbNextType=GetCardType(NextData,cbNextCount);
		int cbFirstType=GetCardType(FirstData,cbFirstCount);

		//特殊情况分析
		if((cbNextType+cbFirstType)==(GameConstants.ZJH_CT_SPECIAL+GameConstants.ZJH_CT_BAO_ZI) && bCompare)
			return cbFirstType>cbNextType;

		//还原单牌类型
		if(cbNextType==GameConstants.ZJH_CT_SPECIAL)cbNextType=GameConstants.ZJH_CT_SINGLE;
		if(cbFirstType==GameConstants.ZJH_CT_SPECIAL)cbFirstType=GameConstants.ZJH_CT_SINGLE;

		//类型判断
		if (cbFirstType!=cbNextType) return cbFirstType>cbNextType;

		//简单类型
		switch(cbFirstType)
		{
		case GameConstants.ZJH_CT_BAO_ZI:			//豹子
		case GameConstants.ZJH_CT_SINGLE:			//单牌
		case GameConstants.ZJH_CT_JIN_HUA:		//金花
			{
				//对比数值
				for (int i=0;i<cbFirstCount;i++)
				{
					int cbNextValue=GetCardLogicValue(NextData[i]);
					int cbFirstValue=GetCardLogicValue(FirstData[i]);
					if (cbFirstValue!=cbNextValue) return cbFirstValue>cbNextValue;
				}
			}
		case GameConstants.ZJH_CT_SHUN_ZI:		//顺子
		case GameConstants.ZJH_CT_SHUN_JIN:		//顺金 432>A32
			{		
				int cbNextValue=GetCardLogicValue(NextData[0]);
				int cbFirstValue=GetCardLogicValue(FirstData[0]);

				//特殊A32
				if(cbNextValue==14 && GetCardLogicValue(NextData[cbNextCount-1])==2)
				{
					cbNextValue=3;
				}
				if(cbFirstValue==14 && GetCardLogicValue(FirstData[cbFirstCount-1])==2)
				{
					cbFirstValue=3;
				}

				//对比数值
				if (cbFirstValue!=cbNextValue) return cbFirstValue>cbNextValue;
			}
		case GameConstants.ZJH_CT_DOUBLE:			//对子
			{
				int cbNextValue=GetCardLogicValue(NextData[0]);
				int cbFirstValue=GetCardLogicValue(FirstData[0]);

				//查找对子/单牌
				int bNextDouble=0,bNextSingle=0;
				int bFirstDouble=0,bFirstSingle=0;
				if(cbNextValue==GetCardLogicValue(NextData[1]))
				{
					bNextDouble=cbNextValue;
					bNextSingle=GetCardLogicValue(NextData[cbNextCount-1]);
				}
				else
				{
					bNextDouble=GetCardLogicValue(NextData[cbNextCount-1]);
					bNextSingle=cbNextValue;
				}
				if(cbFirstValue==GetCardLogicValue(FirstData[1]))
				{
					bFirstDouble=cbFirstValue;
					bFirstSingle=GetCardLogicValue(FirstData[cbFirstCount-1]);
				}
				else 
				{
					bFirstDouble=GetCardLogicValue(FirstData[cbFirstCount-1]);
					bFirstSingle=cbFirstValue;
				}

				if (bNextDouble!=bFirstDouble) return bFirstDouble>bNextDouble;
				if (bNextSingle!=bFirstSingle)return bFirstSingle>bNextSingle;
			}
		}
		return false;
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
		int cbLogicValue[]=new int[GameConstants.ZJH_MAX_COUNT];
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
	public int GetCardLogicValue(int CardData){
		//扑克属性
		int bCardColor=get_card_color(CardData);
		int bCardValue=get_card_value(CardData);

		//转换数值
		return (bCardValue==1)?(bCardValue+13):bCardValue;
	}
	// 获取数值
	public int get_card_value(int card) {
		return card & GameConstants.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & GameConstants.LOGIC_MASK_COLOR) >> 4;
	}
	
	// 删除扑克
	public boolean remove_cards_by_data(int cards[], int card_count, int remove_cards[], int remove_count) {
		// 检验数据
		if (card_count < remove_count)
			return false;

		// 定义变量
		int cbDeleteCount = 0;
		int cbTempCardData[] = new int[GameConstants.MAX_HH_COUNT];

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
