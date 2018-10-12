/**
 * 
 */
package com.cai.game.pdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
import com.cai.common.util.FvMask;
import com.cai.common.util.RandomUtil;

//经典斗地主
//分析结构
class tagAnalyseResult
{
	int							cbEightCount;						//八张数目
	int							cbSevenCount;						//七张数目
	int							cbSixCount;							//六张数目
	int							cbFiveCount;						//五张数目
	int 							cbFourCount;						//四张数目
	int 							cbThreeCount;						//三张数目
	int 							cbDoubleCount;						//两张数目
	int							cbSignedCount;						//单张数目
	int							cbEightCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//八张扑克
	int							cbSevenCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//七张扑克
	int							cbSixCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//六张扑克
	int							cbFiveCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//五张扑克
	int							cbFourCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//四张扑克
	int							cbThreeCardData[]=new int[GameConstants.PDK_MAX_COUNT];			//三张扑克
	int							cbDoubleCardData[]=new int[GameConstants.PDK_MAX_COUNT];		//两张扑克
	int							cbSignedCardData[]=new int[GameConstants.PDK_MAX_COUNT];		//单张扑克
	public tagAnalyseResult(){
		cbEightCount = 0;
		cbSevenCount = 0;
		cbSixCount = 0;
		cbFiveCount = 0;
		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		cbSignedCount = 0;
		Arrays.fill(cbEightCardData,0);
		Arrays.fill(cbSevenCardData,0);
		Arrays.fill(cbSixCardData,0);
		Arrays.fill(cbFiveCardData,0);
		Arrays.fill(cbFourCardData,0);
		Arrays.fill(cbThreeCardData,0);
		Arrays.fill(cbDoubleCardData,0);
		Arrays.fill(cbSignedCardData,0);
	}
	public void Reset(){
		cbEightCount = 0;
		cbSevenCount = 0;
		cbSixCount = 0;
		cbFiveCount = 0;
		cbFourCount = 0;
		cbThreeCount = 0;
		cbDoubleCount = 0;
		cbSignedCount = 0;
		Arrays.fill(cbEightCardData,0);
		Arrays.fill(cbSevenCardData,0);
		Arrays.fill(cbSixCardData,0);
		Arrays.fill(cbFiveCardData,0);
		Arrays.fill(cbFourCardData,0);
		Arrays.fill(cbThreeCardData,0);
		Arrays.fill(cbDoubleCardData,0);
		Arrays.fill(cbSignedCardData,0);
	}
};
//出牌结果
class tagOutCardResult
{
	int							cbCardCount;						//扑克数目
	int							cbResultCard[] = new int[GameConstants.PDK_MAX_COUNT];			//结果扑克
	
	public tagOutCardResult(){
		cbCardCount = 0;
		Arrays.fill(cbResultCard,0);
	}
	public void Reset(){
		cbCardCount = 0;
		Arrays.fill(cbResultCard,0);
	}
};


class tagOutCardTypeResult 
{
	int							cbCardType[]=new int[GameConstants.MAX_TYPE_COUNT];							//扑克类型
	int							cbCardTypeCount;
	int							cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];//每手个数
	int							cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][GameConstants.PDK_MAX_COUNT];//扑克数据
	public tagOutCardTypeResult(){
		cbCardTypeCount=0;
		Arrays.fill(cbCardType,0);
		Arrays.fill(cbEachHandCardCount,0);
		for(int i=0;i<GameConstants.MAX_TYPE_COUNT;i++){
			for(int j=0;j<GameConstants.PDK_MAX_COUNT;j++){
				cbCardData[i][j]=0;
			}
		}
	}
	
	public void Reset(){
		cbCardTypeCount=0;
		Arrays.fill(cbCardType,0);
		Arrays.fill(cbEachHandCardCount,0);
		for(int i=0;i<GameConstants.MAX_TYPE_COUNT;i++){
			for(int j=0;j<GameConstants.PDK_MAX_COUNT;j++){
				cbCardData[i][j]=0;
			}
		}
	}
};



public class PDKGameLogic {

	private int cbIndexCount=5;
	public int _game_rule_index; // 游戏规则
	public int _laizi = GameConstants.INVALID_CARD;//癞子牌数据

	public PDKGameLogic() {

	}
	
	//获取类型
	public int GetCardType(int cbCardData[], int cbCardCount,int cbRealData[])
	{
		//this.sort_card_date_list(cbCardData, cbCardCount);
		int nlaizicount=this.GetLaiZiCount(cbRealData, cbCardCount);
		//简单牌型
		switch (cbCardCount)
		{
		case 0:	//空牌
			{
				return GameConstants.PDK_CT_ERROR;
			}
		case 1: //单牌
			{
				return GameConstants.PDK_CT_SINGLE;
			}
		case 2:	//对牌
			{
				//牌型判断
				if (GetCardLogicValue(cbCardData[0])==GetCardLogicValue(cbCardData[1])) return GameConstants.PDK_CT_DOUBLE;

				return GameConstants.PDK_CT_ERROR;
			}
		case 4:	//天王炸
			{
				boolean bMissileCard = true;
				for ( int cbCardIdx = 0; cbCardIdx < cbCardCount; ++cbCardIdx )
				{
					if ( this.get_card_color( cbCardData[ cbCardIdx ] ) != 0x40 ) 
					{
						bMissileCard = false;
						break;
					}
				}
				if ( bMissileCard ) return GameConstants.PDK_CT_MISSILE_CARD;
			}
		}

		//分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData,cbCardCount,AnalyseResult);

		//炸弹判断
		if ( 4 <= cbCardCount && cbCardCount <= 8 )
		{
			//牌型判断
			if ((AnalyseResult.cbFourCount==1)&&(cbCardCount==4)) {
				if(nlaizicount == 0){
					return GameConstants.PDK_CT_BOMB_CARD;
				}else if(nlaizicount == 4){
					return GameConstants.PDK_CT_MAGIC_BOOM;
				}else{
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbFiveCount==1)&&(cbCardCount==5)) {
				if(nlaizicount == 0){
					return GameConstants.PDK_CT_BOMB_CARD;
				}else{
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbSixCount==1)&&(cbCardCount==6)) {
				if(nlaizicount == 0){
					return GameConstants.PDK_CT_BOMB_CARD;
				}else{
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbSevenCount==1)&&(cbCardCount==7)) {
				if(nlaizicount == 0){
					return GameConstants.PDK_CT_BOMB_CARD;
				}else{
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
			if ((AnalyseResult.cbEightCount==1)&&(cbCardCount==8)) {
				if(nlaizicount == 0){
					return GameConstants.PDK_CT_BOMB_CARD;
				}else{
					return GameConstants.PDK_CT_RUAN_BOMB;
				}
			}
		}
		if(AnalyseResult.cbFourCount == 1){
			if(cbCardCount == 5){
				return GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE;
			}
			else if(cbCardCount == 6){
				return GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO;
			}
			else if(cbCardCount == 7){
				return GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE;
			}
		}

		//三牌判断
		if (AnalyseResult.cbThreeCount>0)
		{
			//三条类型
			if(AnalyseResult.cbThreeCount==1 && cbCardCount==3) return GameConstants.PDK_CT_THREE ;
			if(AnalyseResult.cbThreeCount*5==cbCardCount && cbCardCount==5) return GameConstants.PDK_CT_THREE_TAKE_TWO ;
			if ((AnalyseResult.cbThreeCount*4==cbCardCount)&&(cbCardCount == 4)) return GameConstants.PDK_CT_THREE_TAKE_ONE;

			//连牌判断
			if (AnalyseResult.cbThreeCount>1)
			{
				//变量定义
				int CardData=AnalyseResult.cbThreeCardData[0];
				int cbFirstLogicValue=GetCardLogicValue(CardData);

				//错误过虑
				if (cbFirstLogicValue>=15) return GameConstants.PDK_CT_ERROR;

				//连牌判断
				for (int i=1;i<AnalyseResult.cbThreeCount;i++)
				{
					int CardDatatemp=AnalyseResult.cbThreeCardData[i*3];
					if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)+i)) return GameConstants.PDK_CT_ERROR;
				}
			}
			for(int i=AnalyseResult.cbThreeCount;i>0;i--){
				if (i*5==cbCardCount) return GameConstants.PDK_CT_PLANE;
			}
			//牌形判断
			
			if(AnalyseResult.cbThreeCount*5>cbCardCount && AnalyseResult.cbThreeCount*3<=cbCardCount){
				return GameConstants.PDK_CT_PLANE_LOST;
			}

			return GameConstants.PDK_CT_ERROR;
		}

		//两张类型
		if (AnalyseResult.cbDoubleCount>=2)
		{
			//变量定义
			int CardData=AnalyseResult.cbDoubleCardData[0];
			int cbFirstLogicValue=GetCardLogicValue(CardData);

			//错误过虑
			if (cbFirstLogicValue>=15) return GameConstants.PDK_CT_ERROR;

			//连牌判断
			for (int i=1;i<AnalyseResult.cbDoubleCount;i++)
			{
				int CardDatatemp=AnalyseResult.cbDoubleCardData[i*2];
				if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)+i)) return GameConstants.PDK_CT_ERROR;
			}

			//二连判断
			if ((AnalyseResult.cbDoubleCount*2)==cbCardCount) return GameConstants.PDK_CT_DOUBLE_LINE;

			return GameConstants.PDK_CT_ERROR;
		}

		//单张判断
		if ((AnalyseResult.cbSignedCount>=5)&&(AnalyseResult.cbSignedCount==cbCardCount))
		{
			//变量定义
			int CardData=AnalyseResult.cbSignedCardData[0];
			int cbFirstLogicValue=GetCardLogicValue(CardData);

			//错误过虑
			if (cbFirstLogicValue>=15) return GameConstants.PDK_CT_ERROR;

			//连牌判断
			for (int i=1;i<AnalyseResult.cbSignedCount;i++)
			{
				int CardDatatemp=AnalyseResult.cbSignedCardData[i];
				if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)+i)) return GameConstants.PDK_CT_ERROR;
			}
			
			CardData=AnalyseResult.cbSignedCardData[0];
			int cbFirstColor=get_card_color(CardData);
			for(int i=1;i<AnalyseResult.cbSignedCount;i++){
				int CardDatatemp=AnalyseResult.cbSignedCardData[i];
				int cbNextColor=get_card_color(CardDatatemp);
				if(cbFirstColor != cbNextColor) return GameConstants.PDK_CT_SINGLE_LINE;
			}
			if(has_rule(GameConstants.GAME_RULE_LIANGFU_COUNT)){
				return GameConstants.PDK_CT_HONG_HUA_SHUN;
			}
			else{
				return GameConstants.PDK_CT_SINGLE_LINE;
			}
		}

		return GameConstants.PDK_CT_ERROR;
	}

	
	//分析扑克
	public void AnalysebCardData(int cbCardData[], int cbCardCount, tagAnalyseResult AnalyseResult)
	{
		//设置结果
		AnalyseResult.Reset();

		//扑克分析
		for (int i=0;i<cbCardCount;i++)
		{
			//变量定义
			int cbSameCount=1,cbCardValueTemp=0;
			int cbLogicValue=GetCardLogicValue(cbCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			//设置结果
			switch (cbSameCount)
			{
			case 1:		//单张
				{
					int cbIndex=AnalyseResult.cbSignedCount++;
					AnalyseResult.cbSignedCardData[cbIndex*cbSameCount]=cbCardData[i];
					break;
				}
			case 2:		//两张
				{
					int cbIndex=AnalyseResult.cbDoubleCount++;
					AnalyseResult.cbDoubleCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbDoubleCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					break;
				}
			case 3:		//三张
				{
					int cbIndex=AnalyseResult.cbThreeCount++;
					AnalyseResult.cbThreeCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbThreeCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbThreeCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					break;
				}
			case 4:		//四张
				{
					int cbIndex=AnalyseResult.cbFourCount++;
					if(cbCardCount != 5){
						AnalyseResult.cbFourCardData[cbIndex*cbSameCount]=cbCardData[i];
						AnalyseResult.cbFourCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
						AnalyseResult.cbFourCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
						AnalyseResult.cbFourCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
					}
					else{
						AnalyseResult.cbThreeCardData[cbIndex*cbSameCount]=cbCardData[i];
						AnalyseResult.cbThreeCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
						AnalyseResult.cbThreeCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					}
					break;
				}
			case 5:		//五张
				{
					int cbIndex=AnalyseResult.cbFiveCount++;
					AnalyseResult.cbFiveCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbFiveCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbFiveCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					AnalyseResult.cbFiveCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
					AnalyseResult.cbFiveCardData[cbIndex*cbSameCount+4]=cbCardData[i+4];
					break;
				}
			case 6:		//六张
				{
					int cbIndex=AnalyseResult.cbSixCount++;
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount+4]=cbCardData[i+4];
					AnalyseResult.cbSixCardData[cbIndex*cbSameCount+5]=cbCardData[i+5];
					break;
				}
			case 7:		//七张
				{
					int cbIndex=AnalyseResult.cbSevenCount++;
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+4]=cbCardData[i+4];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+5]=cbCardData[i+5];
					AnalyseResult.cbSevenCardData[cbIndex*cbSameCount+6]=cbCardData[i+6];
					break;
				}
			case 8:		//八张
				{
					int cbIndex=AnalyseResult.cbEightCount++;
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+4]=cbCardData[i+4];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+5]=cbCardData[i+5];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+6]=cbCardData[i+6];
					AnalyseResult.cbEightCardData[cbIndex*cbSameCount+7]=cbCardData[i+7];
					break;
				}
			}

			//设置索引
			i+=cbSameCount-1;
		}
		return;
	}

	//对比扑克
	public boolean CompareCard(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount)
	{
		//获取类型
		int cbNextType=GetCardType(cbNextCard,cbNextCount,cbNextCard);
		int cbFirstType=GetCardType(cbFirstCard,cbFirstCount,cbFirstCard);

		//类型判断
		if (cbNextType==GameConstants.PDK_CT_ERROR) return false;
		if (cbNextType==GameConstants.PDK_CT_MISSILE_CARD) return true;
		if ( cbFirstType == GameConstants.PDK_CT_MISSILE_CARD ) return false;

		//炸弹判断
		if ((cbFirstType!=GameConstants.PDK_CT_BOMB_CARD)&&(cbNextType==GameConstants.PDK_CT_BOMB_CARD)) return true;
		if ((cbFirstType==GameConstants.PDK_CT_BOMB_CARD)&&(cbNextType!=GameConstants.PDK_CT_BOMB_CARD)) return false;

		//规则判断
		if((cbFirstType!=cbNextType) && 
			cbNextType == GameConstants.PDK_CT_HONG_HUA_SHUN && cbFirstType==GameConstants.PDK_CT_SINGLE_LINE)return true;
		if ((cbFirstType!=cbNextType)||(cbFirstType!=GameConstants.PDK_CT_BOMB_CARD && cbFirstCount!=cbNextCount)) return false;

		//开始对比
		switch (cbNextType)
		{
		
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE:
			{
				//获取数值
				int cbNextLogicValue=GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue=GetCardLogicValue(cbFirstCard[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:	
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE:
			{
				//分析扑克
				tagAnalyseResult NextResult = new tagAnalyseResult();
				tagAnalyseResult FirstResult = new tagAnalyseResult();
				AnalysebCardData(cbNextCard,cbNextCount,NextResult);
				AnalysebCardData(cbFirstCard,cbFirstCount,FirstResult);

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(NextResult.cbThreeCardData[0]);
				int cbFirstLogicValue=GetCardLogicValue(FirstResult.cbThreeCardData[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE:
			{
				//分析扑克
				tagAnalyseResult NextResult = new tagAnalyseResult();
				tagAnalyseResult FirstResult = new tagAnalyseResult();
				AnalysebCardData(cbNextCard,cbNextCount,NextResult);
				AnalysebCardData(cbFirstCard,cbFirstCount,FirstResult);

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(NextResult.cbFourCardData[0]);
				int cbFirstLogicValue=GetCardLogicValue(FirstResult.cbFourCardData[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_BOMB_CARD:
			{
				//数目判断
				if ( cbNextCount != cbFirstCount ) return cbNextCount > cbFirstCount;

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue=GetCardLogicValue(cbFirstCard[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		}

		return false;
	}
	//判断是否有压牌
	//出牌搜索
	public boolean SearchOutCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		
		//获取出牌类型
		int card_type=GetCardType(cbTurnCardData,cbTurnCardCount,cbTurnCardData);
		if (card_type==GameConstants.PDK_CT_MISSILE_CARD) return false;
		
		//搜索炸弹
		if(SearchBoomCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
			return true;
		}
		//搜索顺子
		if(card_type == GameConstants.PDK_CT_SINGLE_LINE){
			return SearchSingleLineCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_DOUBLE_LINE){
			return SearchDoubleLineCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_PLANE){
			return SearchThreeLineCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_SINGLE){
			return SearchSingleCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_DOUBLE){
			return SearchDoubleCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_THREE || card_type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| card_type == GameConstants.PDK_CT_THREE_TAKE_TWO || card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE){
			return SearchThreeCard(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		
		//int cbFirstType=GetCardType(cbFirstCard,cbFirstCount);
		return false;
	}
	//搜索三张
	public boolean SearchThreeCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		if(cbHandCardCount < cbTurnCardCount){
			return false;
		}
		
		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCard[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCard[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(cbSameCount>=3 && GetCardLogicValue(cbTmpCard[i])>GetCardLogicValue(cbTurnCardData[0]))
			{
				return true;
			}
			//设置索引
			i+=cbSameCount-1;
		}
		return false;
	}
	//搜索对子
	public boolean SearchDoubleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCard[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCard[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(cbSameCount>=2 && GetCardLogicValue(cbTmpCard[i])>GetCardLogicValue(cbTurnCardData[0]))
			{
				return true;
			}
			//设置索引
			i+=cbSameCount-1;
		}
		return false;
	}
	//搜索单张
	public boolean SearchSingleCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		for(int i=0; i<cbHandCardCount; ++i){
			if(GetCardLogicValue(cbTmpCard[i])>GetCardLogicValue(cbTurnCardData[0])) 
			{
				return true;
			}
		}
			
		return false;
	}
	//搜索飞机
	public boolean SearchThreeLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		if(cbHandCardCount < cbTurnCardCount)return false;
		//连牌判断
		int cbFirstCard = 0 ;
		//去除2和王
		for(int i=0 ; i<cbHandCardCount ; ++i)	if(GetCardLogicValue(cbTmpCard[i])<15)	{cbFirstCard = i ; break ;}

		int cbLeftCardCount = cbHandCardCount-cbFirstCard ;
		boolean bFindThreeLine = true ;
		int cbThreeLineCount = 0 ;
		int cbThreeLineCard[]=new int[20] ;
		//开始判断
		while (cbLeftCardCount>=cbTurnCardCount && bFindThreeLine)
		{
			int cbLastCard = cbTmpCard[cbFirstCard] ;
			int cbSameCount = 1 ;
			cbThreeLineCount = 0 ;
			bFindThreeLine = false ;
			for(int i=cbFirstCard+1 ; i<cbLeftCardCount+cbFirstCard ; ++i)
			{
				//搜索同牌
				while (get_card_value(cbLastCard)==get_card_value(cbTmpCard[i]) && i<cbLeftCardCount+cbFirstCard)
				{
					++cbSameCount;
					++i ;
				}

				int cbLastThreeCardValue=0 ;
				if(cbThreeLineCount>0) cbLastThreeCardValue = GetCardLogicValue(cbThreeLineCard[cbThreeLineCount-1]) ;

				//重新开始
				if((cbSameCount<3 || (cbThreeLineCount>0&&(cbLastThreeCardValue-GetCardLogicValue(cbLastCard))!=1)) && i<=cbLeftCardCount+cbFirstCard)
				{
					if(cbThreeLineCount>=cbTurnCardCount) break ;

					if(cbSameCount>=3) i-= 3 ;
					cbLastCard = cbTmpCard[i] ;
					cbThreeLineCount = 0 ;
				}
				//保存数据
				else if(cbSameCount>=3)
				{
					cbThreeLineCard[cbThreeLineCount] = cbTmpCard[i-cbSameCount] ;
					cbThreeLineCard[cbThreeLineCount+1] = cbTmpCard[i-cbSameCount+1] ;
					cbThreeLineCard[cbThreeLineCount+2] = cbTmpCard[i-cbSameCount+2] ;
					cbThreeLineCount += 3 ;

					//结尾判断
					if(i==(cbLeftCardCount+cbFirstCard-3))
						if((GetCardLogicValue(cbLastCard)-GetCardLogicValue(cbTmpCard[i]))==1 && (GetCardLogicValue(cbTmpCard[i])==GetCardLogicValue(cbTmpCard[i+1])) && (GetCardLogicValue(cbTmpCard[i])==GetCardLogicValue(cbTmpCard[i+2])))
						{
							cbThreeLineCard[cbThreeLineCount] = cbTmpCard[i] ;
							cbThreeLineCard[cbThreeLineCount+1] = cbTmpCard[i+1] ;
							cbThreeLineCard[cbThreeLineCount+2] = cbTmpCard[i+2] ;
							cbThreeLineCount += 3 ;
							break ;
						}

				}

				cbLastCard = cbTmpCard[i] ;
				cbSameCount = 1 ;
			}

			//保存数据
			if(cbThreeLineCount>=cbTurnCardCount)
			{
				if(GetCardLogicValue(cbThreeLineCard[0])>GetCardLogicValue(cbTurnCardData[0])){
					return true;
				}
			}
		}
		return false;
	}
	//搜索连对
	public boolean SearchDoubleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		//连牌判断
		int cbFirstCard = 0 ;
		//去除2和王
		for(int i=0 ; i<cbHandCardCount ; ++i)	if(GetCardLogicValue(cbTmpCard[i])<15)	{cbFirstCard = i ; break ;}

		int cbLeftCardCount = cbHandCardCount-cbFirstCard ;
		boolean bFindDoubleLine = true ;
		int cbDoubleLineCount = 0 ;
		int cbDoubleLineCard[]=new int[24] ;
		//开始判断
		while (cbLeftCardCount>=cbTurnCardCount && bFindDoubleLine)
		{
			int cbLastCard = cbTmpCard[cbFirstCard] ;
			int cbSameCount = 1 ;
			cbDoubleLineCount = 0 ;
			bFindDoubleLine=false ;
			for(int i=cbFirstCard+1 ; i<cbLeftCardCount+cbFirstCard ; ++i)
			{
				//搜索同牌
				while (get_card_value(cbLastCard)==get_card_value(cbTmpCard[i]) && i<cbLeftCardCount+cbFirstCard)
				{
					++cbSameCount;
					if(i == cbLeftCardCount+cbFirstCard-1)
						break;
					++i ;
				}

				int cbLastDoubleCardValue=0 ;
				if(cbDoubleLineCount>0) cbLastDoubleCardValue = GetCardLogicValue(cbDoubleLineCard[cbDoubleLineCount-1]) ;
				//重新开始
				if((cbSameCount<2 || (cbDoubleLineCount>0 && (cbLastDoubleCardValue-GetCardLogicValue(cbLastCard))!=1)) && i<=cbLeftCardCount+cbFirstCard)
				{
					if(cbDoubleLineCount>=cbTurnCardCount) break ;

					if(cbSameCount>=2) i-=cbSameCount ;

					cbLastCard = cbTmpCard[i] ;
					cbDoubleLineCount = 0 ;
				}
				//保存数据
				else if(cbSameCount>=2)
				{
					cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i-cbSameCount] ;
					cbDoubleLineCard[cbDoubleLineCount+1] = cbTmpCard[i-cbSameCount+1] ;
					cbDoubleLineCount += 2 ;

					//结尾判断
					if(i==(cbLeftCardCount+cbFirstCard-2))
						if((GetCardLogicValue(cbLastCard)-GetCardLogicValue(cbTmpCard[i]))==1 && (GetCardLogicValue(cbTmpCard[i])==GetCardLogicValue(cbTmpCard[i+1])))
						{
							cbDoubleLineCard[cbDoubleLineCount] = cbTmpCard[i] ;
							cbDoubleLineCard[cbDoubleLineCount+1] = cbTmpCard[i+1] ;
							cbDoubleLineCount += 2 ;
							break ;
						}

				}

				cbLastCard = cbTmpCard[i] ;
				cbSameCount = 1 ;
			}

			//保存数据
			if(cbDoubleLineCount>=cbTurnCardCount)
			{
				if(GetCardLogicValue(cbDoubleLineCard[0]) > GetCardLogicValue(cbTurnCardData[0])){
					return true;
				}
			}
		}
		return false;
	}
	//分析顺子
	public boolean SearchSingleLineCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount)
	{
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}

		int cbLineCardCount = 0 ;

		//数据校验
		if(cbHandCardCount<cbTurnCardCount) return false;

		int cbFirstCard = 0 ;
		//去除2和王
		for(int i=0 ; i<cbHandCardCount ; ++i)	if(GetCardLogicValue(cbTmpCard[i])<15)	{cbFirstCard = i ; break ;}

		int cbSingleLineCard[]=new int[12] ;
		int cbSingleLineCount=0 ;
		int cbLeftCardCount = cbHandCardCount ;
		boolean bFindSingleLine = true ;

		//连牌判断
		while (cbLeftCardCount>=cbTurnCardCount && bFindSingleLine)
		{
			cbSingleLineCount=1 ;
			bFindSingleLine = false ;
			int cbLastCard = cbTmpCard[cbFirstCard] ;
			cbSingleLineCard[cbSingleLineCount-1] = cbTmpCard[cbFirstCard] ;
			for (int i=cbFirstCard+1; i<cbLeftCardCount; i++)
			{
				int cbCardData=cbTmpCard[i];

				//连续判断
				if (1!=(GetCardLogicValue(cbLastCard)-GetCardLogicValue(cbCardData)) && get_card_value(cbLastCard)!=get_card_value(cbCardData)) 
				{
					cbLastCard = cbTmpCard[i] ;
					if(cbSingleLineCount<cbTurnCardCount) 
					{
						cbSingleLineCount = 1 ;
						cbSingleLineCard[cbSingleLineCount-1] = cbTmpCard[i] ;
						continue ;
					}
					else break ;
				}
				//同牌判断
				else if(get_card_value(cbLastCard)!=get_card_value(cbCardData))
				{
					cbLastCard = cbCardData ;
					cbSingleLineCard[cbSingleLineCount] = cbCardData ;
					++cbSingleLineCount ;
				}					
			}

			//保存数据
			if(cbSingleLineCount>=cbTurnCardCount)
			{
				if(GetCardLogicValue(cbTurnCardData[0])<GetCardLogicValue(cbSingleLineCard[0])){
					return true;
				}
			}
		}
		return false;
	}
	//分析炸弹
	public boolean SearchBoomCard(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		
		int cbTmpCardData[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCardData[i]=cbHandCardData[i];
		}


		int cbBomCardCount = 0;

		if(cbHandCardCount<2) return false;

		//双王炸弹
		if(0x4F==cbTmpCardData[0] && 0x4F==cbTmpCardData[1] && 0x4E==cbTmpCardData[2] && 0x4E==cbTmpCardData[3])
		{
			return true;
		}
		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}
			if(cbSameCount >= 4 && cbSameCount>cbTurnCardCount){
				return true;
			}else if(cbSameCount >= 4 && cbSameCount == cbTurnCardCount){
				int cbBomCardData[]=new int[cbSameCount];
				for(int j=0;j<cbSameCount;j++){
					cbBomCardData[j]=cbTmpCardData[i+j];
				}
				if(CompareCard(cbTurnCardData,cbBomCardData,cbTurnCardCount,cbSameCount)){
					return true;
				}
			}else{
				return false;
			}

			//设置索引
			i+=cbSameCount-1;
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
	public void sort_card_date_list(int card_date[],int card_count)
	{
		//转换数值
		int logic_value[] = new int [card_count];
		for (int i=0;i<card_count;i++) {
			logic_value[i]=GetCardLogicValue(card_date[i]);
		}

		//排序操作
		boolean sorted=true;
		int temp_date,last=card_count-1;
		int nLaiZicount=this.GetLaiZiCount(card_date, card_count);
		int index=0;
		for (int i=0;i<last;i++){
			if(logic_value[i] == GetCardLogicValue(this._laizi) && logic_value[index] != GetCardLogicValue(this._laizi)){
				temp_date=card_date[i];
				card_date[i]=card_date[index];
				card_date[index]=temp_date;
				temp_date=logic_value[i];
				logic_value[i]=logic_value[index];
				logic_value[index]=temp_date;
				index++;
			}
		}
		do
		{
			sorted=true;
			for (int i=nLaiZicount;i<last;i++)
			{
				if ((logic_value[i]<logic_value[i+1])||
					((logic_value[i]==logic_value[i+1])&&(card_date[i]>card_date[i+1])))
				{
					//交换位置
					temp_date=card_date[i];
					card_date[i]=card_date[i+1];
					card_date[i+1]=temp_date;
					temp_date=logic_value[i];
					logic_value[i]=logic_value[i+1];
					logic_value[i+1]=temp_date;
					sorted=false;
				}	
			}
			last--;
		} while(sorted==false);

		return;
	}
	public void sort_card_date_list_by_type(int card_date[],int card_count,int type)
	{
		tagAnalyseResult Result = new tagAnalyseResult();
		AnalysebCardData(card_date,card_count,Result);

		int index=0;
		if(type == GameConstants.PDK_CT_SINGLE || type == GameConstants.PDK_CT_SINGLE_LINE
				|| type == GameConstants.PDK_CT_HONG_HUA_SHUN){
			for(int i=0;i<Result.cbSignedCount;i++){
				card_date[index++]=Result.cbSignedCardData[i];
			}
		}else if(type == GameConstants.PDK_CT_DOUBLE || type == GameConstants.PDK_CT_DOUBLE_LINE){
			for(int i=0;i<Result.cbDoubleCount;i++){
				for(int j=0;j<2;j++){
					card_date[index++]=Result.cbDoubleCardData[i*2+j];
				}
			}
		}else if(type == GameConstants.PDK_CT_THREE || type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| type == GameConstants.PDK_CT_THREE_TAKE_TWO || type == GameConstants.PDK_CT_PLANE
				|| type == GameConstants.PDK_CT_PLANE_LOST){
			for(int i=0;i<Result.cbThreeCount;i++){
				for(int j=0;j<3;j++){
					card_date[index++]=Result.cbThreeCardData[i*3+j];
				}
			}
			for(int i=0;i<Result.cbSignedCount;i++){
				card_date[index++]=Result.cbSignedCardData[i];
			}
			for(int i=0;i<Result.cbDoubleCount;i++){
				for(int j=0;j<2;j++){
					card_date[index++]=Result.cbDoubleCardData[i*2+j];
				}
			}
		}else if(type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE || type == GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO
				|| type == GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE){
			for(int i=0;i<Result.cbFourCount;i++){
				for(int j=0;j<4;j++){
					card_date[index++]=Result.cbFourCardData[i*4+j];
				}
			}
			for(int i=0;i<Result.cbThreeCount;i++){
				for(int j=0;j<3;j++){
					card_date[index++]=Result.cbThreeCardData[i*3+j];
				}
			}
			for(int i=0;i<Result.cbSignedCount;i++){
				card_date[index++]=Result.cbSignedCardData[i];
			}
			for(int i=0;i<Result.cbDoubleCount;i++){
				for(int j=0;j<2;j++){
					card_date[index++]=Result.cbDoubleCardData[i*2+j];
				}
			}
		}else if(type == GameConstants.PDK_CT_BOMB_CARD){
			for(int i=0;i<Result.cbEightCount;i++){
				for(int j=0;j<8;j++){
					card_date[index++]=Result.cbEightCardData[i*8+j];
				}
			}
			for(int i=0;i<Result.cbSevenCount;i++){
				for(int j=0;j<7;j++){
					card_date[index++]=Result.cbSevenCardData[i*7+j];
				}
			}
			for(int i=0;i<Result.cbSixCount;i++){
				for(int j=0;j<6;j++){
					card_date[index++]=Result.cbSixCardData[i*6+j];
				}
			}
			for(int i=0;i<Result.cbFiveCount;i++){
				for(int j=0;j<5;j++){
					card_date[index++]=Result.cbFiveCardData[i*5+j];
				}
			}
		}
		
		
		
		
		
		
		
		return;
	}
	public int GetCardLogicValue(int CardData){
		//扑克属性
		int cbCardColor=get_card_color(CardData);
		int cbCardValue=get_card_value(CardData);

		//转换数值
		if (cbCardColor==0x04) return cbCardValue+2;
		return (cbCardValue<=2)?(cbCardValue+13):cbCardValue;
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
	
	//放走包赔
	public boolean fang_zou_bao_pei(int cbCardData[], int cbCardCount,int cbOutCardData[]){
		//分析扑克
		
		if(GetAllBomCard(cbCardData,cbCardCount)>0){
			return true;
		}
		if(GetAllLineCard(cbCardData,cbCardCount)>0){
			return true;
		}
		if(GetAllThreeCard(cbCardData,cbCardCount)>0){
			return true;
		}
		if(GetAllDoubleCard(cbCardData,cbCardCount)>0){
			return true;
		}
		
		
		if(GetCardLogicValue(cbCardData[0])!=GetCardLogicValue(cbOutCardData[0])){
			return true;
		}
		
		return false;
	}
	//获取炸弹
	public int GetAllBomCard(int cbHandCardData[], int cbHandCardCount)
	{
		int cbTmpCardData[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCardData[i]=cbHandCardData[i];
		}


		int cbBomCardCount = 0 ;

		if(cbHandCardCount<2) return 0;

		//双王炸弹
		if(0x4F==cbTmpCardData[0] && 0x4E==cbTmpCardData[1])
		{
			cbBomCardCount+=2;
		}

		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(4==cbSameCount)
			{
				cbBomCardCount+=4;
			}

			//设置索引
			i+=cbSameCount-1;
		}
		return cbBomCardCount;
	}
	//获取顺子
	public int GetAllLineCard(int cbHandCardData[], int cbHandCardCount)
	{
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int cbLineCardCount = 0 ;

		//数据校验
		if(cbHandCardCount<5) return 0;

		int cbFirstCard = 0 ;
		//去除2和王
		for(int i=0 ; i<cbHandCardCount ; ++i)	if(GetCardLogicValue(cbTmpCard[i])<15)	{cbFirstCard = i ; break ;}

		int cbSingleLineCard[]=new int[12] ;
		int cbSingleLineCount=0 ;
		int cbLeftCardCount = cbHandCardCount ;
		boolean bFindSingleLine = true ;

		//连牌判断
		while (cbLeftCardCount>=5 && bFindSingleLine)
		{
			cbSingleLineCount=1 ;
			bFindSingleLine = false ;
			int cbLastCard = cbTmpCard[cbFirstCard] ;
			cbSingleLineCard[cbSingleLineCount-1] = cbTmpCard[cbFirstCard] ;
			for (int i=cbFirstCard+1; i<cbLeftCardCount; i++)
			{
				int cbCardData=cbTmpCard[i];

				//连续判断
				if (1!=(GetCardLogicValue(cbLastCard)-GetCardLogicValue(cbCardData)) && get_card_value(cbLastCard)!=get_card_value(cbCardData)) 
				{
					cbLastCard = cbTmpCard[i] ;
					if(cbSingleLineCount<5) 
					{
						cbSingleLineCount = 1 ;
						cbSingleLineCard[cbSingleLineCount-1] = cbTmpCard[i] ;
						continue ;
					}
					else break ;
				}
				//同牌判断
				else if(get_card_value(cbLastCard)!=get_card_value(cbCardData))
				{
					cbLastCard = cbCardData ;
					cbSingleLineCard[cbSingleLineCount] = cbCardData ;
					++cbSingleLineCount ;
				}					
			}

			//保存数据
			if(cbSingleLineCount>=5)
			{
				this.remove_cards_by_data(cbTmpCard, cbLeftCardCount, cbSingleLineCard, cbSingleLineCount);
				cbLineCardCount += cbSingleLineCount ;
				cbLeftCardCount -= cbSingleLineCount ;
				bFindSingleLine = true ;
			}
		}
		return cbLineCardCount;
	}
	
	//获取三条
	public int GetAllThreeCard(int cbHandCardData[], int cbHandCardCount)
	{
		int cbTmpCardData[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCardData[i]=cbHandCardData[i];
		}


		int cbThreeCardCount = 0 ;

		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(cbSameCount>=3)
			{
				cbThreeCardCount+=3;	
			}

			//设置索引
			i+=cbSameCount-1;
		}
		return cbThreeCardCount;
	}
	//分析对子
	public int GetAllDoubleCard(int cbHandCardData[], int cbHandCardCount)
	{
		int cbTmpCardData[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCardData[i]=cbHandCardData[i];
		}


		int cbDoubleCardCount = 0 ;

		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(cbSameCount>=2)
			{
				cbDoubleCardCount+=2;
			}

			//设置索引
			i+=cbSameCount-1;
		}
		return cbDoubleCardCount;
	}
	//赖子数目
	public int GetLaiZiCount(int cbHandCardData[], int cbHandCardCount)
	{
		if(_laizi == GameConstants.INVALID_CARD){
			return 0;
		}
		int bLaiZiCount=0;
		for(int i=0;i<cbHandCardCount;i++)
		{
			if(GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(_laizi))
				bLaiZiCount++;
		}

		return bLaiZiCount;
	}
	public void GetCardTypeLaizi(int cbCardData[], int cbCardCount,tagOutCardTypeResult out_card_type_result)
	{
		int bLaiZiCount=GetLaiZiCount(cbCardData,cbCardCount);
		
		int tempCardData[] = new int[cbCardCount];
		for(int i=0;i<cbCardCount;i++){
			tempCardData[i]=cbCardData[i];
		}
		for(int i=0;i<cbCardCount;i++){
			if(GetCardLogicValue(cbCardData[i]) == GetCardLogicValue(this._laizi)){
				for(int valueone=1;valueone<13;valueone++){
					//一个癞子变牌
					tempCardData[i]=(0x0F-valueone)%13+1;
					if(bLaiZiCount >= 2){
						for(int j=i+1;j<cbCardCount;j++){
							if(GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)){
								for(int valuetwo=1;valuetwo<14;valuetwo++){
									//两个癞子变牌
									tempCardData[j]=(0x0F-valuetwo)%13+1;
									if(bLaiZiCount >= 3){
										for(int x=j+1;x<cbCardCount;x++){
											if(GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)){
												for(int valuethree=1;valuethree<14;valuethree++){
													//三个癞子变牌
													tempCardData[x]=(0x0F-valuethree)%13+1;
													if(bLaiZiCount >= 4){
														for(int y=x+1;y<cbCardCount;y++){
															if(GetCardLogicValue(cbCardData[j]) == GetCardLogicValue(this._laizi)){
																for(int valuefour=1;valuefour<14;valuefour++){
																	//四个癞子变牌
																	tempCardData[y]=(0x0F-valuefour)%13+1;
																	int card_type=this.GetCardType(tempCardData, cbCardCount,cbCardData);
																	if(cbCardCount == 4 ){
																		//四癞子炸弹直接返回
																		int count=out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count]=GameConstants.PDK_CT_MAGIC_BOOM;
																		for(int card_index=0;card_index<cbCardCount;card_index++){
																			out_card_type_result.cbCardData[count][card_index]=cbCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count]=cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																		return;
																	}
																	if(cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD){
																		continue;
																	}
																	if(card_type != GameConstants.PDK_CT_ERROR){
																		int count=out_card_type_result.cbCardTypeCount;
																		out_card_type_result.cbCardType[count]=card_type;
																		for(int card_index=0;card_index<cbCardCount;card_index++){
																			out_card_type_result.cbCardData[count][card_index]=tempCardData[card_index];
																		}
																		out_card_type_result.cbEachHandCardCount[count]=cbCardCount;
																		out_card_type_result.cbCardTypeCount++;
																	}
																}
															}
														}
													}
													int card_type=this.GetCardType(tempCardData, cbCardCount,cbCardData);
													if(cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD){
														continue;
													}
													if(card_type != GameConstants.PDK_CT_ERROR){
														int count=out_card_type_result.cbCardTypeCount;
														out_card_type_result.cbCardType[count]=card_type;
														for(int card_index=0;card_index<cbCardCount;card_index++){
															out_card_type_result.cbCardData[count][card_index]=tempCardData[card_index];
														}
														out_card_type_result.cbEachHandCardCount[count]=cbCardCount;
														out_card_type_result.cbCardTypeCount++;
													}
												}
											}
										}
									}
									int card_type=this.GetCardType(tempCardData, cbCardCount,cbCardData);
									if(cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD){
										continue;
									}
									if(card_type != GameConstants.PDK_CT_ERROR){
										int count=out_card_type_result.cbCardTypeCount;
										out_card_type_result.cbCardType[count]=card_type;
										for(int card_index=0;card_index<cbCardCount;card_index++){
											out_card_type_result.cbCardData[count][card_index]=tempCardData[card_index];
										}
										out_card_type_result.cbEachHandCardCount[count]=cbCardCount;
										out_card_type_result.cbCardTypeCount++;
									}
								}
							}
						}
					}
					int card_type=this.GetCardType(tempCardData, cbCardCount,cbCardData);
					if(cbCardCount > 4 && card_type == GameConstants.PDK_CT_BOMB_CARD){
						continue;
					}
					if(card_type != GameConstants.PDK_CT_ERROR){
						int count=out_card_type_result.cbCardTypeCount;
						out_card_type_result.cbCardType[count]=card_type;
						for(int card_index=0;card_index<cbCardCount;card_index++){
							out_card_type_result.cbCardData[count][card_index]=tempCardData[card_index];
						}
						out_card_type_result.cbEachHandCardCount[count]=cbCardCount;
						out_card_type_result.cbCardTypeCount++;
					}
				}
			}
		}

	}
	//对比扑克
	public boolean CompareCardLaizi(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount,int card_type)
	{
		int cbFirstCardTemp[]=new int[cbFirstCount];
		int cbNextCardTemp[]=new int[cbNextCount];
		for(int j=0;j<cbFirstCount;j++){
			cbFirstCardTemp[j]=cbFirstCard[j];
		}
		for(int j=0;j<cbNextCount;j++){
			cbNextCardTemp[j]=cbNextCard[j];
		}
		
		
		int cbNextType=GetCardType(cbFirstCardTemp,cbFirstCount,cbFirstCard);
		int cbFirstType=GetCardType(cbNextCardTemp,cbNextCount,cbNextCard);

		//类型判断
		if (cbNextType==GameConstants.PDK_CT_ERROR) return false;
		//其中一方有炸弹判断
		if((cbFirstType >=GameConstants.PDK_CT_RUAN_BOMB || cbNextType >= GameConstants.PDK_CT_RUAN_BOMB) && cbFirstType != cbNextType){
			return cbFirstType>cbNextCount;
		}
		//规则判断
		if((cbFirstType!=cbNextType) && 
			cbNextType == GameConstants.PDK_CT_HONG_HUA_SHUN && cbFirstType==GameConstants.PDK_CT_SINGLE_LINE)return true;
		if ((cbFirstType!=cbNextType)||(cbFirstType!=GameConstants.PDK_CT_BOMB_CARD && cbFirstCount!=cbNextCount)) return false;

		//开始对比
		switch (cbNextType)
		{
		case GameConstants.PDK_CT_SINGLE:
		case GameConstants.PDK_CT_DOUBLE:
		case GameConstants.PDK_CT_THREE:
		case GameConstants.PDK_CT_SINGLE_LINE:
		case GameConstants.PDK_CT_DOUBLE_LINE:
		case GameConstants.PDK_CT_PLANE:
			{
				//获取数值
				int cbNextLogicValue=GetCardLogicValue(cbNextCardTemp[0]);
				int cbFirstLogicValue=GetCardLogicValue(cbFirstCardTemp[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE:	
		case GameConstants.PDK_CT_THREE_TAKE_TWO:
		case GameConstants.PDK_CT_THREE_TAKE_ONE:
			{
				//分析扑克
				tagAnalyseResult NextResult = new tagAnalyseResult();
				tagAnalyseResult FirstResult = new tagAnalyseResult();
				AnalysebCardData(cbNextCardTemp,cbNextCount,NextResult);
				AnalysebCardData(cbFirstCardTemp,cbFirstCount,FirstResult);

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(NextResult.cbThreeCardData[0]);
				int cbFirstLogicValue=GetCardLogicValue(FirstResult.cbThreeCardData[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_TWO:
		case GameConstants.PDK_CT_FOUR_LINE_TAKE_THREE:
			{
				//分析扑克
				tagAnalyseResult NextResult = new tagAnalyseResult();
				tagAnalyseResult FirstResult = new tagAnalyseResult();
				AnalysebCardData(cbNextCardTemp,cbNextCount,NextResult);
				AnalysebCardData(cbFirstCardTemp,cbFirstCount,FirstResult);

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(NextResult.cbFourCardData[0]);
				int cbFirstLogicValue=GetCardLogicValue(FirstResult.cbFourCardData[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.PDK_CT_BOMB_CARD:
		case GameConstants.PDK_CT_RUAN_BOMB:
			{
				//数目判断
				if ( cbNextCount != cbFirstCount ) return cbNextCount > cbFirstCount;

				//获取数值
				int cbNextLogicValue=GetCardLogicValue(cbNextCardTemp[0]);
				int cbFirstLogicValue=GetCardLogicValue(cbFirstCardTemp[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		}

		return false;
	}
	public int get_change_data(int cbCardData[],int cbChangeData[], int cbCardCount, int card_type){
		
		tagOutCardTypeResult out_card_type_result=new tagOutCardTypeResult();
		GetCardTypeLaizi(cbCardData,cbCardCount,out_card_type_result);
		for(int i=0;i<out_card_type_result.cbCardTypeCount;i++){
			if(card_type == GameConstants.PDK_CT_ERROR){
				if(out_card_type_result.cbCardType[0] > GameConstants.PDK_CT_PASS){
					for(int card_index=0;card_index<out_card_type_result.cbEachHandCardCount[i];card_index++){
						cbChangeData[card_index]=out_card_type_result.cbCardData[i][card_index];
						return card_type;
					}
				}
			}else{
				if(out_card_type_result.cbCardType[i] == card_type){
					for(int card_index=0;card_index<out_card_type_result.cbEachHandCardCount[i];card_index++){
						cbChangeData[card_index]=out_card_type_result.cbCardData[i][card_index];
					}
					return card_type;
				}
			}
		}
		for(int i=0;i<cbCardCount;i++){
			cbChangeData[i]=cbCardData[i];
		}
		return this.GetCardType(cbCardData, cbCardCount, cbChangeData);
		
	}
	//分析炸弹
	public boolean SearchBoomCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
			
		int cbTmpCardData[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCardData[i]=cbHandCardData[i];
		}


		int cbBomCardCount = 0;
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);

		if(cbHandCardCount<2) return false;

		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCardData[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCardData[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}
			if(cbSameCount+nLaiziCount >= 4){
				int cbBomCardData[]=new int[cbSameCount+nLaiziCount];
				for(int j=0;j<cbSameCount;j++){
					cbBomCardData[j]=cbTmpCardData[i+j];
				}
				for(int j=cbSameCount;j<cbSameCount+nLaiziCount;j++){
					cbBomCardData[j]=cbBomCardData[j-1];
				}
				if(CompareCard(cbTurnCardData,cbBomCardData,cbTurnCardCount,cbSameCount+nLaiziCount)){
					return true;
				}
			}

			//设置索引
			i+=cbSameCount-1;
		}
		return false;
	}
	//判断是否有压牌
	//出牌搜索
	public boolean SearchOutCardLaiZi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount,int cbChangeData[]){
			
		//获取出牌类型
		int card_type=GetCardType(cbChangeData,cbTurnCardCount,cbTurnCardData);
		if (card_type==GameConstants.PDK_CT_MAGIC_BOOM) return false;
		if(GetLaiZiCount(cbHandCardData, cbHandCardCount) == 4)return true;	
		//搜索炸弹
		if(SearchBoomCardLaizi(cbHandCardData,cbHandCardCount,cbChangeData,cbTurnCardCount)){
			return true;
		}
		//搜索顺子
		if(card_type == GameConstants.PDK_CT_SINGLE_LINE){
			return SearchSingleLineCardLaiZi(cbHandCardData,cbHandCardCount,cbChangeData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_DOUBLE_LINE){
			return SearchDoubleLineCardLaizi(cbHandCardData,cbHandCardCount,cbChangeData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_PLANE){
			return SearchThreeLineCardLaizi(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_SINGLE){
			return SearchSingleCardLaizi(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_DOUBLE){
			return SearchDoubleCardLaizi(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
		if(card_type == GameConstants.PDK_CT_THREE || card_type == GameConstants.PDK_CT_THREE_TAKE_ONE
				|| card_type == GameConstants.PDK_CT_THREE_TAKE_TWO || card_type == GameConstants.PDK_CT_FOUR_LINE_TAKE_ONE){
			return SearchThreeCardLaizi(cbHandCardData,cbHandCardCount,cbTurnCardData,cbTurnCardCount);
		}
			
		//int cbFirstType=GetCardType(cbFirstCard,cbFirstCount);
		return false;
	}
	//分析顺子
	public boolean SearchSingleLineCardLaiZi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount)
	{
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		//数据校验
		if(cbHandCardCount<cbTurnCardCount) return false;
		for(int i=0;i<cbHandCardCount;i++){
			if(GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)){
				for(int valueone=1;valueone<13;valueone++){
					//一个癞子变牌
					cbTmpCard[i]=(0x0F-valueone)%13+1;
					if(nLaiziCount >= 2){
						for(int j=i+1;j<cbHandCardCount;j++){
							if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
								for(int valuetwo=1;valuetwo<14;valuetwo++){
									//两个癞子变牌
									cbTmpCard[j]=(0x0F-valuetwo)%13+1;
									if(nLaiziCount >= 3){
										for(int x=j+1;x<cbHandCardCount;x++){
											if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
												for(int valuethree=1;valuethree<14;valuethree++){
													//三个癞子变牌
													cbTmpCard[x]=(0x0F-valuethree)%13+1;
													if(nLaiziCount >= 4){
														for(int y=x+1;y<cbHandCardCount;y++){
															if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
																for(int valuefour=1;valuefour<14;valuefour++){
																	//四个癞子变牌
																	cbTmpCard[y]=(0x0F-valuefour)%13+1;
																	if(SearchSingleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
																		return true;
																	}
																}
															}
														}
													}
													if(SearchSingleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
														return true;
													}
												}
											}
										}
									}
									if(SearchSingleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
										return true;
									}
								}
							}
						}
					}
					if(SearchSingleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
						return true;
					}
				}
			}
		}
		return false;
	}
	//搜索连对
	public boolean SearchDoubleLineCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		//数据校验
		if(cbHandCardCount<cbTurnCardCount) return false;
		for(int i=0;i<cbHandCardCount;i++){
			if(GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)){
				for(int valueone=1;valueone<13;valueone++){
					//一个癞子变牌
					cbTmpCard[i]=(0x0F-valueone)%13+1;
					if(nLaiziCount >= 2){
						for(int j=i+1;j<cbHandCardCount;j++){
							if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
								for(int valuetwo=1;valuetwo<14;valuetwo++){
									//两个癞子变牌
									cbTmpCard[j]=(0x0F-valuetwo)%13+1;
									if(nLaiziCount >= 3){
										for(int x=j+1;x<cbHandCardCount;x++){
											if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
												for(int valuethree=1;valuethree<14;valuethree++){
													//三个癞子变牌
													cbTmpCard[x]=(0x0F-valuethree)%13+1;
													if(nLaiziCount >= 4){
														for(int y=x+1;y<cbHandCardCount;y++){
															if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
																for(int valuefour=1;valuefour<14;valuefour++){
																	//四个癞子变牌
																	cbTmpCard[y]=(0x0F-valuefour)%13+1;
																	if(SearchDoubleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
																		return true;
																	}
																}
															}
														}
													}
													if(SearchDoubleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
														return true;
													}
												}
											}
										}
									}
									if(SearchDoubleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
										return true;
									}
								}
							}
						}
					}
					if(SearchDoubleLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
						return true;
					}
				}
			}
		}

		return true;

	}
	//搜索飞机
	public boolean SearchThreeLineCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		//数据校验
		if(cbHandCardCount<cbTurnCardCount) return false;
		for(int i=0;i<cbHandCardCount;i++){
			if(GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)){
				for(int valueone=1;valueone<13;valueone++){
					//一个癞子变牌
					cbTmpCard[i]=(0x0F-valueone)%13+1;
					if(nLaiziCount >= 2){
						for(int j=i+1;j<cbHandCardCount;j++){
							if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
								for(int valuetwo=1;valuetwo<14;valuetwo++){
									//两个癞子变牌
									cbTmpCard[j]=(0x0F-valuetwo)%13+1;
									if(nLaiziCount >= 3){
										for(int x=j+1;x<cbHandCardCount;x++){
											if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
												for(int valuethree=1;valuethree<14;valuethree++){
													//三个癞子变牌
													cbTmpCard[x]=(0x0F-valuethree)%13+1;
													if(nLaiziCount >= 4){
														for(int y=x+1;y<cbHandCardCount;y++){
															if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
																for(int valuefour=1;valuefour<14;valuefour++){
																	//四个癞子变牌
																	cbTmpCard[y]=(0x0F-valuefour)%13+1;
																	if(SearchThreeLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
																		return true;
																	}
																}
															}
														}
													}
													if(SearchThreeLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
														return true;
													}
												}
											}
										}
									}
									if(SearchThreeLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
										return true;
									}
								}
							}
						}
					}
					if(SearchThreeLineCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
						return true;
					}
				}
			}
		}
		return false;
	}
	//搜索单张
	public boolean SearchSingleCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		if(GetCardLogicValue(cbTurnCardData[0]) >= 15){
			return false;
		}else{
			int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
			if(nLaiziCount > 0){
				return true;
			}
		}
		for(int i=0; i<cbHandCardCount; ++i){
			if(GetCardLogicValue(cbTmpCard[i])>GetCardLogicValue(cbTurnCardData[0])) 
			{
				return true;
			}
		}
			
		return false;
	}
	//搜索对子
	public boolean SearchDoubleCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		if(GetCardLogicValue(cbTurnCardData[0]) >= 15){
			return false;
		}else{
			if(nLaiziCount >= 2){
				return true;
			}
		}
		//扑克分析
		for (int i=0;i<cbHandCardCount;i++)
		{
			//变量定义
			int cbSameCount=1;
			int cbLogicValue=GetCardLogicValue(cbTmpCard[i]);

			//搜索同牌
			for (int j=i+1;j<cbHandCardCount;j++)
			{
				//获取扑克
				if (GetCardLogicValue(cbTmpCard[j])!=cbLogicValue) break;

				//设置变量
				cbSameCount++;
			}

			if(cbSameCount+nLaiziCount>=2 && GetCardLogicValue(cbTmpCard[i])>GetCardLogicValue(cbTurnCardData[0]))
			{
				return true;
			}
			//设置索引
			i+=cbSameCount-1;
		}
		return false;
	}
	//搜索三张
	public boolean SearchThreeCardLaizi(int cbHandCardData[], int cbHandCardCount, int cbTurnCardData[], int cbTurnCardCount){
		int cbTmpCard[] = new int[cbHandCardCount];
		for(int i=0;i<cbHandCardCount;i++){
			cbTmpCard[i]=cbHandCardData[i];
		}
		int nLaiziCount=this.GetLaiZiCount(cbHandCardData, cbHandCardCount);
		//数据校验
		if(cbHandCardCount<cbTurnCardCount) return false;
		for(int i=0;i<cbHandCardCount;i++){
			if(GetCardLogicValue(cbHandCardData[i]) == GetCardLogicValue(this._laizi)){
				for(int valueone=1;valueone<13;valueone++){
					//一个癞子变牌
					cbTmpCard[i]=(0x0F-valueone)%13+1;
					if(nLaiziCount >= 2){
						for(int j=i+1;j<cbHandCardCount;j++){
							if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
								for(int valuetwo=1;valuetwo<14;valuetwo++){
									//两个癞子变牌
									cbTmpCard[j]=(0x0F-valuetwo)%13+1;
									if(nLaiziCount >= 3){
										for(int x=j+1;x<cbHandCardCount;x++){
											if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
												for(int valuethree=1;valuethree<14;valuethree++){
													//三个癞子变牌
													cbTmpCard[x]=(0x0F-valuethree)%13+1;
													if(nLaiziCount >= 4){
														for(int y=x+1;y<cbHandCardCount;y++){
															if(GetCardLogicValue(cbHandCardData[j]) == GetCardLogicValue(this._laizi)){
																for(int valuefour=1;valuefour<14;valuefour++){
																	//四个癞子变牌
																	cbTmpCard[y]=(0x0F-valuefour)%13+1;
																	if(SearchThreeCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
																		return true;
																	}
																}
															}
														}
													}
													if(SearchThreeCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
														return true;
													}
												}
											}
										}
									}
									if(SearchThreeCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
										return true;
									}
								}
							}
						}
					}
					if(SearchThreeCard(cbTmpCard,cbHandCardCount,cbTurnCardData,cbTurnCardCount)){
						return true;
					}
				}
			}
		}
		return false;
	}
	public boolean has_rule(int cbRule) {
		return FvMask.has_any(_game_rule_index, FvMask.mask(cbRule));
	}

}
