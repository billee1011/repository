package com.cai.hp;

import java.util.List;

import org.apache.log4j.Logger;

import com.cai.common.constant.HPGameConstans;
import com.cai.common.domain.WeaveItem;
import com.cai.common.util.RandomUtil;
import com.cai.hp.model.AnalyseItemHP;
import com.cai.hp.model.KindItem;
import com.cai.hp.model.TongCardResult;

public class HPGameLogic {

	private static Logger logger = Logger.getLogger(HPGameLogic.class);

	private int _magic_card_index[];
	private int _magic_card_count;

	// 获取数值
	public int get_card_value(int card) {
		return card & HPGameConstans.LOGIC_MASK_VALUE;
	}

	// 获取花色
	public int get_card_color(int card) {
		return (card & HPGameConstans.LOGIC_MASK_COLOR) >> 4;
	}

	// 有效判断
	public boolean is_valid_card(int card) {
		int cbValue = get_card_value(card);// (card&HPGameConstans.LOGIC_MASK_VALUE);
		int cbColor = get_card_color(card);// (card&HPGameConstans.LOGIC_MASK_COLOR);
		// 如果颜色是0||1，牌值必须是0--A(10)
		// 如果颜色是2，牌值必须是0--C(12)
		return (((cbColor == 0 || cbColor == 1) && (cbValue > 0 && cbValue <= 10))
				|| ((cbColor == 2) && (cbValue >= 1 && cbValue <= 12)));
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

	// 获取组合
	public int get_weave_card(int cbWeaveKind, int cbCenterCard, int cbCardBuffer[]) {
		// 组合扑克
		// 组合扑克
		switch (cbWeaveKind) {
		case HPGameConstans.WIK_DUI: // 对牌操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;

			return 3;
		}
		case HPGameConstans.WIK_SI_ZHAO: // 四招操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;
			cbCardBuffer[4] = cbCenterCard;

			return 5;
		}
		case HPGameConstans.WIK_SAN_ZHAO: // 三招操作
		{
			// 设置变量
			cbCardBuffer[0] = cbCenterCard;
			cbCardBuffer[1] = cbCenterCard;
			cbCardBuffer[2] = cbCenterCard;
			cbCardBuffer[3] = cbCenterCard;

			return 4;
		}
		default: {
			logger.error("get_weave_card:invalid cbWeaveKind");
		}
		}

		return 0;
	}

	// 给定一个扑克，根据扑克的值得到扑克牌等级
	public int getCardRank(int cbCardData) {
		// 断言传进来的牌是合法的
		boolean isValid = is_valid_card(cbCardData);

		if (isValid == false)
			return -1;
		// 1.上，可，已，千，返回3
		if (cbCardData == 0x23 || cbCardData == 0x29 || cbCardData == 0x22 || cbCardData == 0x28)
			return 3;
		// 2.孔，人，礼，返回5
		if (cbCardData == 0x21 || cbCardData == 0x25 || cbCardData == 0x2B)
			return 5;
		// 3.大，土，知，子返回 7
		if (cbCardData == 0x24 || cbCardData == 0x26 || cbCardData == 0x2A || cbCardData == 0x2C)
			return 7;
		// 4.如果是数字牌，直接得到牌值
		else
			return cbCardData & 0x0F;

	}

	// 动作等级
	public int getUserActionRank(int cbUserAction) {
		// 胡牌等级
		// if (cbUserAction&HPGameConstans.WIK_CHI_HU) { return 4; }
		//
		// //招牌牌等级
		// if (cbUserAction&(HPGameConstans.WIK_SAN_ZHAO |
		// HPGameConstans.WIK_SI_ZHAO)) { return 3; }
		//
		// //对牌等级
		// if (cbUserAction&HPGameConstans.WIK_DUI) { return 2; }
		return 0;
	}

	public void saveHuaCard(int cbCardData[], int bCardCount, int cbHuaCardIndex[]) {
		// 找花牌
		for (int i = 0; i < bCardCount; i++) {
			// 如果是花牌，保存到花牌索引数组中
			if ((cbCardData[i] & HPGameConstans.LOGIC_MASK_COLOR) == 0x10) {
				int index = switchToCardData(cbCardData[i]);
				cbHuaCardIndex[index / 2]++;
				assert (cbHuaCardIndex[index / 2] <= 2);
			}
		}
	}

	// 判断是否可以对牌,如果索引数组中有两张一样的,返回对操作码 cbCardIndex[HPGameConstans.MAX_INDEX]
	public int estimateDuiCard(int cbCardIndex[], int cbSiTong[], int cbSiTongCount, int cbCurrentCard) {
		// 参数效验
		assert is_valid_card(cbCurrentCard);

		int cbIndex = switchToCardData(cbCurrentCard);

		// 如果给定的牌已经在当前玩家的四藏牌里面,不进行三招分析
		if (cbCardIndex[cbIndex] >= 4) {
			for (int i = 0; i < cbSiTongCount; i++) {
				if (cbIndex == cbSiTong[i]) {
					return HPGameConstans.WIK_NULL;
				}
			}
		}

		// 对牌判断
		return (cbCardIndex[cbIndex] >= 2) ? HPGameConstans.WIK_DUI : HPGameConstans.WIK_NULL;
	}

	// 判断招牌 cbCardIndex[MAX_INDEX]
	public int estimateZhaoCard(int cbCardIndex[], int cbSiTong[], int cbSiTongCount, int cbCurrentCard) {
		// 参数效验
		assert is_valid_card(cbCurrentCard);
		int cbActionMask = HPGameConstans.WIK_NULL;

		int cbIndex = switchToCardData(cbCurrentCard);
		boolean bSanZhao = true;
		// 如果给定的牌已经在当前玩家的四藏牌里面,不进行三招分析
		for (int i = 0; i < cbSiTongCount; i++) {
			if (cbIndex == cbSiTong[i]) {
				bSanZhao = false;
				break;
			}
		}
		if (bSanZhao == false) {
			if (cbCardIndex[cbIndex] == 4)
				cbActionMask |= HPGameConstans.WIK_SI_ZHAO;
		} else {
			if (cbCardIndex[cbIndex] == 3)
				cbActionMask |= HPGameConstans.WIK_SAN_ZHAO;
			if (cbCardIndex[cbIndex] == 4)
				cbActionMask |= HPGameConstans.WIK_SI_ZHAO | HPGameConstans.WIK_SAN_ZHAO;

		}

		return cbActionMask;
	}

	// 11_17
	// 根据扑克牌数据得到花牌保存到索引中,返回花牌的个数
	public int getHuaCard(int cbCardData[], int bCardCount, int cbHuaCardIndex[]) {
		int bHuaCardCount = 0;
		// 循环从牌数据数组中得到花牌
		for (int i = 0; i < bCardCount; i++) {
			// 参数效验
			assert is_valid_card(cbCardData[i]);

			// 如果是花牌，将花牌转换成索引存放到花牌索引数组中
			if ((cbCardData[i] & HPGameConstans.LOGIC_MASK_COLOR) == 0x10) {
				cbHuaCardIndex[switchToCardData(cbCardData[i]) / 2]++;

				assert (cbHuaCardIndex[switchToCardData(cbCardData[i]) / 2] <= 2);
				bHuaCardCount++;
			}
		}
		return bHuaCardCount;
	}

	// 分析玩家手中牌数组中是否可以藏牌，四统 int cbCardIndex[MAX_INDEX]
	public int analyseSiTong(int cbCardIndex[], TongCardResult SiTongResult) {
		int cbActionMask = HPGameConstans.WIK_NULL;
		// ZeroMemory(&SiTongResult,sizeof(SiTongResult));

		// 分析索引数组中是否有四张相同的,如果有得到四统的操作
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			if (cbCardIndex[i] >= 4) {
				SiTongResult.cbCardData[SiTongResult.cbCardCount++] = switchToCardData(i);
				cbActionMask |= HPGameConstans.WIK_SI_TONG;
			}
		}
		return cbActionMask;
	}

	// 分析玩家手中牌数组中是否可以藏牌，五统 BYTE cbCardIndex[MAX_INDEX]
	public int analyseWuTong(int cbCardIndex[], WeaveItem WeaveItem[], int cbItemCount,
			TongCardResult WuTongResult) {
		int cbActionMask = HPGameConstans.WIK_NULL;
		// ZeroMemory(&WuTongResult,sizeof(WuTongResult));

		// 分析索引数组中是否有四张相同的,如果有得到五统的操作
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			if (cbCardIndex[i] == 5) {
				WuTongResult.cbCardData[WuTongResult.cbCardCount++] = switchToCardData(i);
				cbActionMask |= HPGameConstans.WIK_WU_TONG;
			}
		}

		// 组合牌中藏牌分析
		for (int j = 0; j < cbItemCount; j++) {
			int cbWeaveKind = WeaveItem[j].weave_kind;
			int cbCenterCardIndex = switchToCardIndex(WeaveItem[j].center_card);
			// 要求：组合牌中对应的牌操作为三招，并且手中牌对应索引位置的牌数为1
			if ((cbCardIndex[cbCenterCardIndex] == 1) && (cbWeaveKind == HPGameConstans.WIK_SAN_ZHAO)) {
				WuTongResult.cbCardData[WuTongResult.cbCardCount++] = switchToCardData(cbCenterCardIndex);
				cbActionMask |= HPGameConstans.WIK_WU_TONG;
			}
		}
		return cbActionMask;
	}

	// 给定一个操作类型和操作中心牌索引，得到三张牌的索引,返回操作索引的个数
	public int getWeaveIndex(int cbWeaveKind, int cbCenterCardIndex, int cbCardIndex[]) {
		// 组合扑克
		switch (cbWeaveKind) {
		case HPGameConstans.WIK_DUI: // 对牌操作
		{
			// 设置变量
			cbCardIndex[0] = cbCenterCardIndex;
			cbCardIndex[1] = cbCenterCardIndex;
			cbCardIndex[2] = cbCenterCardIndex;

			return 3;
		}
		case HPGameConstans.WIK_QI_TA: // 三连情况
		{
			// 0--9之间的数字牌组合
			if (cbCenterCardIndex < (10 - 2)) {
				cbCardIndex[0] = cbCenterCardIndex;
				cbCardIndex[1] = cbCenterCardIndex + 1;
				cbCardIndex[2] = cbCenterCardIndex + 2;
			}
			// 孔乙已:孔
			if (cbCenterCardIndex == 10) {
				cbCardIndex[0] = cbCenterCardIndex;
				cbCardIndex[1] = 0;
				cbCardIndex[2] = cbCenterCardIndex + 1;
			}
			// 上大人:上
			if (cbCenterCardIndex == 12) {
				cbCardIndex[0] = cbCenterCardIndex;
				cbCardIndex[1] = cbCenterCardIndex + 1;
				cbCardIndex[2] = cbCenterCardIndex + 2;
			}
			// 七十土:土
			if (cbCenterCardIndex == 15) {
				cbCardIndex[0] = 6;
				cbCardIndex[1] = 9;
				cbCardIndex[2] = cbCenterCardIndex;
			}
			// 化三千:化
			if (cbCenterCardIndex == 16) {
				cbCardIndex[0] = cbCenterCardIndex;
				cbCardIndex[1] = 2;
				cbCardIndex[2] = cbCenterCardIndex + 1;
			}
			// 可知礼
			if (cbCenterCardIndex == 18) {
				cbCardIndex[0] = (cbCenterCardIndex);
				cbCardIndex[1] = (cbCenterCardIndex + 1);
				cbCardIndex[2] = (cbCenterCardIndex + 2);
			}
			// 八九子
			if (cbCenterCardIndex == 21) {
				cbCardIndex[0] = (7);
				cbCardIndex[1] = (8);
				cbCardIndex[2] = (cbCenterCardIndex);
			}

			return 3;
		}
		case HPGameConstans.WIK_SI_ZHAO: // 四招操作
		{
			// 设置变量
			cbCardIndex[0] = cbCenterCardIndex;
			cbCardIndex[1] = cbCenterCardIndex;
			cbCardIndex[2] = cbCenterCardIndex;
			cbCardIndex[3] = cbCenterCardIndex;
			cbCardIndex[4] = cbCenterCardIndex;

			return 5;
		}
		case HPGameConstans.WIK_SAN_ZHAO: // 三招操作
		{
			// 设置变量
			cbCardIndex[0] = cbCenterCardIndex;
			cbCardIndex[1] = cbCenterCardIndex;
			cbCardIndex[2] = cbCenterCardIndex;
			cbCardIndex[3] = cbCenterCardIndex;

			return 4;
		}
		default: {

		}
		}

		return 0;
	}

	// 校正胡点,传进一个牌数据，和一个牌索引
	boolean checkHuPoint(int cbCardData, int cbCardIndex) {
		// 转换扑克数据
		if (cbCardData != 0) {
			if (switchToCardData(cbCardData) == cbCardIndex)
				return true;
			return false;
		}
		return false;
	}

	// //分析胡牌
	// BYTE CGameLogic::AnalyseHuCard(BYTE cbCardIndex[MAX_INDEX], tagWeaveItem
	// WeaveItem[], BYTE cbItemCount,BYTE cbCurrentCard,BYTE HuaCardInex[], BYTE
	// WeavHuaIndex[], tagHuCardResult & ChiHuResult)
	// {
	// CAnalyseItemArray AnalyseItemArray;
	// //设置变量
	// AnalyseItemArray.RemoveAll();
	// ZeroMemory(&ChiHuResult,sizeof(ChiHuResult));
	//
	// //定义临时数组保存手中牌索引
	// BYTE cbCardIndexTemp[MAX_INDEX];
	// CopyMemory(cbCardIndexTemp,cbCardIndex,sizeof(cbCardIndexTemp));
	//
	// //定义变量保存手中花牌的个数
	// BYTE cbTempHuaCardIndex[5];
	// BYTE cbTempWeaveHuaIndex[5];
	// CopyMemory(cbTempHuaCardIndex,HuaCardInex,sizeof(cbTempHuaCardIndex));
	// CopyMemory(cbTempWeaveHuaIndex,WeavHuaIndex,sizeof(cbTempWeaveHuaIndex));
	//
	// //将当前牌加入手中牌索引数组中
	// if (cbCurrentCard != 0)
	// {
	// BYTE CardIndex = SwitchToCardIndex(cbCurrentCard);
	// cbCardIndexTemp[CardIndex]++;
	//
	// //如果当前牌是花牌，保存到手中花牌索引数组中
	// if ((cbCurrentCard & 0xF0) == 0x10) cbTempHuaCardIndex[CardIndex/2]++;
	// }
	//
	// //得到手中各经牌的个数
	// BYTE cbHandGoldCard[5];
	// for (BYTE i=0; i<5; i++)
	// {
	// cbHandGoldCard[i] = cbCardIndexTemp[i*2];
	// }
	//
	// //#ifdef _DEBUG
	//// m_Debug.PrintCardMessage(cbCardIndex);
	//// m_Debug.PrintWeaveItemsMessage(WeaveItem,cbItemCount);
	// //#endif
	//
	// AnalyseCard(cbCardIndexTemp, WeaveItem, cbItemCount, AnalyseItemArray);
	//
	// //对胡牌组合进行分析，得出最佳的胡牌组合
	// if (AnalyseItemArray.GetCount()>0)
	// {
	// //用动态数组
	// tagAnalyseItem BestAnalyseItem;
	// ZeroMemory(&BestAnalyseItem,sizeof(BestAnalyseItem));
	//
	// //逐一分析每一合法胡牌的组合
	// for (INT_PTR i=0;i<AnalyseItemArray.GetCount();i++)
	// {
	// //对某一种组合进行分析
	// tagAnalyseItem *pAnalyseItem=&AnalyseItemArray[i];
	//
	// BYTE cbMaxPoint = 0;
	// BYTE cbMaxPointGoldCard = 255;
	// //对每一种组合分别讨论以1,3,5,7,9作为主金得出胡数最大的情况
	// for (BYTE j=0; j<5; j++)
	// {
	// //得到主经的索引值
	// BYTE RealGold = j*2; //当前的假设主精
	// WORD WeavePoint=0; //用户组合牌的总点数
	// WORD HandPoint=0; //用户手中的总点数
	//
	// //每次重新保存手中经牌和花牌的个数，
	// BYTE cbGoldCard[5]; //用户手中牌的各精牌个数
	// BYTE cbHuaCardIndex[5]; //用户手中牌的各花牌个数
	// CopyMemory(cbGoldCard,cbHandGoldCard,sizeof(cbGoldCard));
	// CopyMemory(cbHuaCardIndex,cbTempHuaCardIndex,sizeof(cbHuaCardIndex));
	//
	// //1 分析用户组合牌中的分数
	// //////////////////////下面用到的花牌索引应该是组合牌中的花牌///////////////////
	// BYTE cbItemPoint; //测试时用，用于记录每一组成的点数
	// ZeroMemory(pAnalyseItem->cbKindPoint,sizeof(pAnalyseItem->cbKindPoint));
	// for(BYTE m=0;m<cbItemCount;m++)
	// {
	// //得到组合牌的中心牌索引
	// cbItemPoint=0;
	// BYTE index = pAnalyseItem->cbCenterCard[m];
	// //四招情况:1.是金牌,2.普通牌
	// if (pAnalyseItem->cbWeaveKind[m]== WIK_SI_ZHAO)
	// {
	// //1.是经牌1,3,5,7,9,分为:1.1主经,1.2非主经
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	//
	// //1.是主金牌+56
	// if (index == RealGold)
	// {
	// cbItemPoint = 56;
	// WeavePoint += 56;
	// }
	// //不是主金+28
	// else
	// {
	// cbItemPoint = 28;
	// WeavePoint += 28;
	// }
	//
	// }
	// //2.不是经牌的普通牌分:2.1红牌,2.2黑牌
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+8
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// WeavePoint += 8;
	// cbItemPoint = 8;
	// }
	// //2.2黑牌+4
	// else
	// {
	// WeavePoint += 4;
	// cbItemPoint = 4;
	// }
	// }
	// }
	// //三招情况:1.是金牌,2.普通牌
	// else if (pAnalyseItem->cbWeaveKind[m] == WIK_SAN_ZHAO)
	// {
	// //1,3,5,7,9的情况
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	// //1.是主金牌根据花牌个数进行算分
	// if (index == RealGold)
	// {
	// //根据花牌个数进行计算
	// switch(cbTempWeaveHuaIndex[index/2])
	// {
	// case 1: //一张花牌的情况
	// {
	// cbItemPoint = 24;
	// WeavePoint += 24;
	// break;
	// }
	// case 2: //两张花牌情况
	// {
	// cbItemPoint = 28;
	// WeavePoint += 28;
	// break;
	// }
	// }
	// }
	// //非主金的牌,根据花牌的个数进行算分
	// else
	// {
	// //根据花牌个数进行计算
	// switch(cbTempWeaveHuaIndex[index/2])
	// {
	// case 1: //一张花牌的情况
	// {
	// cbItemPoint = 12;
	// WeavePoint += 12;
	// break;
	// }
	// case 2: //两张花牌情况
	// {
	// cbItemPoint = 14;
	// WeavePoint += 14;
	// break;
	// }
	// }
	// }
	//
	// }
	// //普通牌2.1红牌算4胡,2.2黑牌算2胡
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+4
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// WeavePoint += 4;
	// cbItemPoint = 4;
	// }
	// //2.2黑牌+2
	// else
	// {
	// WeavePoint += 2;
	// cbItemPoint = 2;
	// }
	// }
	// }
	// //对牌情况:1.是金牌,2.普通牌
	// else if (pAnalyseItem->cbWeaveKind[m] == WIK_DUI)
	// {
	// //1,3,5,7,9的情况
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	// //1.是主金牌根据花牌个数进行算分
	// if (index == RealGold)
	// {
	// //根据花牌个数进行计算
	// switch(cbTempWeaveHuaIndex[index/2])
	// {
	// case 0: //0张花牌的情况
	// {
	// cbItemPoint = 6;
	// WeavePoint += 6;
	// break;
	// }
	// case 1: //1张花牌情况
	// {
	// cbItemPoint = 8;
	// WeavePoint += 8;
	// break;
	// }
	// case 2: //2张花牌情况
	// {
	// cbItemPoint = 10;
	// WeavePoint += 10;
	// break;
	// }
	// }
	// }
	// //////////////非主金的情况下算胡///////////////////
	// //2.非主金的牌
	// else
	// {
	// //根据花牌个数进行计算
	// switch(cbTempWeaveHuaIndex[index/2])
	// {
	// case 0: //0张花牌的情况
	// {
	// cbItemPoint = 3;
	// WeavePoint += 3;
	//
	// break;
	// }
	// case 1: //1张花牌情况
	// {
	// cbItemPoint = 4;
	// WeavePoint += 4;
	// break;
	// }
	// case 2: //2张花牌情况
	// {
	// cbItemPoint = 5;
	// WeavePoint += 5;
	// break;
	// }
	// }
	// }
	//
	// }
	// //普通牌2.1红牌算1胡,2.2黑牌不算胡
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+1
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// WeavePoint += 1;
	// cbItemPoint = 1;
	// }
	// //2.2黑牌+0
	// else
	// {
	// WeavePoint += 0;
	// cbItemPoint = 0;
	// }
	// }
	// }
	//
	// pAnalyseItem->cbKindPoint[m] = cbItemPoint;
	// }
	//
	// //2 计算用户手中坎牌的所有点数
	// for(BYTE k=cbItemCount; k<8;k++)
	// {
	// cbItemPoint=0;
	// //取每种组合里面的每小组的中心牌索引
	// BYTE index = pAnalyseItem->cbCenterCard[k];
	//
	// //五藏情况:1.1是金牌,1.2.普通牌
	// if (pAnalyseItem->cbWeaveKind[k] == WIK_WU_TONG)
	// {
	// //1.是金牌1,3,5,7,9,分为:1.1主金,1.2非主金
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	// cbHuaCardIndex[index/2] = 0; //手中此花牌个数为0
	// cbGoldCard[index/2] -= 5; //手中此精牌个数为0
	//
	// //1.1主金
	// if (index == RealGold)
	// {
	// //对应位置的花牌数减为0
	// HandPoint += 56;
	// cbItemPoint = 56;
	// }
	// //1.2非主金的金牌
	// else
	// {
	// //对应位置的花牌数减为0
	// HandPoint += 28;
	// cbItemPoint = 28;
	// }
	//
	// }
	// //2.普通牌(手中牌):红牌算8胡，黑牌算4胡
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+8
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// HandPoint += 8;
	// cbItemPoint = 8;
	// }
	// //2.2黑牌+4
	// else
	// {
	// HandPoint += 4;
	// cbItemPoint = 4;
	// }
	// }
	// }
	// //四藏情况:1.是金牌,2.普通牌
	// else if (pAnalyseItem->cbWeaveKind[k] == WIK_SI_TONG)
	// {
	// //1.是金牌1,3,5,7,9的情况
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	//
	// cbGoldCard[index/2] -= 4;
	// //1.是主金牌,根据花牌个数进行算分
	// if (index == RealGold)
	// {
	// //根据花牌个数进行计算
	// switch(cbHuaCardIndex[index/2])
	// {
	// case 1: //一张花牌的情况
	// {
	// cbItemPoint = 24;
	// HandPoint += 24;
	// cbHuaCardIndex[index/2] -= 1;
	//
	// break;
	// }
	// case 2: //两张花牌情况
	// {
	// cbItemPoint = 28;
	// HandPoint += 28;
	// cbHuaCardIndex[index/2] -= 2;
	// break;
	// }
	// }
	// }
	// //非主金的牌
	// else
	// {
	// //根据花牌个数进行计算
	// switch(cbHuaCardIndex[index/2])
	// {
	// case 1: //一张花牌的情况
	// {
	// cbItemPoint = 12;
	// HandPoint += 12;
	// cbHuaCardIndex[index/2] -= 1;
	//
	// break;
	// }
	// case 2: //两张花牌情况
	// {
	// cbItemPoint = 14;
	// HandPoint += 14;
	// cbHuaCardIndex[index/2] -= 2;
	// break;
	// }
	// }
	//
	// }
	//
	// }
	// //普通牌2.1红牌算4胡,2.2黑牌算2胡
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+4
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// cbItemPoint = 4;
	// HandPoint += 4;
	// }
	// //2.2黑牌+2
	// else
	// {
	// cbItemPoint = 2;
	// HandPoint += 2;
	// }
	// }
	// }
	// //三张相同的：1.是金牌,2.普通牌
	// else if (pAnalyseItem->cbWeaveKind[k] == WIK_DUI)
	// {
	// //是金牌:1,3,5,7,9的情况
	// if( index == 0 || index == 2 || index == 4 || index == 6 || index == 8)
	// {
	//
	//
	// cbGoldCard[index/2] -= 3;
	// //1.是主金牌,根据花牌个数进行算分
	// if (index == RealGold)
	// {
	// //根据花牌个数进行计算
	// switch(cbHuaCardIndex[index/2])
	// {
	// case 0: //0张花牌的情况
	// {
	// cbItemPoint = 10;
	// HandPoint += 10;
	// cbHuaCardIndex[index/2] -= 0;
	//
	// break;
	// }
	// case 1: //1张花牌情况
	// {
	// cbItemPoint = 12;
	// HandPoint += 12;
	// cbHuaCardIndex[index/2] -= 1;
	// break;
	// }
	// case 2: //2张花牌情况
	// {
	// cbItemPoint = 14;
	// HandPoint += 14;
	// cbHuaCardIndex[index/2] -= 2;
	// break;
	// }
	// }
	// }
	// //1.2非主金的花牌
	// else
	// {
	// //根据花牌个数进行计算
	// switch(cbHuaCardIndex[index/2])
	// {
	// case 0: //0张花牌的情况
	// {
	// cbItemPoint = 5;
	// HandPoint += 5;
	// cbHuaCardIndex[index/2] -= 0;
	//
	// break;
	// }
	// case 1: //1张花牌情况
	// {
	// cbItemPoint = 6;
	// HandPoint += 6;
	// cbHuaCardIndex[index/2] -= 1;
	// break;
	// }
	// case 2: //2张花牌情况
	// {
	// cbItemPoint = 7;
	// HandPoint += 7;
	// cbHuaCardIndex[index/2] -= 2;
	// break;
	// }
	// }
	// }
	//
	// }
	// //2.普通牌
	// else
	// {
	// //2.1红牌,只有上,大,人,可,知,礼六种情况+2
	// if (index == 12 || index == 13 || index == 14 ||index == 18 || index ==
	// 19 || index == 20 )
	// {
	// HandPoint += 2;
	// cbItemPoint = 2;
	// }
	// //2.2黑牌+1
	// else
	// {
	// HandPoint += 1;
	// cbItemPoint = 1;
	// }
	// }
	// }
	//
	// //句子判断,只统计上大人，可知礼的个数和剩下的经牌个数
	// else if (pAnalyseItem->cbWeaveKind[k] == WIK_QI_TA)
	// {
	// //上大人/可知礼组合 +1
	// if (index == 12 || index == 18)
	// {
	// HandPoint += 1;
	// cbItemPoint = 1;
	// }
	// }
	// pAnalyseItem->cbKindPoint[k] = cbItemPoint;
	// }
	//
	// //3 数剩下的金牌的个数
	// for (BYTE t=0; t<5; t++)
	// {
	// //计算花牌的点数
	// if (cbHuaCardIndex[t] > 0)
	// {
	// //除去经牌中花牌的个数
	// cbGoldCard[t] -= cbHuaCardIndex[t];
	// //主金情况
	// if ((t*2) == RealGold)
	// {
	// HandPoint += cbHuaCardIndex[t]*4;
	// }
	// else
	// {
	// HandPoint += cbHuaCardIndex[t]*2;
	// }
	// }
	// //计算白皮经牌的点数
	// if (cbGoldCard[t] > 0)
	// {
	// //主金情况白皮的*2
	// if ((t*2) == RealGold)
	// {
	// HandPoint += cbGoldCard[t]*2;
	// }
	// //非主金,白皮的*1
	// else
	// {
	// HandPoint += cbGoldCard[t]*1;
	// }
	// }
	// }
	// //#ifdef _DEBUG
	//// m_Debug.PrintValidKindItemPointBeforeMessage(pAnalyseItem,WeavePoint,HandPoint);
	// //#endif
	//
	// //校正手中牌的胡点:如果胡牌组合中，别人打出的一张牌与自己手中牌形成三张一样的，只能将其当作倒下的牌算点数
	// //将手中牌每一种组合进行转换，判断所供牌是否在其中
	// if (cbCurrentCard != 0)
	// {
	// //得到所供的牌
	// BYTE cbCurrentCardIndex = SwitchToCardIndex(cbCurrentCard);
	// //首先是丫口判断,如果所供的牌在丫口中，不进行校正
	// if ((cbCurrentCardIndex != pAnalyseItem->cbCardEye[0]) &&
	// (cbCurrentCardIndex != pAnalyseItem->cbCardEye[1]))
	// {
	// bool Qi_Ta = false;
	// //分析别人出的牌是否在句牌中
	// for (BYTE l=cbItemCount; l<8; l++)
	// {
	// //得到WIK_QI_TA组合类型的所有索引，判断是否有所供的牌
	// if (pAnalyseItem->cbWeaveKind[l] == WIK_QI_TA)
	// {
	// BYTE cbWeaveIndex[5]={-1,-1,-1,-1,-1};
	// BYTE cbWeaveCount =
	// GetWeaveIndex(pAnalyseItem->cbWeaveKind[l],pAnalyseItem->cbCenterCard[l],cbWeaveIndex);
	// //判断所供的牌是否在WIK_QI_TA类型中，如果在，直接跳出不用校正
	// for (BYTE n=0; n<cbWeaveCount; n++)
	// {
	// if (cbCurrentCardIndex == cbWeaveIndex[n])
	// {
	// Qi_Ta = true;
	// break;
	// }
	// }
	//
	// //如果在句牌中找到了别人所打的牌跳出循环
	// if (Qi_Ta == true) break;
	// }
	// //得到WIK_SI_TONG组合类型中的所有索引，判断所供的牌是否是在其中，如果是，不能胡牌
	// else if (pAnalyseItem->cbWeaveKind[l] == WIK_SI_TONG ||
	// pAnalyseItem->cbWeaveKind[l] == WIK_WU_TONG)
	// {
	// if (cbCurrentCardIndex == pAnalyseItem->cbCenterCard[i])
	// {
	// //AfxMessageBox("对不起！最佳胡牌组合分析得出，别人打出的牌形成四张的不能胡牌!");
	// return WIK_NULL;
	// }
	// }
	//
	// }
	//
	// //如果在句牌中没有找到别人打的牌，再到对牌中找
	// if (Qi_Ta == false)
	// {
	// for (BYTE i=cbItemCount; i<8; i++)
	// {
	// //WIK_DUI
	// if (pAnalyseItem->cbWeaveKind[i] == WIK_DUI)
	// {
	// //如果手中对牌与所供牌相同，进行胡点校正
	// if (cbCurrentCardIndex == pAnalyseItem->cbCenterCard[i])
	// {
	// //如果所供牌是金牌分：1.主金，2.普通金牌
	// if (cbCurrentCardIndex == 0 || cbCurrentCardIndex == 2 ||
	// cbCurrentCardIndex == 4 || cbCurrentCardIndex == 6 || cbCurrentCardIndex
	// == 8)
	// {
	// //1.主金减4胡
	// if (cbCurrentCardIndex == RealGold)
	// {
	// HandPoint -= 4; //HuPoint-=4;
	// pAnalyseItem->cbKindPoint[i] -=4;
	// }
	// //2.普通金牌减2胡
	// else
	// {
	// HandPoint -= 2; //HuPoint -=2;
	// pAnalyseItem->cbKindPoint[i] -=2;
	// }
	// }
	// //普通牌分,总胡数减1
	// else
	// {
	// HandPoint -= 1;
	// pAnalyseItem->cbKindPoint[i] -=1;
	// //HuPoint -=1;
	// }
	// //处理完成后，结束循环
	// break;
	// }
	// }
	//
	// }
	// }
	// }
	// }
	//
	//
	//
	// //以该经牌做为主经是否大于前面的点数
	// //保存本组合中最大的点数及其对应的主精牌
	// if((WeavePoint+HandPoint)>cbMaxPoint)
	// {
	// cbMaxPoint=WeavePoint+HandPoint;
	// cbMaxPointGoldCard=RealGold;
	// }
	// //先设置本循环中计算的点数及主精
	// pAnalyseItem->cbPoint = WeavePoint+HandPoint;
	// pAnalyseItem->cbGoldCard = RealGold;
	// #ifdef _DEBUG
	// m_Debug.PrintValidKindItemPointAfterMessage(pAnalyseItem,(BYTE)WeavePoint,(BYTE)HandPoint);
	// #endif
	// }
	// //修正：设置本组合中真正最大的点数及其对应的主精牌
	// pAnalyseItem->cbPoint = cbMaxPoint;
	// pAnalyseItem->cbGoldCard = cbMaxPointGoldCard;
	// }
	//
	//
	// //从所有的胡牌组合中找出胡点最大的组合
	// INT_PTR BestIndex = 0;
	// //对每一个胡牌组合进行比较，找出最佳胡牌组合
	// for (INT_PTR i=0;i<AnalyseItemArray.GetCount();i++)
	// {
	// if (AnalyseItemArray[i].cbPoint > AnalyseItemArray[BestIndex].cbPoint)
	// {
	// BestIndex = i;
	// }
	// }
	//
	// BestAnalyseItem.cbPoint = AnalyseItemArray[BestIndex].cbPoint;
	// BestAnalyseItem.cbGoldCard = AnalyseItemArray[BestIndex].cbGoldCard;
	// //最好保存该种情况下的胡牌组合,以便别人算分
	// CopyMemory(&BestAnalyseItem
	// ,&AnalyseItemArray[BestIndex],sizeof(tagAnalyseItem));
	//
	// if (BestAnalyseItem.cbPoint < 17)
	// {
	// return WIK_NULL;
	// }
	//
	// ChiHuResult.IsHu=true;
	// ChiHuResult.HuScore= 2 + ((BestAnalyseItem.cbPoint-17)/5)*2;
	// ChiHuResult.bHuPoint = BestAnalyseItem.cbPoint; //胡牌点数
	// ChiHuResult.bRealGold = BestAnalyseItem.cbGoldCard;
	// CopyMemory(&ChiHuResult.AnalyseItem,
	// &BestAnalyseItem,sizeof(BestAnalyseItem));
	//
	// #ifdef _DEBUG
	// m_Debug.PrintHuCardMessage(&BestAnalyseItem);
	// #endif
	// return WIK_CHI_HU;
	// }
	// return WIK_NULL;
	//
	// }

	// 扑克转换成牌数据
	public int switchToCardData(int cbCardIndex) {
		// ASSERT(cbCardIndex<34);
		// return ((cbCardIndex/9)<<4)|(cbCardIndex%9+1);
		assert ((cbCardIndex >= 0) && (cbCardIndex < 22));

		// BYTE CardData;
		if ((cbCardIndex >= 0) && (cbCardIndex < 10)) {
			// CardData=((cbCardIndex/10)<<4)|((cbCardIndex%10)+1);
			return 0x00 | (cbCardIndex + 1);
		} else {
			return 0x20 | (cbCardIndex - 9);

			// CardData=(2<<4)|((cbCardIndex%12)+1);
		}
		// return CardData;
	}

	// 扑克数据转换成扑克点数转换的结果：
	// 万， 条， 筒
	// 0-8 9-15 16-23
	public int switchToCardIndex(int cbCardData) {

		assert (is_valid_card(cbCardData));

		int cbValue = (cbCardData & HPGameConstans.LOGIC_MASK_VALUE);
		int cbColor = (cbCardData & HPGameConstans.LOGIC_MASK_COLOR) >> 4;

		// 如果是数字，直接得到最后一位值
		if (cbColor == 0 || cbColor == 1)
			return cbValue - 1;
		else
			return cbValue + 9;
	}

	//// 扑克转换,
	// 扑克转换, BYTE cbCardIndex[MAX_INDEX] BYTE cbCardData[MAX_COUNT]
	public int switchToCardData(int cbCardIndex[], int cbCardData[], int cbHuaCardIndex[]) {
		// 转换扑克
		int cbPosition = 0;
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			if (cbCardIndex[i] != 0) {
				assert (cbPosition < HPGameConstans.MAX_COUNT);
				// 如果是经牌，特殊处理
				if (i == 0 || i == 2 || i == 4 || i == 6 || i == 8) {
					// 将花牌数组中花牌个数保存到牌数据中
					for (int k = 0; k < cbHuaCardIndex[i / 2]; k++) {
						cbCardData[cbPosition++] = 0x10 | (i + 1);
					}
					// 将剩下的画白皮的
					for (int j = 0; j < (cbCardIndex[i] - cbHuaCardIndex[i / 2]); j++) {
						cbCardData[cbPosition++] = 0x00 | (i + 1);
					}
				}
				// 普通牌处理
				else {
					for (int j = 0; j < cbCardIndex[i]; j++) {
						cbCardData[cbPosition++] = switchToCardData(i);
					}
				}
			}
		}

		return cbPosition;
	}

	// 扑克转换 cbCardIndex[MAX_INDEX]
	public int switchToCardIndex(int cbCardData[], int cbCardCount, int cbCardIndex[]) {
		// 设置变量
		// ZeroMemory(cbCardIndex,sizeof(BYTE)*MAX_INDEX);

		// 转换扑克
		for (int i = 0; i < cbCardCount; i++) {
			assert (is_valid_card(cbCardData[i]));

			cbCardIndex[switchToCardIndex(cbCardData[i])]++;
		}

		return cbCardCount;
	}

	// 求绝对值
	public int abs(int cbFirst, int cbSecond) {
		return (cbFirst > cbSecond) ? (cbFirst - cbSecond) : (cbSecond - cbFirst);

	}

	// 判断加入了待分析的牌后是否构成丫口
	public boolean isYaKou(int cbCardIndex[], int firstIndex, int secondeIndex) {
		int[] cbCardIndexTemp = new int[HPGameConstans.MAX_INDEX];
		// ZeroMemory(cbCardIndexTemp,sizeof(cbCardIndexTemp));

		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			cbCardIndexTemp[i] = cbCardIndex[i];
		}
		// CopyMemory(cbCardIndexTemp,cbCardIndex,sizeof(cbCardIndexTemp));

		// 统计数组中的个数要为2
		int cbCardCount = 0;
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++)
			cbCardCount += cbCardIndexTemp[i];

		String sz;
		// sz.Format("剩下的牌个数为:%d",cbCardCount);
		// AfxMessageBox(sz);

		if (cbCardCount != 2)
			return false;

		// ASSERT(cbCardCount == 2);
		// if (cbCardCount != 2) return false;

		// BYTE firstIndex = -1,secondeIndex = -1;

		// 丫口为半句话
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] != 0) {
				firstIndex = i;
				cbCardIndexTemp[i]--;
				break;
			}
		}
		// 找下一张牌
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
			if (cbCardIndexTemp[i] != 0) {
				secondeIndex = i;
				cbCardIndexTemp[i]--;
				break;
			}
		}

		// CString sz;
		// sz.Format("第一个索引为：%d,第二个索引为:%d",firstIndex,secondeIndex);
		// AfxMessageBox(sz);

		// 判断是不是丫口
		// 1.如果两张牌都是数字的情况下，相减绝对值必须<=2 单独考虑七，十，
		if (firstIndex < 10 && secondeIndex < 10) {
			// 特例:七，十
			if ((firstIndex == 6 && secondeIndex == 9) || (firstIndex == 9 && secondeIndex == 6))
				return true;

			// 其他情况按数字组合算,相减绝对值必须<=2
			if (abs(firstIndex, secondeIndex) <= 2)
				return true;
		} else {
			// 2.有数字和字的情况下
			// 孔,乙，己
			if ((firstIndex == 10) && (secondeIndex == 10 || secondeIndex == 0 || secondeIndex == 11))
				return true;
			if ((firstIndex == 0) && (secondeIndex == 10 || secondeIndex == 0 || secondeIndex == 11))
				return true;
			if ((firstIndex == 11) && (secondeIndex == 10 || secondeIndex == 0 || secondeIndex == 11))
				return true;

			// 上大人
			if ((firstIndex == 12) && (secondeIndex == 12 || secondeIndex == 13 || secondeIndex == 14))
				return true;
			if ((firstIndex == 13) && (secondeIndex == 12 || secondeIndex == 13 || secondeIndex == 14))
				return true;
			if ((firstIndex == 14) && (secondeIndex == 12 || secondeIndex == 13 || secondeIndex == 14))
				return true;

			// 化,三,千
			if ((firstIndex == 16) && (secondeIndex == 16 || secondeIndex == 2 || secondeIndex == 17))
				return true;
			if ((firstIndex == 2) && (secondeIndex == 16 || secondeIndex == 2 || secondeIndex == 17))
				return true;
			if ((firstIndex == 17) && (secondeIndex == 16 || secondeIndex == 2 || secondeIndex == 17))
				return true;

			// 七十土
			if ((firstIndex == 6) && (secondeIndex == 6 || secondeIndex == 9 || secondeIndex == 15))
				return true;
			if ((firstIndex == 9) && (secondeIndex == 6 || secondeIndex == 9 || secondeIndex == 15))
				return true;
			if ((firstIndex == 15) && (secondeIndex == 6 || secondeIndex == 9 || secondeIndex == 15))
				return true;

			// 八九子
			if ((firstIndex == 7) && (secondeIndex == 7 || secondeIndex == 8 || secondeIndex == 21))
				return true;
			if ((firstIndex == 8) && (secondeIndex == 7 || secondeIndex == 8 || secondeIndex == 21))
				return true;
			if ((firstIndex == 21) && (secondeIndex == 7 || secondeIndex == 8 || secondeIndex == 21))
				return true;

			// 可知礼
			if ((firstIndex == 18) && (secondeIndex == 18 || secondeIndex == 19 || secondeIndex == 20))
				return true;
			if ((firstIndex == 19) && (secondeIndex == 18 || secondeIndex == 19 || secondeIndex == 20))
				return true;
			if ((firstIndex == 20) && (secondeIndex == 18 || secondeIndex == 19 || secondeIndex == 20))
				return true;
		}

		return false;
	}

	// 分析用户手中的牌，得到可能胡牌的组合信息//用户手中牌的索引数组 //组合牌的数组 //组合个数 //胡牌组合数组 int
	// cbCardIndex[MAX_INDEX] , CAnalyseItemArray & AnalyseItemArray
	public boolean analyseCard(int cardIndex[], WeaveItem weaveItem[], int cbWeaveCount,
			List<AnalyseItemHP> analyseItemArray) {

		// 统计索引数组中的所有牌数
		int cbCardCount = 0;
		for (int i = 0; i < HPGameConstans.MAX_INDEX; i++)
			cbCardCount += cardIndex[i];

		// 效验数目，索引数组中牌的总数在最特殊的情况下，都会有一对丫口
		assert (cbCardCount >= 2);
		if (cbCardCount < 2)
			return false;

		// 变量定义
		int cbKindItemCount = 0;
		KindItem kindItem[] = new KindItem[HPGameConstans.MAX_COUNT - 2];
		for (int i = 0; i < kindItem.length; i++) {
			kindItem[i] = new KindItem();
		}

		// 需求判断--用户手中胡牌的正确组合数
		int cbLessKindItem = 8 - cbWeaveCount;

		// 单吊判断，cbCardCount=2的情况：原来手中牌只有一张牌，加入要分析的牌后正好构成两张，其他的牌都在组合牌中
		if (cbCardCount == 2) {
			int firstIndex = -1, secondeIndex = -1;
			// 如果剩下的两张是丫口，保存所有组合牌
			if (isYaKou(cardIndex, firstIndex, secondeIndex) == true) {
				AnalyseItemHP analyseItem = new AnalyseItemHP();
				// ZeroMemory(&AnalyseItem,sizeof(AnalyseItem));

				// 分析每一组组合牌，得到组合牌的组合牌型和中间牌索引,保存到分析子项中
				for (int j = 0; j < cbWeaveCount; j++) {
					analyseItem.cbWeaveKind[j] = weaveItem[j].weave_kind;
					analyseItem.cbCenterCard[j] = switchToCardIndex(weaveItem[j].center_card);
				}
				// 保存丫口
				analyseItem.cbCardEye[0] = firstIndex;
				analyseItem.cbCardEye[1] = secondeIndex;
				// 将分析结果插入到分析数组中
				analyseItemArray.add(analyseItem);

				return true;
			}
			return false;
		}

		// 加入待分析的牌后，手中牌>=3的情况，对手中牌索引数组进行分析
		if (cbCardCount >= 3) {
			for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
				// 三个一样的牌
				if (cardIndex[i] >= 3) {
					kindItem[cbKindItemCount].cbCenterCard = i;
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;

					kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_DUI;
				}
				// 四个一样
				if (cardIndex[i] >= 4) {
					kindItem[cbKindItemCount].cbCenterCard = i;
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbCardIndex[3] = i;

					kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_SI_TONG;
				}
				// 五个一样
				if (cardIndex[i] == 5) {
					kindItem[cbKindItemCount].cbCenterCard = i;
					kindItem[cbKindItemCount].cbCardIndex[0] = i;
					kindItem[cbKindItemCount].cbCardIndex[1] = i;
					kindItem[cbKindItemCount].cbCardIndex[2] = i;
					kindItem[cbKindItemCount].cbCardIndex[3] = i;
					kindItem[cbKindItemCount].cbCardIndex[4] = i;
					kindItem[cbKindItemCount].cbCardCount = 5;

					kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_WU_TONG;
				}

				// 2.连牌判断
				// 0--9之间的排序
				if ((i < (10 - 2)) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if ((cardIndex[i + 1] >= j) && (cardIndex[i + 2] >= j)) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
							kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}
				}
				// 11-21
				// 孔乙已
				else if ((i == 10) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[0] >= j && cardIndex[11] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = 0;
							kindItem[cbKindItemCount].cbCardIndex[2] = 11;
							kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}
				}
				// 上大人
				else if ((i == 12) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[13] >= j && cardIndex[14] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
							kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}
				}
				// 七十土
				else if ((i == 15) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[6] >= j && cardIndex[9] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = 6;
							kindItem[cbKindItemCount].cbCardIndex[1] = 9;
							kindItem[cbKindItemCount].cbCardIndex[2] = i;
							kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}
				}
				// 化三千
				else if ((i == 16) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[2] >= j && cardIndex[17] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = 2;
							kindItem[cbKindItemCount].cbCardIndex[2] = 17;
							// kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}

				}
				// 可知礼
				else if ((i == 18) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[19] >= j && cardIndex[20] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = i;
							kindItem[cbKindItemCount].cbCardIndex[1] = i + 1;
							kindItem[cbKindItemCount].cbCardIndex[2] = i + 2;
							// kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}
				}
				// 八九子
				else if ((i == 21) && (cardIndex[i] > 0)) {
					for (int j = 1; j <= cardIndex[i]; j++) {
						if (cardIndex[7] >= j && cardIndex[8] >= j) {
							kindItem[cbKindItemCount].cbCenterCard = i;
							kindItem[cbKindItemCount].cbCardIndex[0] = 7;
							kindItem[cbKindItemCount].cbCardIndex[1] = 8;
							kindItem[cbKindItemCount].cbCardIndex[2] = i;
							kindItem[cbKindItemCount].cbCardCount = 3;

							kindItem[cbKindItemCount++].cbWeaveKind = HPGameConstans.WIK_QI_TA;
						}
					}

				}
			}
		}

		// 分析所有的组合，从而得到可能胡牌的情况
		if (cbKindItemCount >= cbLessKindItem) {
			// 变量定义
			int cbCardIndexTemp[] = new int[HPGameConstans.MAX_INDEX];

			// 变量定义
			int cbIndex[] = new int[] { 0, 1, 2, 3, 4, 5, 6, 7 };
			KindItem pKindItem[] = new KindItem[8];
			for (int i = 0; i < cbIndex.length; i++) {
				pKindItem[i] = new KindItem();
			}

			do {
				// 每次循环将传进来的牌索引数组拷贝到临时数组中，进行分析
				// 设置变量
				for (int i = 0; i < HPGameConstans.MAX_INDEX; i++) {
					cbCardIndexTemp[i] = cardIndex[i];
				}

				// 每次从上面分析得出的分析子项中取cbLessKindItem个分析子项进行分析，
				// 注意：索引数组cbIndex[]在每次循环结束时都重新设置了
				for (int i = 0; i < cbLessKindItem; i++) {
					// pKindItem[i]=&KindItem[cbIndex[i]];
					pKindItem[i].cbWeaveKind = kindItem[cbIndex[i]].cbWeaveKind;
					pKindItem[i].cbCenterCard = kindItem[cbIndex[i]].cbCenterCard;
					for (int j = 0; j < 5; j++) {
						pKindItem[i].cbCardIndex[j] = kindItem[cbIndex[i]].cbCardIndex[j];
						pKindItem[i].cbValidIndex[j] = kindItem[cbIndex[i]].cbValidIndex[j];
					}
				}

				// 数量判断
				boolean bEnoughCard = true;
				// 修改临时数组的值，把临时数组中构成cbLessKindItem个分析子项里的每一张牌，牌数减1，
				// 以下是都是三个一组的
				for (int k = 0; k < cbLessKindItem; k++) {
					// 对组合里的每一个牌索引进行分析
					for (int i = 0; i < pKindItem[k].cbCardCount; i++) {
						// 存在判断
						int cbCardIndex = pKindItem[k].cbCardIndex[i];

						if (cbCardIndexTemp[cbCardIndex] == 0) {
							bEnoughCard = false;
							break;
						} else
							cbCardIndexTemp[cbCardIndex]--;
					}
				}
				// 胡牌判断，注意下面使用到的cbCardIndexTemp[]数组是经前面修改过后的
				if (bEnoughCard == true) {
					// 统计数组中的个数要为2
//					int cbCardCount = 0;
					int count = 0;
					for (int i = 0; i < HPGameConstans.MAX_INDEX; i++)
						count += cbCardIndexTemp[i];

					// 丫口值
					int cbfirstIndex = -1, cbSecondIndex = -1;
					// 对剩下的牌进行分析，判断是否是丫口，
					if (isYaKou(cbCardIndexTemp, cbfirstIndex, cbSecondIndex) == true) {
						assert ((cbfirstIndex != -1) && (cbSecondIndex != -1));

						AnalyseItemHP analyseItem = new AnalyseItemHP();

						// 得到组合牌中的牌型，保存到分析子项中
						for (int i = 0; i < cbWeaveCount; i++) {
							analyseItem.cbWeaveKind[i] = weaveItem[i].weave_kind;
							analyseItem.cbCenterCard[i] = switchToCardIndex(weaveItem[i].center_card);
						}

						// 得到手中牌的牌型，保存到分析子项中
						for (int i = 0; i < cbLessKindItem; i++) {
							analyseItem.cbWeaveKind[i + cbWeaveCount] = pKindItem[i].cbWeaveKind;
							analyseItem.cbCenterCard[i + cbWeaveCount] = pKindItem[i].cbCenterCard;
						}
						// 设置牌眼
						analyseItem.cbCardEye[0] = cbfirstIndex;
						analyseItem.cbCardEye[1] = cbSecondIndex;

						// 将分析子项插入到分析数组中
						analyseItemArray.add(analyseItem);
					}
				}

				// 设置索引，索引数组中存放的是分析子项数组的下标，每次取分析子项进行分析时，都是按照索引数组
				// 里面存放的下标值进行存取，当cbIndex[cbLessKindItem-1]的最后一位存放的值与得出的分析子项下标相同，
				// 重新调整索引数组，下一次取值就会取新的组合
				if (cbIndex[cbLessKindItem - 1] == (cbKindItemCount - 1)) {
					int i = cbLessKindItem - 1;
					for (; i > 0; i--) {
						if ((cbIndex[i - 1] + 1) != cbIndex[i]) {
							int cbNewIndex = cbIndex[i - 1];
							for (int j = (i - 1); j < cbLessKindItem; j++) {
								cbIndex[j] = cbNewIndex + j - i + 2;
							}
							break;
						}
					}
					// 跳出整个while循环
					if (i == 0)
						break;
				} else
					cbIndex[cbLessKindItem - 1]++;

			} while (true);
		}

		return (analyseItemArray.size() > 0);
	}

}
