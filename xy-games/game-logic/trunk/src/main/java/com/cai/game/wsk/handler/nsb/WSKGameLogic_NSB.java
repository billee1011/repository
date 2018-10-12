package com.cai.game.wsk.handler.nsb;

import com.cai.common.constant.GameConstants;
import com.cai.common.constant.game.wsk.GameConstants_NSB;
import com.cai.game.wsk.WSKGameLogic;

public class WSKGameLogic_NSB extends WSKGameLogic {

	// 获取类型
	public int GetCardType_WSK(int cbCardData[], int cbCardCount) {
		if (cbCardCount > cbCardData.length) {
			return GameConstants_NSB.CT_ERROR;
		}
		// 简单牌型
		switch (cbCardCount) {
		case 0: // 空牌
		{
			return GameConstants_NSB.CT_ERROR;
		}
		case 1: // 单牌
		{
			return GameConstants_NSB.CT_SINGLE;
		}
		case 2: // 两张
		{
			if (GetCardValue(cbCardData[0]) == GetCardValue(cbCardData[1]))
				// if (GetCardValue(cbCardData[0]) != 0x0E &&
				// GetCardValue(cbCardData[0]) != 0x0F)
				return GameConstants_NSB.CT_DOUBLE; // 一般对
			// return GameConstants_NSB.CT_ERROR;
		}
		case 3: // 三牌
		{
			if (cbCardData.length < 3) {
				return GameConstants_NSB.CT_ERROR;
			}
			int cardValue = GetCardValue(cbCardData[0]);
			boolean flag = true;
			for (int i = 0; i < 3; i++) {
				if (cardValue != GetCardValue(cbCardData[i])) {
					flag = false;
					break;
				}
			}
			if (flag)
				return GameConstants_NSB.CT_THREE_NON;

			if (GetCardValue(cbCardData[0]) == 13 && GetCardValue(cbCardData[1]) == 10 && GetCardValue(cbCardData[2]) == 5) {
				if (GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[1]) && GetCardColor(cbCardData[0]) == GetCardColor(cbCardData[2]))
					return GameConstants_NSB.CT_510K_SC; // 同色510K
				return GameConstants_NSB.CT_510K_DC; // 普通510K
			}
			if ((GetCardValue(cbCardData[0]) == 0x0E || GetCardValue(cbCardData[0]) == 0x0F)
					&& (GetCardValue(cbCardData[1]) == 0x0E || GetCardValue(cbCardData[1]) == 0x0F)
					&& (GetCardValue(cbCardData[2]) == 0x0E || GetCardValue(cbCardData[2]) == 0x0F))
				return GameConstants_NSB.CT_THREE_NON; // 3王
		}
		}

		// 分析扑克
		tagAnalyseResult AnalyseResult = new tagAnalyseResult();
		AnalysebCardData(cbCardData, cbCardCount, AnalyseResult);

		// 炸弹类型
		if ((cbCardCount == 4) && (cbCardData[0] == 0x4F) && (cbCardData[3] == 0x4E))
			return GameConstants_NSB.CT_KING_FOUR;// 4王
		if ((cbCardCount == 4) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants_NSB.CT_BOMB_4;
		if ((cbCardCount == 5) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants_NSB.CT_BOMB_5;
		if ((cbCardCount == 6) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants_NSB.CT_BOMB_6;
		if ((cbCardCount == 7) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants_NSB.CT_BOMB_7;
		if ((cbCardCount == 8) && (AnalyseResult.cbBlockCount[cbCardCount - 1] == 1))
			return GameConstants_NSB.CT_BOMB_8;

		// 对连类型
		if (AnalyseResult.cbBlockCount[1] * 2 == cbCardCount) {
			int cbDoubleCount = AnalyseResult.cbBlockCount[1] * 2;
			if (IsStructureLink(AnalyseResult.cbCardData[1], cbDoubleCount, 2))
				return GameConstants_NSB.CT_DOUBLE_LINK;
		}
		// 三连类型
		if (AnalyseResult.cbBlockCount[2] * 3 == cbCardCount) {
			int cbThreeCount = AnalyseResult.cbBlockCount[2] * 3;
			if (IsStructureLink(AnalyseResult.cbCardData[2], cbThreeCount, 3))
				return GameConstants_NSB.CT_THREE_LINK_NON;
		}
		// 三连带翅膀
		if (cbCardCount != 5 && AnalyseResult.cbBlockCount[2] == AnalyseResult.cbBlockCount[1]
				&& AnalyseResult.cbBlockCount[2] * 3 + AnalyseResult.cbBlockCount[1] * 2 == cbCardCount) {
			int cbThreeCount = AnalyseResult.cbBlockCount[2] * 3;
			if (!IsStructureLink(AnalyseResult.cbCardData[2], cbThreeCount, 3))
				return GameConstants_NSB.CT_ERROR;
			int cbDoubleCount = AnalyseResult.cbBlockCount[1] * 2;
			if (!IsStructureLink(AnalyseResult.cbCardData[1], cbDoubleCount, 2))
				return GameConstants_NSB.CT_ERROR;

			return GameConstants_NSB.CT_THREE_LINK;
		}
		// 三张带一对
		if (cbCardCount == 5 && AnalyseResult.cbBlockCount[2] * 3 + AnalyseResult.cbBlockCount[1] * 2 == cbCardCount) {
			return GameConstants_NSB.CT_THREE;
		} else {
			if (cbCardCount == 5 && (GetCardValue(cbCardData[0]) == 0x0E || GetCardValue(cbCardData[0]) == 0x0F)
					&& (GetCardValue(cbCardData[1]) == 0x0E || GetCardValue(cbCardData[1]) == 0x0F)
					&& (GetCardValue(cbCardData[2]) == 0x0E || GetCardValue(cbCardData[2]) == 0x0F)) {
				if (GetCardValue(cbCardData[3]) == GetCardValue(cbCardData[4])) {
					return GameConstants_NSB.CT_THREE;
				} else {
					return GameConstants_NSB.CT_ERROR;
				}
			}
		}

		// 顺子类型
		if ((cbCardCount >= 5) && (AnalyseResult.cbBlockCount[0] == cbCardCount)) {
			// 扑克属性
			int cbSignedCount = AnalyseResult.cbBlockCount[0];
			int cbCardColor = GetCardColor(AnalyseResult.cbCardData[0], cbSignedCount);
			if (IsStructureLink(AnalyseResult.cbCardData[0], cbSignedCount, 1)) {
				if (cbCardColor == 0xF0) {
					return GameConstants_NSB.CT_SHUN_ZI_DC;
				} else {
					return GameConstants_NSB.CT_SHUN_ZI_SC;
				}
			}
		}

		return GameConstants_NSB.CT_ERROR;
	}

	// 对比扑克
	public boolean CompareCard_WSK(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 类型判断
		int cbNextType = GetCardType_WSK(cbNextCard, cbNextCount);
		int cbFirstType = GetCardType_WSK(cbFirstCard, cbFirstCount);

		// 炸弹以上一定大于单牌、对子和单龙,连对
		if (cbNextType >= GameConstants_NSB.CT_510K_DC && cbFirstType <= GameConstants_NSB.CT_SHUN_ZI_DC)
			return true;

		// 炸弹比较
		if (cbFirstType >= GameConstants_NSB.CT_510K_DC && cbNextType >= GameConstants_NSB.CT_510K_DC) {
			if (cbNextType > cbFirstType) {
				return true;
			} else if (cbFirstType == cbNextType) {
				if (cbFirstType == GameConstants_NSB.CT_510K_SC) {
					// 变量定义
					int cbConsultNext = GetCardColor(cbNextCard[0]);
					int cbConsultFirst = GetCardColor(cbFirstCard[0]);

					return cbConsultNext > cbConsultFirst;
				} else {
					SortCardList(cbFirstCard, cbFirstCount, GameConstants.WSK_ST_ORDER);
					SortCardList(cbNextCard, cbNextCount, GameConstants.WSK_ST_ORDER);
					if (GameConstants_NSB.CT_SHUN_ZI_SC == cbNextType && cbNextCount < cbFirstCount) {
						return false;
					}
					if (cbNextCount > cbFirstCount)
						return true;
					// 变量定义
					int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
					int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

					if (cbConsultNext == cbConsultFirst
							&& (GameConstants_NSB.CT_510K_SC == cbNextType || GameConstants_NSB.CT_SHUN_ZI_SC == cbNextType)) {
						return GetCardColor(cbNextCard[0]) > GetCardColor(cbFirstCard[0]);
					}
					return cbConsultNext > cbConsultFirst;
				}

			} else {
				return false;
			}
		}

		if (cbNextCount != cbFirstCount)
			return false;
		if (cbFirstType != cbNextType) {
			return false;
		}

		// 相同类型
		switch (cbFirstType) {
		case GameConstants_NSB.CT_SINGLE: // 单牌类型
		case GameConstants_NSB.CT_DOUBLE: // 对子类型
		case GameConstants_NSB.CT_THREE_NON: // 三张类型,不带翅膀
		{
			// 变量定义
			int cbConsultNext = GetCardLogicValue(cbNextCard[0]);
			int cbConsultFirst = GetCardLogicValue(cbFirstCard[0]);

			return cbConsultNext > cbConsultFirst;
		}
		case GameConstants_NSB.CT_THREE: { // 三张类型
			if ((GetCardValue(cbFirstCard[0]) == 0x0E || GetCardValue(cbFirstCard[0]) == 0x0F)
					&& (GetCardValue(cbFirstCard[1]) == 0x0E || GetCardValue(cbFirstCard[1]) == 0x0F)
					&& (GetCardValue(cbFirstCard[2]) == 0x0E || GetCardValue(cbFirstCard[2]) == 0x0F)) {
				return CompareCardByValue(cbFirstCard, cbNextCard, cbFirstCount, cbNextCount);
			}
			if ((GetCardValue(cbNextCard[0]) == 0x0E || GetCardValue(cbNextCard[0]) == 0x0F)
					&& (GetCardValue(cbNextCard[1]) == 0x0E || GetCardValue(cbNextCard[1]) == 0x0F)
					&& (GetCardValue(cbNextCard[2]) == 0x0E || GetCardValue(cbNextCard[2]) == 0x0F)) {
				return CompareCardByValue(cbFirstCard, cbNextCard, cbFirstCount, cbNextCount);
			}
			// 分析扑克
			tagAnalyseResult firstAnalyseResult = new tagAnalyseResult();
			AnalysebCardData(cbFirstCard, cbFirstCount, firstAnalyseResult);
			tagAnalyseResult nextAnalyseResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, nextAnalyseResult);

			int cbConsultFirst = GetCardLogicValue(firstAnalyseResult.cbCardData[2][0]);
			int cbConsultNext = GetCardLogicValue(nextAnalyseResult.cbCardData[2][0]);
			return cbConsultNext > cbConsultFirst;
		}
		case GameConstants_NSB.CT_SINGLE_LINK: // 单连类型
		case GameConstants_NSB.CT_DOUBLE_LINK: // 对连类型
		case GameConstants_NSB.CT_THREE_LINK_NON: // 三连类型 不带翅膀
		{
			if (cbNextCount != cbFirstCount)
				return false;
			return CompareCardByValue(cbFirstCard, cbNextCard, cbFirstCount, cbNextCount);
		}
		case GameConstants_NSB.CT_SHUN_ZI_DC: // 三连类型 不带翅膀
		{
			return CompareCardByValue(cbFirstCard, cbNextCard, cbFirstCount, cbNextCount);
		}
		case GameConstants_NSB.CT_THREE_LINK: // 三连类型 带翅膀
			if (cbNextCount != cbFirstCount)
				return false;

			// 分析扑克
			tagAnalyseResult firstAnalyseResult = new tagAnalyseResult();
			AnalysebCardData(cbFirstCard, cbFirstCount, firstAnalyseResult);
			tagAnalyseResult nextAnalyseResult = new tagAnalyseResult();
			AnalysebCardData(cbNextCard, cbNextCount, nextAnalyseResult);

			return CompareCardByValue(firstAnalyseResult.cbCardData[2], nextAnalyseResult.cbCardData[2], firstAnalyseResult.cbCardData[2].length,
					nextAnalyseResult.cbCardData[2].length);
		}

		return false;
	}

	// 对比扑克
	public boolean CompareCardByValue(int cbFirstCard[], int cbNextCard[], int cbFirstCount, int cbNextCount) {
		// 变量定义
		boolean bHaveTwoNext = false;
		int cbConsultNext[] = new int[2];
		for (int i = 0; i < 2; i++) {
			cbConsultNext[i] = 0x00;
		}

		// 参照扑克
		for (int i = 0; i < cbNextCount; i++) {
			// 获取数值
			int cbConsultValue = GetCardValue(cbNextCard[i]);

			// 设置变量
			if ((bHaveTwoNext == false) && (cbConsultValue == 0x02))
				bHaveTwoNext = true;
			if (cbConsultValue == 0x0F) {
				cbConsultValue = 17;
			}
			if (cbConsultValue == 0x0E) {
				cbConsultValue = 16;
			}
			if (cbConsultValue == 0x02) {
				cbConsultValue = 15;
			}

			// 设置扑克
			if (cbConsultValue == 0x01) {
				if (14 > cbConsultNext[0])
					cbConsultNext[0] = 14;
				if (cbConsultValue > cbConsultNext[1])
					cbConsultNext[1] = cbConsultValue;
			} else {
				if (cbConsultValue > cbConsultNext[0])
					cbConsultNext[0] = cbConsultValue;
				if (cbConsultValue > cbConsultNext[1])
					cbConsultNext[1] = cbConsultValue;
			}
		}

		// 变量定义
		boolean bHaveTwoFirst = false;
		int cbConsultFirst[] = new int[2];
		for (int i = 0; i < 2; i++) {
			cbConsultFirst[i] = 0x00;
		}

		// 参照扑克
		for (int i = 0; i < cbFirstCount; i++) {
			// 获取数值
			int cbConsultValue = GetCardValue(cbFirstCard[i]);
			if (cbConsultValue == 0x0F) {
				cbConsultValue = 17;
			}
			if (cbConsultValue == 0x0E) {
				cbConsultValue = 16;
			}
			if (cbConsultValue == 0x02) {
				cbConsultValue = 15;
			}

			// 设置变量
			if ((bHaveTwoFirst == false) && (cbConsultValue == 0x02))
				bHaveTwoFirst = true;

			// 设置扑克
			if (cbConsultValue == 0x01) {
				if (14 > cbConsultFirst[0])
					cbConsultFirst[0] = 14;
				if (cbConsultValue > cbConsultFirst[1])
					cbConsultFirst[1] = cbConsultValue;
			} else {
				if (cbConsultValue > cbConsultFirst[0])
					cbConsultFirst[0] = cbConsultValue;
				if (cbConsultValue > cbConsultFirst[1])
					cbConsultFirst[1] = cbConsultValue;
			}
		}

		// 对比扑克
		int cbResultNext = (bHaveTwoNext == false) ? cbConsultNext[0] : cbConsultNext[1];
		int cbResultFirst = (bHaveTwoFirst == false) ? cbConsultFirst[0] : cbConsultFirst[1];

		return cbResultNext > cbResultFirst;
	}
}
