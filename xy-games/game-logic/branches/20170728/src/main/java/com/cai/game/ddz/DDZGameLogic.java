/**
 * 
 */
package com.cai.game.ddz;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.GameConstants;
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
	int							cbEightCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//八张扑克
	int							cbSevenCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//七张扑克
	int							cbSixCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//六张扑克
	int							cbFiveCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//五张扑克
	int							cbFourCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//四张扑克
	int							cbThreeCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];			//三张扑克
	int							cbDoubleCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];		//两张扑克
	int							cbSignedCardData[]=new int[GameConstants.DDZ_MAX_COUNT_JD];		//单张扑克
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
	int							cbResultCard[] = new int[GameConstants.DDZ_MAX_COUNT_JD];			//结果扑克
	
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
	int							cbCardType;							//扑克类型
	int							cbCardTypeCount;					//牌型数目
	int							cbEachHandCardCount[] = new int[GameConstants.MAX_TYPE_COUNT];//每手个数
	int							cbCardData[][] = new int[GameConstants.MAX_TYPE_COUNT][GameConstants.DDZ_MAX_COUNT_JD];//扑克数据
	public tagOutCardTypeResult(){
		cbCardType = 0;
		cbCardTypeCount = 0;
		
		Arrays.fill(cbEachHandCardCount,0);
		Arrays.fill(cbCardData,0);
	}
	
	public void Reset(){
		cbCardType = 0;
		cbCardTypeCount = 0;
		
		Arrays.fill(cbEachHandCardCount,0);
		Arrays.fill(cbCardData,0);
	}
};


//扑克信息
class tagHandCardInfo {
	int						cbHandCardData[] = new int[GameConstants.DDZ_MAX_COUNT_JD];				//扑克数据
	int						cbHandCardCount;							//扑克数目
	tagOutCardTypeResult		CardTypeResult[] = new tagOutCardTypeResult[13] ;					//分析数据

	//初始数据
	public tagHandCardInfo(){
		Arrays.fill(cbHandCardData,0);
		cbHandCardCount = 0;
		for(int i=0;i<13;i++){
			CardTypeResult[i].Reset();
		}
	}
	public void Reset(){
		Arrays.fill(cbHandCardData,0);
		cbHandCardCount = 0;
		for(int i=0;i<13;i++){
			CardTypeResult[i].Reset();
		}
	}
};

//栈结构
class tagStackHandCardInfo {

	//成员变量
	private ArrayList<tagHandCardInfo>		m_HandCardInfoFreeArray;					//扑克信息
	private ArrayList<tagHandCardInfo>			m_HandCardInfoArray;						//扑克信息
	public tagStackHandCardInfo() { 
		m_HandCardInfoFreeArray.clear();
		m_HandCardInfoArray.clear();
	}


	//元素压栈
	public void Push( tagHandCardInfo  pHandCardInfo ) {

		//是否还有空间
		if ( 0 < m_HandCardInfoFreeArray.size() ) {
			//获取空间
			tagHandCardInfo  pHandCardInfoFree = m_HandCardInfoFreeArray.get(0);
			m_HandCardInfoFreeArray.remove(0);

			//元素赋值
			for(int i=0;i<GameConstants.DDZ_MAX_COUNT_JD;i++){
				pHandCardInfoFree.cbHandCardData[i]=pHandCardInfo.cbHandCardData[i];
			}
			for(int i=0;i<13;i++){
				pHandCardInfoFree.CardTypeResult[i]=pHandCardInfo.CardTypeResult[i];
			}
			pHandCardInfoFree.cbHandCardCount = pHandCardInfo.cbHandCardCount;

			//压入栈顶
			int nECount = m_HandCardInfoArray.size() ; 
			m_HandCardInfoArray.set(nECount, pHandCardInfoFree);
		}
		else {
			//申请空间
			tagHandCardInfo  pNewHandCardInfo = new tagHandCardInfo() ;

			//元素赋值
			for(int i=0;i<GameConstants.DDZ_MAX_COUNT_JD;i++){
				pNewHandCardInfo.cbHandCardData[i]=pHandCardInfo.cbHandCardData[i];
			}
			for(int i=0;i<13;i++){
				pNewHandCardInfo.CardTypeResult[i]=pHandCardInfo.CardTypeResult[i];
			}
			pNewHandCardInfo.cbHandCardCount = pHandCardInfo.cbHandCardCount;

			//压入栈顶
			int nECount = m_HandCardInfoArray.size() ; 
			m_HandCardInfoArray.set( nECount, pNewHandCardInfo );
		}
		
	}

	//弹出栈顶
	public void Pop() {

		//非空判断
		if ( IsEmpty() ) return ;

		//获取元素
		int nECount = m_HandCardInfoArray.size() ;
		tagHandCardInfo  pTopHandCardInfo = m_HandCardInfoArray.get( nECount - 1 );

		//移除元素
		m_HandCardInfoArray.remove( nECount - 1 );

		//保存空间
		m_HandCardInfoFreeArray.add( pTopHandCardInfo );		
	}

	//初始栈
	public void InitStack() {

		//保存空间
		while ( 0 < m_HandCardInfoArray.size() ) {
			tagHandCardInfo pHandCardInfo = m_HandCardInfoArray.get(0);
			m_HandCardInfoArray.remove( 0 );
			m_HandCardInfoFreeArray.add( pHandCardInfo );
		}
	}

	//清空栈
	public void ClearAll() {

		//释放内存
		while ( 0 < m_HandCardInfoArray.size() ) {
			m_HandCardInfoArray.remove(0);
		}

		//释放内存
		while ( 0 < m_HandCardInfoFreeArray.size() ) {
			tagHandCardInfo pHandCardInfo = m_HandCardInfoFreeArray.get(0);
			m_HandCardInfoFreeArray.remove( 0 );
		}
	}

	//获取栈顶
	public void GetTop( tagHandCardInfo pHandCardInfo ) {

		//非空判断
		if ( IsEmpty() ) {
			return;
		}

		//获取元素
		int nECount = m_HandCardInfoArray.size() ;
		pHandCardInfo = m_HandCardInfoArray.get(nECount - 1);
	}

	//空判断
	public boolean IsEmpty() {
		return m_HandCardInfoArray.isEmpty();
	}
};

public class DDZGameLogic {

	private int cbIndexCount=5;

	public DDZGameLogic() {

	}
	//获取类型
	public int GetCardType(int cbCardData[], int cbCardCount)
	{
		//简单牌型
		switch (cbCardCount)
		{
		case 0:	//空牌
			{
				return GameConstants.DDZ_CT_ERROR;
			}
		case 1: //单牌
			{
				return GameConstants.DDZ_CT_SINGLE;
			}
		case 2:	//对牌
			{
				//牌型判断
				if (GetCardLogicValue(cbCardData[0])==GetCardLogicValue(cbCardData[1])) return GameConstants.DDZ_CT_DOUBLE;

				return GameConstants.DDZ_CT_ERROR;
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
			if ( bMissileCard ) return GameConstants.DDZ_CT_MISSILE_CARD;
		}
		}

		//分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData,cbCardCount,AnalyseResult);

		//炸弹判断
		if ( 4 <= cbCardCount && cbCardCount <= 8 )
		{
			//牌型判断
			if ((AnalyseResult.cbFourCount==1)&&(cbCardCount==4)) return GameConstants.DDZ_CT_BOMB_CARD;
			if ((AnalyseResult.cbFiveCount==1)&&(cbCardCount==5)) return GameConstants.DDZ_CT_BOMB_CARD;
			if ((AnalyseResult.cbSixCount==1)&&(cbCardCount==6)) return GameConstants.DDZ_CT_BOMB_CARD;
			if ((AnalyseResult.cbSevenCount==1)&&(cbCardCount==7)) return GameConstants.DDZ_CT_BOMB_CARD;
			if ((AnalyseResult.cbEightCount==1)&&(cbCardCount==8)) return GameConstants.DDZ_CT_BOMB_CARD;
		}

		//三牌判断
		if (AnalyseResult.cbThreeCount>0)
		{
			//三条类型
			if(AnalyseResult.cbThreeCount==1 && cbCardCount==3) return GameConstants.DDZ_CT_THREE ;

			//连牌判断
			if (AnalyseResult.cbThreeCount>1)
			{
				//变量定义
				int CardData=AnalyseResult.cbThreeCardData[0];
				int cbFirstLogicValue=GetCardLogicValue(CardData);

				//错误过虑
				if (cbFirstLogicValue>=15) return GameConstants.DDZ_CT_ERROR;

				//连牌判断
				for (int i=1;i<AnalyseResult.cbThreeCount;i++)
				{
					int CardDatatemp=AnalyseResult.cbThreeCardData[i*3];
					if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)-i)) return GameConstants.DDZ_CT_ERROR;
				}
			}

			//牌形判断
			if (AnalyseResult.cbThreeCount*3==cbCardCount) return GameConstants.DDZ_CT_THREE_LINE;
			if ((AnalyseResult.cbThreeCount*4==cbCardCount)&&(AnalyseResult.cbSignedCount==AnalyseResult.cbThreeCount)) return GameConstants.DDZ_CT_THREE_TAKE_ONE;
			if ((AnalyseResult.cbThreeCount*5==cbCardCount)&&(AnalyseResult.cbDoubleCount==AnalyseResult.cbThreeCount)) return GameConstants.DDZ_CT_THREE_TAKE_TWO;

			return GameConstants.DDZ_CT_ERROR;
		}

		//两张类型
		if (AnalyseResult.cbDoubleCount>=3)
		{
			//变量定义
			int CardData=AnalyseResult.cbDoubleCardData[0];
			int cbFirstLogicValue=GetCardLogicValue(CardData);

			//错误过虑
			if (cbFirstLogicValue>=15) return GameConstants.DDZ_CT_ERROR;

			//连牌判断
			for (int i=1;i<AnalyseResult.cbDoubleCount;i++)
			{
				int CardDatatemp=AnalyseResult.cbDoubleCardData[i*2];
				if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)-i)) return GameConstants.DDZ_CT_ERROR;
			}

			//二连判断
			if ((AnalyseResult.cbDoubleCount*2)==cbCardCount) return GameConstants.DDZ_CT_DOUBLE_LINE;

			return GameConstants.DDZ_CT_ERROR;
		}

		//单张判断
		if ((AnalyseResult.cbSignedCount>=5)&&(AnalyseResult.cbSignedCount==cbCardCount))
		{
			//变量定义
			int CardData=AnalyseResult.cbSignedCardData[0];
			int cbFirstLogicValue=GetCardLogicValue(CardData);

			//错误过虑
			if (cbFirstLogicValue>=15) return GameConstants.DDZ_CT_ERROR;

			//连牌判断
			for (int i=1;i<AnalyseResult.cbSignedCount;i++)
			{
				int CardDatatemp=AnalyseResult.cbSignedCardData[i];
				if (cbFirstLogicValue!=(GetCardLogicValue(CardDatatemp)-i)) return GameConstants.DDZ_CT_ERROR;
			}

			return GameConstants.DDZ_CT_SINGLE_LINE;
		}

		return GameConstants.DDZ_CT_ERROR;
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
					AnalyseResult.cbFourCardData[cbIndex*cbSameCount]=cbCardData[i];
					AnalyseResult.cbFourCardData[cbIndex*cbSameCount+1]=cbCardData[i+1];
					AnalyseResult.cbFourCardData[cbIndex*cbSameCount+2]=cbCardData[i+2];
					AnalyseResult.cbFourCardData[cbIndex*cbSameCount+3]=cbCardData[i+3];
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
		int cbNextType=GetCardType(cbNextCard,cbNextCount);
		int cbFirstType=GetCardType(cbFirstCard,cbFirstCount);

		//类型判断
		if (cbNextType==GameConstants.DDZ_CT_ERROR) return false;
		if (cbNextType==GameConstants.DDZ_CT_MISSILE_CARD) return true;
		if ( cbFirstType == GameConstants.DDZ_CT_MISSILE_CARD ) return false;

		//炸弹判断
		if ((cbFirstType!=GameConstants.DDZ_CT_BOMB_CARD)&&(cbNextType==GameConstants.DDZ_CT_BOMB_CARD)) return true;
		if ((cbFirstType==GameConstants.DDZ_CT_BOMB_CARD)&&(cbNextType!=GameConstants.DDZ_CT_BOMB_CARD)) return false;

		//规则判断
		if ((cbFirstType!=cbNextType)||(cbFirstType!=GameConstants.DDZ_CT_BOMB_CARD && cbFirstCount!=cbNextCount)) return false;

		//开始对比
		switch (cbNextType)
		{
		case GameConstants.DDZ_CT_SINGLE:
		case GameConstants.DDZ_CT_DOUBLE:
		case GameConstants.DDZ_CT_THREE:
		case GameConstants.DDZ_CT_SINGLE_LINE:
		case GameConstants.DDZ_CT_DOUBLE_LINE:
		case GameConstants.DDZ_CT_THREE_LINE:
			{
				//获取数值
				int cbNextLogicValue=GetCardLogicValue(cbNextCard[0]);
				int cbFirstLogicValue=GetCardLogicValue(cbFirstCard[0]);

				//对比扑克
				return cbNextLogicValue>cbFirstLogicValue;
			}
		case GameConstants.DDZ_CT_THREE_TAKE_TWO:
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
		case GameConstants.DDZ_CT_FOUR_TAKE_ONE:
		case GameConstants.DDZ_CT_FOUR_TAKE_TWO:
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
		case GameConstants.DDZ_CT_BOMB_CARD:
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
		for (int i=0;i<card_count;i++) logic_value[i]=GetCardLogicValue(card_date[i]);

		//排序操作
		boolean sorted=true;
		int temp_date,last=card_count-1;
		do
		{
			sorted=true;
			for (int i=0;i<last;i++)
			{
				if ((logic_value[i]<logic_value[i+1])||
					((logic_value[i]==logic_value[i+1])&&(card_date[i]<card_date[i+1])))
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
	public int GetCardLogicValue(int CardData){
		//扑克属性
		int cbCardColor=get_card_color(CardData);
		int cbCardValue=get_card_value(CardData);

		//转换数值
		if (cbCardColor==0x40) return cbCardValue+2;
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
